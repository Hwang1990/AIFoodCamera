package com.hjun.aifoodcamera.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hjun.aifoodcamera.data.AppModule
import com.hjun.aifoodcamera.data.local.FoodRecordEntity
import com.hjun.aifoodcamera.data.local.UserPreferences
import com.hjun.aifoodcamera.data.repository.FoodRecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiaryUiState(
    val records: List<FoodRecordEntity> = emptyList(),
    val todayCalories: Int = 0,
    val dailyCalorieGoal: Int = UserPreferences.DEFAULT_CALORIE_GOAL
)

class DiaryViewModel(
    private val recordRepository: FoodRecordRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                recordRepository.getAllRecords(),
                recordRepository.getTodayCalories(),
                userPreferences.dailyCalorieGoal
            ) { records, todayCalories, goal ->
                DiaryUiState(
                    records = records,
                    todayCalories = todayCalories,
                    dailyCalorieGoal = goal
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            recordRepository.deleteRecord(id)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            return DiaryViewModel(
                recordRepository = AppModule.foodRecordRepository(appContext),
                userPreferences = AppModule.userPreferences(appContext)
            ) as T
        }
    }
}
