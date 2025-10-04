package com.example.prog7314_universe.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Exam(
    @DocumentId var id: String = "",
    var subject: String = "",
    var module: String = "",
    var date: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var description: String = "",
    var venue: String = "",
    var isCompleted: Boolean = false,
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now()
)
