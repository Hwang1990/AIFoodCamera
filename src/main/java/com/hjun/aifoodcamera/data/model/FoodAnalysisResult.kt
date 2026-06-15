package com.hjun.aifoodcamera.data.model

import com.google.gson.annotations.SerializedName

data class FoodAnalysisResult(
    @SerializedName("foodName")
    val foodName: String,
    @SerializedName("calories")
    val calories: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("protein")
    val protein: String = "",
    @SerializedName("fat")
    val fat: String = "",
    @SerializedName("carbs")
    val carbs: String = ""
) {
    fun caloriesValue(): Int {
        val digits = calories.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }
}
