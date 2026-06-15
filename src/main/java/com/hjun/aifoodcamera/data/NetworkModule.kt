package com.hjun.aifoodcamera.data

import android.content.Context
import com.google.gson.Gson
import com.hjun.aifoodcamera.BuildConfig
import com.hjun.aifoodcamera.data.api.VisionApiService
import com.hjun.aifoodcamera.data.api.VisionContentPart
import com.hjun.aifoodcamera.data.api.VisionImageUrl
import com.hjun.aifoodcamera.data.api.VisionMessage
import com.hjun.aifoodcamera.data.api.VisionRequest
import com.hjun.aifoodcamera.data.local.AppDatabase
import com.hjun.aifoodcamera.data.local.UserPreferences
import com.hjun.aifoodcamera.data.repository.FeedbackRepository
import com.hjun.aifoodcamera.data.repository.FoodAnalysisRepository
import com.hjun.aifoodcamera.data.repository.FoodRecordRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppModule {

    private var foodRecordRepository: FoodRecordRepository? = null
    private var feedbackRepository: FeedbackRepository? = null
    private var userPreferences: UserPreferences? = null

    fun foodRecordRepository(context: Context): FoodRecordRepository {
        return foodRecordRepository ?: synchronized(this) {
            foodRecordRepository ?: FoodRecordRepository(
                AppDatabase.getInstance(context).foodRecordDao()
            ).also { foodRecordRepository = it }
        }
    }

    fun feedbackRepository(context: Context): FeedbackRepository {
        return feedbackRepository ?: synchronized(this) {
            feedbackRepository ?: FeedbackRepository(
                context.applicationContext,
                NetworkModule.gson
            ).also { feedbackRepository = it }
        }
    }

    fun userPreferences(context: Context): UserPreferences {
        return userPreferences ?: synchronized(this) {
            userPreferences ?: UserPreferences(context.applicationContext)
                .also { userPreferences = it }
        }
    }
}

object NetworkModule {

    private const val BASE_URL = "https://api.siliconflow.cn/v1/"
    private const val MODEL = "Qwen/Qwen3-VL-8B-Instruct"

    val gson = Gson()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
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
            请识别这张图片中的食物，并估算其热量和宏量营养素。
            只返回 JSON 对象，不要 Markdown 代码块，格式如下：
            {
              "foodName":"菜名",
              "calories":"估算热量数字(千卡)",
              "description":"一句话描述",
              "protein":"蛋白质克数，如 15克",
              "fat":"脂肪克数，如 8克",
              "carbs":"碳水化合物克数，如 30克"
            }
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
