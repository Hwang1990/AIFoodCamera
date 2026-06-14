package com.hjun.aifoodcamera.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface VisionApiService {

    @POST("chat/completions")
    suspend fun analyzeFood(
        @Header("Authorization") authorization: String,
        @Body request: VisionRequest
    ): VisionResponse
}
