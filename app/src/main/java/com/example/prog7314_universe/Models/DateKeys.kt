package com.example.prog7314_universe.Models

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

object DateKeys {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    fun todayKey(): String = LocalDate.now().format(fmt)
    fun keyOf(date: LocalDate): String = date.format(fmt)
    fun dayOfWeekToMask(day: DayOfWeek): Int {
        return when (day) {
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 4
            DayOfWeek.WEDNESDAY -> 8
            DayOfWeek.THURSDAY -> 16
            DayOfWeek.FRIDAY -> 32
            DayOfWeek.SATURDAY -> 64
            DayOfWeek.SUNDAY -> 1
        }
    }

    fun todayMask(): Int = dayOfWeekToMask(LocalDate.now().dayOfWeek)
}