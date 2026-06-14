package com.hjun.aifoodcamera.data.api

import com.google.gson.annotations.SerializedName

data class VisionRequest(
    val model: String,
    val messages: List<VisionMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 512
)

data class VisionMessage(
    val role: String,
    val content: List<VisionContentPart>
)

data class VisionContentPart(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: VisionImageUrl? = null
)

data class VisionImageUrl(
    val url: String,
    val detail: String = "low"
)

data class VisionResponse(
    val choices: List<VisionChoice>?
)

data class VisionChoice(
    val message: VisionResponseMessage?
)

data class VisionResponseMessage(
    val content: String?
)
