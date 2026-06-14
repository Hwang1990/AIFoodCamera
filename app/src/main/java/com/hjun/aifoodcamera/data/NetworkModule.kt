package com.hjun.aifoodcamera.data

import com.google.gson.Gson
import com.hjun.aifoodcamera.BuildConfig
import com.hjun.aifoodcamera.data.api.VisionApiService
import com.hjun.aifoodcamera.data.api.VisionContentPart
import com.hjun.aifoodcamera.data.api.VisionImageUrl
import com.hjun.aifoodcamera.data.api.VisionMessage
import com.hjun.aifoodcamera.data.api.VisionRequest
import com.hjun.aifoodcamera.data.repository.FoodAnalysisRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // SiliconFlow 视觉 API（deepseek-vl2 已下线，改用 Qwen3-VL-8B）
    private const val BASE_URL = "https://api.siliconflow.cn/v1/"
    private const val MODEL = "Qwen/Qwen3-VL-8B-Instruct"

    private val gson = Gson()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val visionApiService: VisionApiService by lazy {
        retrofit.create(VisionApiService::class.java)
    }

    val foodAnalysisRepository: FoodAnalysisRepository by lazy {
        FoodAnalysisRepository(
            apiService = visionApiService,
            apiKey = BuildConfig.SILICONFLOW_API_KEY,
            gson = gson
        )
    }

    fun buildVisionRequest(base64Image: String): VisionRequest {
        val prompt = """
            请识别这张图片中的食物，并估算其热量。
            只返回 JSON 对象，不要 Markdown 代码块，格式如下：
            {"foodName":"菜名","calories":"估算热量(千卡)","description":"一句话描述"}
        """.trimIndent()

        return VisionRequest(
            model = MODEL,
            messages = listOf(
                VisionMessage(
                    role = "user",
                    content = listOf(
                        VisionContentPart(
                            type = "image_url",
                            imageUrl = VisionImageUrl(
                                url = "data:image/jpeg;base64,$base64Image"
                            )
                        ),
                        VisionContentPart(
                            type = "text",
                            text = prompt
                        )
                    )
                )
            )
        )
    }
}
