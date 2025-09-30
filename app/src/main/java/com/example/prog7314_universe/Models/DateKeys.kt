package com.example.prog7314_universe.Models

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateKeys {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    fun todayKey(): String = LocalDate.now().format(fmt)
    fun keyOf(date: LocalDate): String = date.format(fmt)
}