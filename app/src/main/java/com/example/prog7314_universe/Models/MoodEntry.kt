package com.example.prog7314_universe.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class MoodEntry(
    @DocumentId
    val moodId: String = "",

    val userId: String = "",

    val date: Timestamp = Timestamp.now(),

    val scale: String = "", // Stored as string for Firestore compatibility

    val note: String? = null,

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    @ServerTimestamp
    val updatedAt: Timestamp? = null
){
    // No-argument constructor required by Firestore
    constructor() : this("", "", Timestamp.now(), "", null, null, null)

    fun getMoodScale(): MoodScale {
        return try {
            MoodScale.valueOf(scale)
        } catch (e: IllegalArgumentException) {
            MoodScale.HAPPY // Default fallback
        }
    }

    fun getFormattedDate(): String {
        val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        return dateFormat.format(date.toDate())
    }

    companion object {
        fun create(userId: String, date: Date, moodScale: MoodScale, note: String? = null): MoodEntry {
            return MoodEntry(
                userId = userId,
                date = Timestamp(date),
                scale = moodScale.name,
                note = note
            )
        }
    }
}

enum class MoodScale(val displayName: String, val emoji: String, val colorHex: String) {
    HAPPY("Happy", "😊", "#FFD93D"),
    SAD("Sad", "😢", "#6BCB77"),
    ANGRY("Angry", "😠", "#FF6B6B"),
    FEAR("Fear", "😰", "#4D96FF"),
    DISGUST("Disgust", "😖", "#9D84B7")
}



