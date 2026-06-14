package com.hjun.aifoodcamera.viewmodel

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hjun.aifoodcamera.data.NetworkModule
import com.hjun.aifoodcamera.data.model.FoodAnalysisResult
import com.hjun.aifoodcamera.data.repository.FoodAnalysisRepository
import com.hjun.aifoodcamera.util.ImageUtils
import com.hjun.aifoodcamera.util.TextToSpeechHelper
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
    val foodResult: FoodAnalysisResult? = null,
    val errorMessage: String? = null
)

class CameraViewModel(
    private val repository: FoodAnalysisRepository,
    private val ttsHelper: TextToSpeechHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissResult() {
        _uiState.update { it.copy(foodResult = null) }
    }

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
                foodResult = null
            )
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    _uiState.update { it.copy(isCapturing = false, isAnalyzing = true) }
                    analyzeImage(outputFile)
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

    private fun analyzeImage(imageFile: File) {
        viewModelScope.launch {
            try {
                val base64 = ImageUtils.fileToBase64(imageFile)
                repository.analyzeFood(base64)
                    .onSuccess { result ->
                        _uiState.update {
                            it.copy(
                                isAnalyzing = false,
                                foodResult = result,
                                errorMessage = null
                            )
                        }
                        ttsHelper.speakFoodResult(
                            foodName = result.foodName,
                            calories = result.calories,
                            description = result.description
                        )
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isAnalyzing = false,
                                errorMessage = error.message ?: "识别失败"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = e.message ?: "图片处理失败"
                    )
                }
            } finally {
                imageFile.delete()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val ttsHelper = TextToSpeechHelper(context)
            return CameraViewModel(
                repository = NetworkModule.foodAnalysisRepository,
                ttsHelper = ttsHelper
            ) as T
        }
    }
}
