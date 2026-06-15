package com.hjun.aifoodcamera.util

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await

class ObjectDetectionHelper {

    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .build()
    )

    suspend fun hasObjectInFrame(imageProxy: ImageProxy): Boolean {
        val mediaImage = imageProxy.image ?: return false
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        return try {
            val objects = detector.process(inputImage).await()
            objects.isNotEmpty()
        } catch (_: Exception) {
            false
        } finally {
            imageProxy.close()
        }
    }

    fun close() {
        detector.close()
    }
}
