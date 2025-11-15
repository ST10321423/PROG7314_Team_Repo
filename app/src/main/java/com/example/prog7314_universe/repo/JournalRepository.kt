package com.example.prog7314_universe.repo

import android.net.Uri
import com.example.prog7314_universe.Models.JournalEntry
import com.example.prog7314_universe.utils.getWithOfflineFallback
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
class JournalRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val journalCollection = db.collection("journals")
    private val storageRef = storage.reference.child("journal_images")

    /**
     * Get current user ID
     */
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    /**
     * Get all journal entries for current user as Flow
     */
    fun getAllEntries(): Flow<List<JournalEntry>> = callbackFlow {
        val userId = getCurrentUserId()

        val listener = journalCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(JournalEntry::class.java)?.copy(entryId = doc.id)
                } ?: emptyList()

                trySend(entries)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get journal entry by ID
     */
    suspend fun getEntryById(entryId: String): JournalEntry? {
        return try {
            val snapshot = journalCollection.document(entryId).getWithOfflineFallback()
            snapshot.toObject(JournalEntry::class.java)?.copy(entryId = snapshot.id)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create new journal entry with optional image
     */
    suspend fun createEntry(
        title: String,
        content: String,
        imageUri: Uri? = null
    ): Result<String> {
        return try {
            val userId = getCurrentUserId()

            // Upload image if provided
            val imageUrl = imageUri?.let { uploadImage(it) }

            // Create entry data
            val entryData = hashMapOf(
                "userId" to userId,
                "title" to title,
                "content" to content,
                "imageUri" to imageUrl,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val docRef = journalCollection.add(entryData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update existing journal entry
     */
    suspend fun updateEntry(
        entryId: String,
        title: String,
        content: String,
        imageUri: Uri? = null,
        keepExistingImage: Boolean = false
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "title" to title,
                "content" to content,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            // Handle image update
            if (imageUri != null) {
                // Upload new image
                val imageUrl = uploadImage(imageUri)
                updates["imageUri"] = imageUrl

                // Delete old image if exists
                if (!keepExistingImage) {
                    val oldEntry = getEntryById(entryId)
                    oldEntry?.imageUri?.let { deleteImage(it) }
                }
            } else if (!keepExistingImage) {
                // Remove image
                updates["imageUri"] = ""

                // Delete old image
                val oldEntry = getEntryById(entryId)
                oldEntry?.imageUri?.let { deleteImage(it) }
            }

            journalCollection.document(entryId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete journal entry
     */
    suspend fun deleteEntry(entryId: String): Result<Unit> {
        return try {
            // Get entry to delete associated image
            val entry = getEntryById(entryId)
            entry?.imageUri?.let { deleteImage(it) }

            // Delete entry document
            journalCollection.document(entryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get entries for specific date
     */
    suspend fun getEntriesForDate(date: Date): List<JournalEntry> {
        val userId = getCurrentUserId()

        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = Timestamp(calendar.time)

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = Timestamp(calendar.time)

        return try {
            val snapshot = journalCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .getWithOfflineFallback()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(JournalEntry::class.java)?.copy(entryId = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get entries for current week
     */
    suspend fun getEntriesForCurrentWeek(): List<JournalEntry> {
        val userId = getCurrentUserId()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfWeek = Timestamp(calendar.time)

        calendar.add(Calendar.DAY_OF_WEEK, 7)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfWeek = Timestamp(calendar.time)

        return try {
            val snapshot = journalCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("createdAt", startOfWeek)
                .whereLessThanOrEqualTo("createdAt", endOfWeek)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .getWithOfflineFallback()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(JournalEntry::class.java)?.copy(entryId = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Search entries by title or content
     */
    suspend fun searchEntries(query: String): List<JournalEntry> {
        val userId = getCurrentUserId()

        return try {
            val snapshot = journalCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .getWithOfflineFallback()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(JournalEntry::class.java)?.copy(entryId = doc.id)
            }.filter { entry ->
                entry.title.contains(query, ignoreCase = true) ||
                        entry.content.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Upload image to Firebase Storage
     */
    private suspend fun uploadImage(imageUri: Uri): String {
        val userId = getCurrentUserId()
        val fileName = "${UUID.randomUUID()}.jpg"
        val fileRef = storageRef.child("$userId/$fileName")

        fileRef.putFile(imageUri).await()
        return fileRef.downloadUrl.await().toString()
    }

    /**
     * Delete image from Firebase Storage
     */
    private suspend fun deleteImage(imageUrl: String) {
        try {
            val fileRef = storage.getReferenceFromUrl(imageUrl)
            fileRef.delete().await()
        } catch (e: Exception) {
            // Image deletion failed, but continue
        }
    }
}
