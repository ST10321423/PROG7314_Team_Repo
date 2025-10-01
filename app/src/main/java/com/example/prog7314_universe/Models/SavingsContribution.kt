package com.example.prog7314_universe.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class SavingsContribution(
    @DocumentId var id: String = "",
    var goalId: String = "",
    var amount: Double = 0.0,
    var contributionDate: Timestamp = Timestamp.now()
)
