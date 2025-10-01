package com.example.prog7314_universe.Models

import com.google.firebase.Timestamp

data class HabitLog(
    var completed: Boolean = false,
    var completedAt: Timestamp? = null
)