package com.hjun.aifoodcamera.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodRecordDao {

    @Query("SELECT * FROM food_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<FoodRecordEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(caloriesValue), 0) FROM food_records
        WHERE createdAt >= :startOfDay AND createdAt < :endOfDay
        """
    )
    fun getTodayCalories(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Insert
    suspend fun insert(record: FoodRecordEntity): Long

    @Update
    suspend fun update(record: FoodRecordEntity)

    @Query("DELETE FROM food_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
