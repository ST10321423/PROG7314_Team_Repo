
package com.example.prog7314_universe.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Step 1: Define DataStore instance (extension property)
val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {

    // Step 2: Define keys
    companion object {
        val USERNAME_KEY = stringPreferencesKey("username")
        val EMAIL_KEY = stringPreferencesKey("email")
        val IS_LOGGED_IN_KEY = stringPreferencesKey("is_logged_in")
    }

    // Step 3: Save login data
    suspend fun saveLoginData(username: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[USERNAME_KEY] = username
            prefs[EMAIL_KEY] = email
            prefs[IS_LOGGED_IN_KEY] = "true"
        }
    }

    // Step 4: Read data
    val getUsername: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[USERNAME_KEY] }

    val getEmail: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[EMAIL_KEY] }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_LOGGED_IN_KEY] == "true" }

    // Step 5: Clear data (logout)
    suspend fun clearData() {
        context.dataStore.edit { it.clear() }
    }
}
