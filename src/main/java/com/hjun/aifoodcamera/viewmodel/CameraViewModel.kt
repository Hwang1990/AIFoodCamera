package com.hjun.aifoodcamera.viewmodel

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hjun.aifoodcamera.data.AppModule
import com.hjun.aifoodcamera.data.NetworkModule
import com.hjun.aifoodcamera.data.local.UserPreferences
import com.hjun.aifoodcamera.data.model.FoodAnalysisResult
import com.hjun.aifoodcamera.data.repository.FeedbackRepository
import com.hjun.aifoodcamera.data.repository.FoodAnalysisRepository
import com.hjun.aifoodcamera.data.repository.FoodRecordRepository
import com.hjun.aifoodcamera.util.ImageUtils
import com.hjun.aifoodcamera.util.LoadingMessages
import com.hjun.aifoodcamera.util.TextToSpeechHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor

data class CameraUiState(
    val hasCameraPermission: Boolean = false,
    val isCapturing: Boolean = false,
    val isAnalyzing: Boolean = false,
    val objectDetected: Boolean = false,
    val capturedImagePath: String? = null,
    val foodResult: FoodAnalysisResult? = null,
    val analysisFailed: Boolean = false,
    val errorMessage: String? = null,
    val loadingMessage: String = LoadingMessages.messages.first(),
    val isTtsMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val showEditDialog: Boolean = false,
    val dailyCalorieGoal: Int = UserPreferences.DEFAULT_CALORIE_GOAL,
    val feedbackSubmitted: Boolean = false
)

class CameraViewModel(
    private val context: Context,
    private val repository: FoodAnalysisRepository,
    private val recordRepository: FoodRecordRepository,
    private val feedbackRepository: FeedbackRepository,
    private val userPreferences: UserPreferences,
    private val ttsHelper: TextToSpeechHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var pendingImageFile: File? = null
    private var savedImagePath: String? = null
    private var lastSavedRecordId: Long? = null
    private var loadingMessageJob: Job? = null

    init {
        viewModelScope.launch {
            userPreferences.isTtsMuted.collect { muted ->
                _uiState.update { it.copy(isTtsMuted = muted) }
            }
        }
        viewModelScope.launch {
            userPreferences.dailyCalorieGoal.collect { goal ->
                _uiState.update { it.copy(dailyCalorieGoal = goal) }
            }
        }
        viewModelScope.launch {
            ttsHelper.isSpeaking.collect { speaking ->
                _uiState.update { it.copy(isSpeaking = speaking) }
            }
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun onObjectDetected(detected: Boolean) {
        _uiState.update { it.copy(objectDetected = detected) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissResult() {
        ttsHelper.stop()
        _uiState.update {
            it.copy(
                foodResult = null,
                capturedImagePath = null,
                analysisFailed = false,
                showEditDialog = false
            )
        }
        savedImagePath = null
        pendingImageFile = null
        lastSavedRecordId = null
    }

    fun retakePhoto() = dismissResult()

    fun captureAndAnalyze(
        imageCapture: ImageCapture,
        outputFile: File,
        executor: Executor
    ) {
        if (_uiState.value.isCapturing || _uiState.value.isAnalyzing) return

        _uiState.update {
            it.copy(
                isCapturing = true,
                errorMessage = null,
                foodResult = null,
                analysisFailed = false,
                capturedImagePath = null
            )
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    pendingImageFile = outputFile
                    val persisted = ImageUtils.persistImage(
                        outputFile,
                        File(context.filesDir, "captures")
                    )
                    savedImagePath = persisted
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            isAnalyzing = true,
                            capturedImagePath = persisted
                        )
                    }
                    startLoadingMessages()
                    analyzeImage(outputFile, persisted)
                }

                override fun onError(exception: ImageCaptureException) {
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            isAnalyzing = false,
                            errorMessage = exception.message ?: "拍照失败"
                        )
                    }
                }
            }
        )
    }

    fun analyzeFromGallery(uri: Uri) {
        if (_uiState.value.isAnalyzing) return

        viewModelScope.launch {
            try {
                val cacheFile = ImageUtils.copyUriToCache(context, uri)
                val persisted = ImageUtils.persistImage(
                    cacheFile,
                    File(context.filesDir, "captures"),
                    prefix = "gallery"
                )
                pendingImageFile = cacheFile
                savedImagePath = persisted
                _uiState.update {
                    it.copy(
                        isAnalyzing = true,
                        capturedImagePath = persisted,
                        foodResult = null,
                        analysisFailed = false,
                        errorMessage = null
                    )
                }
                startLoadingMessages()
                analyzeImage(cacheFile, persisted)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = e.message ?: "图片读取失败"
                    )
                }
            }
        }
    }

    fun retryAnalysis() {
        val imagePath = savedImagePath ?: pendingImageFile?.absolutePath ?: return
        val file = File(imagePath)
        if (!file.exists()) return

        _uiState.update {
            it.copy(
                isAnalyzing = true,
                analysisFailed = false,
                errorMessage = null,
                foodResult = null
            )
        }
        startLoadingMessages()
        analyzeImage(file, imagePath)
    }

    fun toggleTts() {
        viewModelScope.launch {
            val newMuted = !_uiState.value.isTtsMuted
            userPreferences.setTtsMuted(newMuted)
            if (newMuted) {
                ttsHelper.stop()
            } else {
                _uiState.value.foodResult?.let { result ->
                    ttsHelper.speakFoodResult(
                        foodName = result.foodName,
                        calories = result.calories,
                        description = result.description
                    )
                }
            }
        }
    }

    fun showEditDialog() {
        _uiState.update { it.copy(showEditDialog = true) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false) }
    }

    fun updateFoodName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        val current = _uiState.value.foodResult
        val updated = current?.copy(foodName = trimmed)
            ?: FoodAnalysisResult(
                foodName = trimmed,
                calories = "未知",
                description = "用户手动输入"
            )
        _uiState.update {
            it.copy(
                foodResult = updated,
                showEditDialog = false,
                analysisFailed = false
            )
        }
        viewModelScope.launch {
            savedImagePath?.let { path ->
                val recordId = lastSavedRecordId
                if (recordId != null) {
                    recordRepository.updateRecord(recordId, updated, path)
                } else {
                    lastSavedRecordId = recordRepository.saveRecord(updated, path)
                }
            }
        }
    }

    fun submitFeedback() {
        viewModelScope.launch {
            val result = feedbackRepository.submitFeedback(
                imagePath = savedImagePath,
                result = _uiState.value.foodResult
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackSubmitted = true) }
            }
        }
    }

    fun clearFeedbackSubmitted() {
        _uiState.update { it.copy(feedbackSubmitted = false) }
    }

    private fun startLoadingMessages() {
        loadingMessageJob?.cancel()
        loadingMessageJob = viewModelScope.launch {
            var index = 0
            while (true) {
                _uiState.update {
                    it.copy(loadingMessage = LoadingMessages.messages[index % LoadingMessages.messages.size])
                }
                index++
                delay(2500)
            }
        }
    }

    private fun stopLoadingMessages() {
        loadingMessageJob?.cancel()
        loadingMessageJob = null
    }

    private fun analyzeImage(imageFile: File, persistedPath: String) {
        viewModelScope.launch {
            try {
                val base64 = ImageUtils.fileToBase64(imageFile)
                repository.analyzeFood(base64)
                    .onSuccess { result ->
                        stopLoadingMessages()
                        savedImagePath = persistedPath
                        lastSavedRecordId = recordRepository.saveRecord(result, persistedPath)
                        _uiState.update {
                            it.copy(
                                isAnalyzing = false,
                                foodResult = result,
                                analysisFailed = false,
                                errorMessage = null
                            )
                        }
                        if (!_uiState.value.isTtsMuted) {
                            ttsHelper.speakFoodResult(
                                foodName = result.foodName,
                                calories = result.calories,
                                description = result.description
                            )
                        }
                    }
                    .onFailure { error ->
                        stopLoadingMessages()
                        _uiState.update {
                            it.copy(
                                isAnalyzing = false,
                                analysisFailed = true,
                                errorMessage = error.message ?: "识别失败"
                            )
                        }
                    }
            } catch (e: Exception) {
                stopLoadingMessages()
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        analysisFailed = true,
                        errorMessage = e.message ?: "图片处理失败"
                    )
                }
            } finally {
                if (imageFile.absolutePath != persistedPath) {
                    imageFile.delete()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLoadingMessages()
        ttsHelper.shutdown()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            val ttsHelper = TextToSpeechHelper(appContext)
            return CameraViewModel(
                context = appContext,
                repository = NetworkModule.foodAnalysisRepository,
                recordRepository = AppModule.foodRecordRepository(appContext),
                feedbackRepository = AppModule.feedbackRepository(appContext),
                userPreferences = AppModule.userPreferences(appContext),
                ttsHelper = ttsHelper
            ) as T
        }
    }
}
