package com.hjun.aifoodcamera.data.repository

import android.content.Context
import com.google.gson.Gson
import com.hjun.aifoodcamera.data.model.FoodAnalysisResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedbackRepository(
    private val context: Context,
    private val gson: Gson
) {

    suspend fun submitFeedback(
        imagePath: String?,
        result: FoodAnalysisResult?,
        userComment: String = ""
    ): Result<Unit> {
        return try {
            val feedbackDir = File(context.filesDir, "feedback").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val feedbackFile = File(feedbackDir, "feedback_$timestamp.json")

            val payload = mapOf(
                "timestamp" to timestamp,
                "userComment" to userComment,
                "result" to result,
                "imagePath" to imagePath
            )
            feedbackFile.writeText(gson.toJson(payload))

            imagePath?.let { path ->
                val source = File(path)
                if (source.exists()) {
                    source.copyTo(File(feedbackDir, "image_$timestamp.jpg"), overwrite = true)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
