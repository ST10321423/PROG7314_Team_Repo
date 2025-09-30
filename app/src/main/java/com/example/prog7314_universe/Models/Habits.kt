package com.example.prog7314_universe.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Habit(
    @DocumentId var habitId: String = "",
    var name: String = "",
    var iconName: String? = null,
    var colorHex: String? = "#4CAF50",
    var cadence: String = "daily",            // or "weekly"
    var daysMask: Int = 0,                    // bitmask Sun=1 … Sat=64
    var timeOfDay: String? = null,            // "morning" or "afternoon" or "evening"
    var difficulty: String = "easy",
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
) {
    companion object {
        fun dayToBit(dayIndexMon1: Int): Int {
            // Kotlin DayOfWeek.MONDAY.value == 1 … SUNDAY == 7  → map to Sun=1 … Sat=64
            val sunFirst = intArrayOf(2,4,8,16,32,64,1) // Mon..Sun -> bit
            return sunFirst[dayIndexMon1 - 1]
        }
        fun maskFor(daysMon1: Set<Int>): Int = daysMon1.fold(0) { acc, d -> acc or dayToBit(d) }
        fun isDueToday(daysMask: Int, mondayIs1: Int): Boolean {
            val bit = dayToBit(mondayIs1)
            return (daysMask and bit) != 0
        }
    }
}
