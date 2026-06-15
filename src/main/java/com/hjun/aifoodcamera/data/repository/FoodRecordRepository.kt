package com.hjun.aifoodcamera.data.repository

import com.hjun.aifoodcamera.data.local.FoodRecordDao
import com.hjun.aifoodcamera.data.local.FoodRecordEntity
import com.hjun.aifoodcamera.data.local.toEntity
import com.hjun.aifoodcamera.data.model.FoodAnalysisResult
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FoodRecordRepository(
    private val dao: FoodRecordDao
) {

    fun getAllRecords(): Flow<List<FoodRecordEntity>> = dao.getAllRecords()

    fun getTodayCalories(): Flow<Int> {
        val (start, end) = todayRange()
        return dao.getTodayCalories(start, end)
    }

    suspend fun saveRecord(result: FoodAnalysisResult, imagePath: String): Long {
        return dao.insert(result.toEntity(imagePath))
    }

    suspend fun updateRecord(id: Long, result: FoodAnalysisResult, imagePath: String) {
        val entity = result.toEntity(imagePath).copy(id = id)
        dao.update(entity)
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteById(id)
    }

    private fun todayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return start to calendar.timeInMillis
    }
}
