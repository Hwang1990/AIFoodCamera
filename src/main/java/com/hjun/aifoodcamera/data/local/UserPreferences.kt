package com.hjun.aifoodcamera.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    private val muteKey = booleanPreferencesKey("tts_muted")
    private val calorieGoalKey = intPreferencesKey("daily_calorie_goal")

    val isTtsMuted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[muteKey] ?: false
    }

    val dailyCalorieGoal: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[calorieGoalKey] ?: DEFAULT_CALORIE_GOAL
    }

    suspend fun setTtsMuted(muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[muteKey] = muted
        }
    }

    suspend fun setDailyCalorieGoal(goal: Int) {
        context.dataStore.edit { prefs ->
            prefs[calorieGoalKey] = goal.coerceIn(500, 10000)
        }
    }

    companion object {
        const val DEFAULT_CALORIE_GOAL = 2000
    }
}
