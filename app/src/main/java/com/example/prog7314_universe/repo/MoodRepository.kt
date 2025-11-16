package com.example.prog7314_universe.repo

import com.example.prog7314_universe.Models.MoodEntry
import com.example.prog7314_universe.Models.MoodScale
import com.example.prog7314_universe.utils.getWithOfflineFallback
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class MoodRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val moodsCollection = db.collection("moods")

    /**
     * Get current user ID
     */
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    /**
     * Get all mood entries for current user as Flow
     */
    fun getAllMoods(): Flow<List<MoodEntry>> = callbackFlow {
        val userId = getCurrentUserId()

        val listener = moodsCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val moods = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MoodEntry::class.java)
                } ?: emptyList()

                trySend(moods)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get mood entry for specific date
     */
    suspend fun getMoodByDate(date: Date): MoodEntry? {
        val userId = getCurrentUserId()

        // Create start and end of day timestamps
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = Timestamp(calendar.time)

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = Timestamp(calendar.time)

        return try {
            val snapshot = moodsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .limit(1)
                .getWithOfflineFallback()

            snapshot.documents.firstOrNull()?.toObject(MoodEntry::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save or update mood entry
     */
    suspend fun saveMood(date: Date, moodScale: MoodScale, note: String? = null): Result<String> {
        return try {
            val userId = getCurrentUserId()

            // Check if mood already exists for this date
            val existingMood = getMoodByDate(date)

            if (existingMood != null) {
                // Update existing mood
                val updates = hashMapOf<String, Any>(
                    "scale" to moodScale.name,
                    "note" to (note ?: ""),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                moodsCollection.document(existingMood.moodId)
                    .update(updates)
                    .await()

                Result.success(existingMood.moodId)
            } else {
                // Create new mood
                val moodEntry = MoodEntry.create(userId, date, moodScale, note)
                val docRef = moodsCollection.add(moodEntry).await()
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete mood entry
     */
    suspend fun deleteMood(moodId: String): Result<Unit> {
        return try {
            moodsCollection.document(moodId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get moods for a specific month
     */
    suspend fun getMoodsForMonth(year: Int, month: Int): List<MoodEntry> {
        val userId = getCurrentUserId()

        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        val startOfMonth = Timestamp(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = Timestamp(calendar.time)

        return try {
            val snapshot = moodsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThanOrEqualTo("date", endOfMonth)
                .getWithOfflineFallback()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MoodEntry::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get moods for current week
     */
    suspend fun getMoodsForCurrentWeek(): List<MoodEntry> {
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
            val snapshot = moodsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startOfWeek)
                .whereLessThanOrEqualTo("date", endOfWeek)
                .getWithOfflineFallback()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MoodEntry::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}