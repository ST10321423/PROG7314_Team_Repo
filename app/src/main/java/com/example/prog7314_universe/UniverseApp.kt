package com.example.prog7314_universe

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.prog7314_universe.utils.PrefManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class UniverseApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val prefManager = PrefManager(applicationContext)
        val savedLanguage = runBlocking { prefManager.language.first() }
        val languageTag = savedLanguage.ifBlank { Locale.getDefault().language }
        val locales = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}