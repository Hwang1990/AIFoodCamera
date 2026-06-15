package com.hjun.aifoodcamera.data.repository

import com.google.gson.Gson
import com.hjun.aifoodcamera.data.NetworkModule
import com.hjun.aifoodcamera.data.api.VisionApiService
import com.hjun.aifoodcamera.data.model.FoodAnalysisResult
import kotlinx.coroutines.delay
import retrofit2.HttpException

class FoodAnalysisRepository(
    private val apiService: VisionApiService,
    private val apiKey: String,
    private val gson: Gson
) {

    suspend fun analyzeFood(base64Image: String): Result<FoodAnalysisResult> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("SILICONFLOW_API_KEY is not configured"))
        }

        var lastError: Exception? = null
        var backoffMs = 1_000L

        repeat(MAX_RETRIES + 1) { attempt ->
            val result = analyzeFoodOnce(base64Image)
            if (result.isSuccess) return result

            lastError = result.exceptionOrNull() as? Exception
                ?: Exception(result.exceptionOrNull()?.message)

            if (attempt < MAX_RETRIES) {
                delay(backoffMs)
                backoffMs *= 2
            }
        }

        return Result.failure(lastError ?: Exception("识别失败"))
    }

    private suspend fun analyzeFoodOnce(base64Image: String): Result<FoodAnalysisResult> {
        return try {
            val request = NetworkModule.buildVisionRequest(base64Image)
            val response = apiService.analyzeFood(
                authorization = "Bearer $apiKey",
                request = request
            )

            val rawText = response.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?: return Result.failure(IllegalStateException("API 返回空内容"))

            val jsonText = extractJson(rawText)
            val result = gson.fromJson(jsonText, FoodAnalysisResult::class.java)
            Result.success(result)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Result.failure(Exception("HTTP ${e.code()}: ${errorBody ?: e.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJson(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```")) return trimmed

        return trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    companion object {
        private const val MAX_RETRIES = 1
    }
}
