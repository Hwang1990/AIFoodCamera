package com.hjun.aifoodcamera.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hjun.aifoodcamera.data.AppModule
import com.hjun.aifoodcamera.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val dailyCalorieGoal: Int = UserPreferences.DEFAULT_CALORIE_GOAL,
    val isTtsMuted: Boolean = false,
    val goalSaved: Boolean = false
)

class SettingsViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.dailyCalorieGoal.collect { goal ->
                _uiState.update { it.copy(dailyCalorieGoal = goal) }
            }
        }
        viewModelScope.launch {
            userPreferences.isTtsMuted.collect { muted ->
                _uiState.update { it.copy(isTtsMuted = muted) }
            }
        }
    }

    fun updateGoalInput(goal: Int) {
        _uiState.update { it.copy(dailyCalorieGoal = goal.coerceIn(500, 10000)) }
    }

    fun saveGoal() {
        viewModelScope.launch {
            userPreferences.setDailyCalorieGoal(_uiState.value.dailyCalorieGoal)
            _uiState.update { it.copy(goalSaved = true) }
        }
    }

    fun clearGoalSaved() {
        _uiState.update { it.copy(goalSaved = false) }
    }

    fun setTtsMuted(muted: Boolean) {
        viewModelScope.launch {
            userPreferences.setTtsMuted(muted)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                userPreferences = AppModule.userPreferences(context.applicationContext)
            ) as T
        }
    }
}
