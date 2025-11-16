package com.example.prog7314_universe

import android.app.Application
import com.example.prog7314_universe.utils.OfflineSupportManager

class UniVerseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OfflineSupportManager.init(this)
    }
}