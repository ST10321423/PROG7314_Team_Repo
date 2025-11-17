package com.example.prog7314_universe.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

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
    var updatedAt: Timestamp? = null,
    var lastCompleted: Timestamp? = null,
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    var streak: Int = 0
) {
    companion object {
        //Convert day index (Monday = 1 ... Sunday = 7) to bit position
        fun dayToBit(dayIndexMon1: Int): Int {
            // Kotlin DayOfWeek.MONDAY.value == 1 … SUNDAY == 7
            // Map to: Sun=1, Mon=2, Tue=4, Wed=8, Thu=16, Fri=32, Sat=64
            val sunFirst = intArrayOf(2, 4, 8, 16, 32, 64, 1) // Mon..Sun -> bit
            return sunFirst[dayIndexMon1 - 1]
        }

        //Creates a mask from a set of days (Monday-1 based)
        fun maskFor(daysMon1: Set<Int>): Int =
            daysMon1.fold(0) { acc, d -> acc or dayToBit(d) }

        //Checks if habit is due today based on mask and current day

        fun isDueToday(daysMask: Int, mondayIs1: Int): Boolean {
            if (daysMask == 0) return true // Daily habit (no specific days)
            val bit = dayToBit(mondayIs1)
            return (daysMask and bit) != 0
        }
    }
}