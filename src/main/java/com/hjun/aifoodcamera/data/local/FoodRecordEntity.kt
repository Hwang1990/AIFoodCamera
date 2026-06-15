package com.hjun.aifoodcamera.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hjun.aifoodcamera.data.model.FoodAnalysisResult

@Entity(tableName = "food_records")
data class FoodRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val foodName: String,
    val calories: String,
    val caloriesValue: Int = 0,
    val description: String,
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toAnalysisResult(): FoodAnalysisResult = FoodAnalysisResult(
        foodName = foodName,
        calories = calories,
        description = description,
        protein = protein,
        fat = fat,
        carbs = carbs
    )
}

fun FoodAnalysisResult.toEntity(imagePath: String): FoodRecordEntity = FoodRecordEntity(
    imagePath = imagePath,
    foodName = foodName,
    calories = calories,
    caloriesValue = caloriesValue(),
    description = description,
    protein = protein,
    fat = fat,
    carbs = carbs
)
