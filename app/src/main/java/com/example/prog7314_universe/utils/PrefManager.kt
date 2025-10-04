package com.example.prog7314_universe.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "app_prefs"


private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class PrefManager(private val context: Context) {

    // Keys
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val TEXT_SCALE = floatPreferencesKey("text_scale") // 0.8f..1.2f
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val TASK_REMINDERS = booleanPreferencesKey("task_reminders_enabled")
        val EXAM_ALERTS = booleanPreferencesKey("exam_alerts_enabled")
        val HABIT_REMINDERS = booleanPreferencesKey("habit_reminders_enabled")
    }

    // Reads (Flows)
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: false }
    val textScale: Flow<Float> = context.dataStore.data.map { it[Keys.TEXT_SCALE] ?: 1.0f }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS] ?: true }
    val taskRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.TASK_REMINDERS] ?: true }
    val examAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EXAM_ALERTS] ?: true }
    val habitRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.HABIT_REMINDERS] ?: true }

    // Writes (suspend)
    suspend fun setDarkMode(value: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = value }
    }

    suspend fun setTextScale(value: Float) {
        context.dataStore.edit { it[Keys.TEXT_SCALE] = value.coerceIn(0.8f, 1.2f) }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = value }
    }

    suspend fun setTaskRemindersEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.TASK_REMINDERS] = value }
    }

    suspend fun setExamAlertsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.EXAM_ALERTS] = value }
    }

    suspend fun setHabitRemindersEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.HABIT_REMINDERS] = value }
    }
}
