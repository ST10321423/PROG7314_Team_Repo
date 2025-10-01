package com.example.prog7314_universe.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class SavingsGoal(
    @DocumentId var id: String = "",
    var userOwnerId: String = "",
    var goalName: String = "",
    var targetAmount: Double = 0.0,
    var deadline: Timestamp = Timestamp.now(),
    var createdAt: Timestamp = Timestamp.now()
)
