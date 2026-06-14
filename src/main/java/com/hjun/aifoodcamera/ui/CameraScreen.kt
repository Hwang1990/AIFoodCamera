package com.hjun.aifoodcamera.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hjun.aifoodcamera.R
import com.hjun.aifoodcamera.viewmodel.CameraViewModel
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
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

            else -> {
                CameraPreview(
                    onImageCaptureReady = { imageCapture = it }
                )

                CaptureButton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    enabled = !uiState.isCapturing && !uiState.isAnalyzing,
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

                if (uiState.isAnalyzing || uiState.isCapturing) {
                    LoadingOverlay(
                        message = if (uiState.isCapturing) {
                            stringResource(R.string.analyzing)
                        } else {
                            stringResource(R.string.analyzing)
                        }
                    )
                }

                uiState.foodResult?.let { result ->
                    FoodResultCard(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        foodName = result.foodName,
                        calories = result.calories,
                        description = result.description,
                        onDismiss = { viewModel.dismissResult() }
                    )
                }

                uiState.errorMessage?.let { error ->
                    ErrorBanner(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        message = error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onImageCaptureReady: (ImageCapture) -> Unit
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

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
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
            .size(80.dp)
            .clip(CircleShape)
            .border(4.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
            .padding(6.dp)
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
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun FoodResultCard(
    modifier: Modifier = Modifier,
    foodName: String,
    calories: String,
    description: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = foodName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.calories_format, calories),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "点击关闭",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.9f))
            .clickable(onClick = onDismiss)
            .padding(16.dp)
    ) {
        Text(
            text = message,
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.camera_permission_denied),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.grant_permission))
        }
    }
}
