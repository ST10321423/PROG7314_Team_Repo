package com.example.prog7314_universe

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.prog7314_universe.utils.OfflineSupportManager
import com.example.prog7314_universe.utils.PrefManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class UniVerseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialise offline support as before
        OfflineSupportManager.init(this)

        // Apply saved app language on startup
        applyPersistedLanguage()
    }

    private fun applyPersistedLanguage() {
        val prefManager = PrefManager(applicationContext)

        // Block briefly at startup to read the saved language from DataStore / Prefs
        val savedLanguageCode = runBlocking {
            prefManager.language.first()
        }

        if (savedLanguageCode.isNotBlank()) {
            val locales = LocaleListCompat.forLanguageTags(savedLanguageCode)
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
