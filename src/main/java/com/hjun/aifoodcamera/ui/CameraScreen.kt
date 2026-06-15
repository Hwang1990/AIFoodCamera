package com.hjun.aifoodcamera.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hjun.aifoodcamera.R
import com.hjun.aifoodcamera.data.model.FoodAnalysisResult
import com.hjun.aifoodcamera.ui.components.AnalyzingOverlay
import com.hjun.aifoodcamera.ui.components.FoodResultCard
import com.hjun.aifoodcamera.ui.components.ViewfinderOverlay
import com.hjun.aifoodcamera.ui.theme.AppDimens
import com.hjun.aifoodcamera.util.ObjectDetectionHelper
import com.hjun.aifoodcamera.viewmodel.CameraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val objectDetectionHelper = remember { ObjectDetectionHelper() }
    val isProcessingFrame = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            analysisExecutor.shutdown()
            objectDetectionHelper.close()
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var editNameText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.analyzeFromGallery(it) }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onCameraPermissionResult(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.showEditDialog) {
        if (uiState.showEditDialog) {
            editNameText = uiState.foodResult?.foodName ?: ""
        }
    }

    val showCamera = !uiState.isAnalyzing &&
        uiState.capturedImagePath == null &&
        uiState.foodResult == null &&
        !uiState.analysisFailed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            !uiState.hasCameraPermission -> {
                PermissionDeniedContent(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }

            uiState.isAnalyzing -> {
                AnalyzingOverlay(
                    imagePath = uiState.capturedImagePath,
                    loadingMessage = uiState.loadingMessage
                )
            }

            uiState.capturedImagePath != null && (uiState.foodResult != null || uiState.analysisFailed) -> {
                AsyncImage(
                    model = File(uiState.capturedImagePath!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                CameraPreview(
                    onImageCaptureReady = { imageCapture = it },
                    onObjectDetection = { imageProxy ->
                        if (!isProcessingFrame.compareAndSet(false, true)) {
                            imageProxy.close()
                            return@CameraPreview
                        }
                        scope.launch {
                            val detected = objectDetectionHelper.hasObjectInFrame(imageProxy)
                            viewModel.onObjectDetected(detected)
                            isProcessingFrame.set(false)
                        }
                    },
                    analysisExecutor = analysisExecutor,
                    enableAnalysis = showCamera
                )
            }
        }

        if (showCamera) {
            ViewfinderOverlay(
                objectDetected = uiState.objectDetected,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showCamera) {
            CaptureButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppDimens.captureButtonBottom),
                enabled = !uiState.isCapturing,
                onClick = {
                    imageCapture?.let { capture ->
                        viewModel.captureAndAnalyze(
                            imageCapture = capture,
                            outputFile = File(
                                context.cacheDir,
                                "capture_${System.currentTimeMillis()}.jpg"
                            ),
                            executor = cameraExecutor
                        )
                    }
                }
            )
        }

        if (uiState.foodResult != null || uiState.analysisFailed) {
            FoodResultCard(
                result = uiState.foodResult ?: FoodAnalysisResult(
                    foodName = "",
                    calories = "",
                    description = ""
                ),
                imagePath = uiState.capturedImagePath,
                dailyCalorieGoal = uiState.dailyCalorieGoal,
                analysisFailed = uiState.analysisFailed,
                isTtsMuted = uiState.isTtsMuted,
                isSpeaking = uiState.isSpeaking,
                onEditName = { viewModel.showEditDialog() },
                onToggleTts = { viewModel.toggleTts() },
                onRetake = { viewModel.retakePhoto() },
                onPickGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRetry = { viewModel.retryAnalysis() },
                onFeedback = { viewModel.submitFeedback() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = AppDimens.screenPaddingCompact, vertical = 12.dp)
                    .fillMaxWidth()
            )
        }

        uiState.errorMessage?.let { error ->
            if (!uiState.analysisFailed) {
                ErrorBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = AppDimens.screenPadding, vertical = 12.dp),
                    message = error,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }

        if (uiState.feedbackSubmitted) {
            LaunchedEffect(Unit) {
                delay(2000)
                viewModel.clearFeedbackSubmitted()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = AppDimens.screenPadding, vertical = 12.dp)
                    .clip(RoundedCornerShape(AppDimens.cardRadiusSmall))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.feedback_thanks),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    if (uiState.showEditDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideEditDialog() },
            title = { Text(stringResource(R.string.edit_name)) },
            text = {
                OutlinedTextField(
                    value = editNameText,
                    onValueChange = { editNameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.food_name_hint)) }
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.updateFoodName(editNameText) },
                    enabled = editNameText.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideEditDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CameraPreview(
    onImageCaptureReady: (ImageCapture) -> Unit,
    onObjectDetection: (ImageProxy) -> Unit,
    analysisExecutor: Executor,
    enableAnalysis: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                onImageCaptureReady(imageCapture)

                val useCases = mutableListOf(preview, imageCapture)

                if (enableAnalysis) {
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                onObjectDetection(imageProxy)
                            }
                        }
                    useCases.add(imageAnalysis)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        *useCases.toTypedArray()
                    )
                } catch (_: Exception) {
                    // Camera binding may fail if permission is revoked at runtime.
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun CaptureButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(AppDimens.captureButtonOuter)
            .clip(CircleShape)
            .border(AppDimens.captureButtonBorder, MaterialTheme.colorScheme.onBackground, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.captureButtonInner)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun ErrorBanner(
    modifier: Modifier = Modifier,
    message: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.cardRadiusSmall))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onError,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.camera_permission_denied),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.grant_permission))
        }
    }
}
