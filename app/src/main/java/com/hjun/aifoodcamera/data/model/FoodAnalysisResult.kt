package com.hjun.aifoodcamera.data.model

import com.google.gson.annotations.SerializedName

data class FoodAnalysisResult(
    @SerializedName("foodName")
    val foodName: String,
    @SerializedName("calories")
    val calories: String,
    @SerializedName("description")
    val description: String
)
