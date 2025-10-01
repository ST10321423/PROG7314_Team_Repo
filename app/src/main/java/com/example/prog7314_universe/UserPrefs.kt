package com.example.prog7314_universe

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

val Context.dataStore by preferencesDataStore("user_prefs")

object UserPrefsKeys {
    val THEME = stringPreferencesKey("theme")             // "system" | "light" | "dark"
    val LANG  = stringPreferencesKey("lang")              // "en", "af", "zu"
    val TEXT_SCALE = floatPreferencesKey("text_scale")    // 0.85f .. 1.25f
    val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")

    val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
    val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    val REMINDER_MIN  = intPreferencesKey("reminder_min")

    val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
}

class UserPrefs(private val context: Context) {
    val flow = context.dataStore.data.map { it }

    suspend fun setTheme(value: String) =
        context.dataStore.edit { it[UserPrefsKeys.THEME] = value }

    suspend fun setLang(value: String) =
        context.dataStore.edit { it[UserPrefsKeys.LANG] = value }

    suspend fun setTextScale(scale: Float) =
        context.dataStore.edit { it[UserPrefsKeys.TEXT_SCALE] = scale }

    suspend fun setReduceMotion(enabled: Boolean) =
        context.dataStore.edit { it[UserPrefsKeys.REDUCE_MOTION] = enabled }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[UserPrefsKeys.NOTIF_ENABLED] = enabled }

    suspend fun setReminder(hour: Int, min: Int) =
        context.dataStore.edit { p -> p[UserPrefsKeys.REMINDER_HOUR] = hour; p[UserPrefsKeys.REMINDER_MIN] = min }

    suspend fun setBiometric(enabled: Boolean) =
        context.dataStore.edit { it[UserPrefsKeys.BIOMETRIC] = enabled }
}
