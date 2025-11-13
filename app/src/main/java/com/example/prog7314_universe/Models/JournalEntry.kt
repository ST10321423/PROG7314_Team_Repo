package com.example.prog7314_universe.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class JournalEntry(
    @DocumentId
    val entryId: String = "",

    val userId: String = "",

    val title: String = "",

    val content: String = "",

    val imageUri: String? = null,

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    @ServerTimestamp
    val updatedAt: Timestamp? = null
){
    constructor() : this("", "", "", "", null, null, null)


    fun getFormattedDate(): String {
        val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy - h:mm a", java.util.Locale.getDefault())
        return createdAt?.toDate()?.let { dateFormat.format(it) } ?: ""
    }

    fun getPreviewText(): String {
        return if (content.length > 100) {
            content.substring(0, 100) + "..."
        } else {
            content
        }
    }
    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "userId" to userId,
            "title" to title,
            "content" to content,
            "imageUri" to imageUri,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
    }
}
