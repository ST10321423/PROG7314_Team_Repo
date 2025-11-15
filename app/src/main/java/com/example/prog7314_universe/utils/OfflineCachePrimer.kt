package com.example.prog7314_universe.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

/**
 * Keeps the most important Firestore collections synced locally so that the UI can
 * continue to function when the device is offline.
 */
object OfflineCachePrimer {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val registrations = mutableListOf<ListenerRegistration>()
    private val habitLogRegistrations = mutableMapOf<String, ListenerRegistration>()
    private val goalContributionRegistrations = mutableMapOf<String, ListenerRegistration>()

    private var primedUserId: String? = null

    fun start(userId: String) {
        if (primedUserId == userId) return
        stop()
        primedUserId = userId

        val userDoc = firestore.collection("users").document(userId)
        registrations += keepSynced(userDoc.collection("tasks").limit(200))
        registrations += keepSynced(userDoc.collection("habits").limit(200)) { snapshot ->
            val habitIds = snapshot?.documents?.map { it.id }.orEmpty()
            updateSubListeners(
                habitIds,
                habitLogRegistrations
            ) { habitId ->
                userDoc.collection("habits").document(habitId)
                    .collection("logs")
                    .orderBy("completedAt", Query.Direction.DESCENDING)
                    .limit(30)
                    .addSnapshotListener(MetadataChanges.INCLUDE) { _, error ->
                        if (error != null) Log.w(TAG, "Habit log cache listener error", error)
                    }
            }
        }
        registrations += keepSynced(userDoc.collection("exams").limit(200))
        registrations += keepSynced(userDoc.collection("savingsGoals").limit(200)) { snapshot ->
            val goalIds = snapshot?.documents?.map { it.id }.orEmpty()
            updateSubListeners(
                goalIds,
                goalContributionRegistrations
            ) { goalId ->
                userDoc.collection("savingsGoals").document(goalId)
                    .collection("contributions")
                    .orderBy("contributionDate", Query.Direction.DESCENDING)
                    .limit(100)
                    .addSnapshotListener(MetadataChanges.INCLUDE) { _, error ->
                        if (error != null) Log.w(TAG, "Contribution cache listener error", error)
                    }
            }
        }

        // Global collections filtered by user id
        registrations += keepSynced(
            firestore.collection("journals")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
        )
        registrations += keepSynced(
            firestore.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(200)
        )
    }

    fun stop() {
        registrations.forEach { it.remove() }
        registrations.clear()
        habitLogRegistrations.values.forEach { it.remove() }
        habitLogRegistrations.clear()
        goalContributionRegistrations.values.forEach { it.remove() }
        goalContributionRegistrations.clear()
        primedUserId = null
    }

    private fun keepSynced(
        query: Query,
        onSnapshot: ((QuerySnapshot?) -> Unit)? = null
    ): ListenerRegistration {
        return query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Offline cache priming listener error", error)
                return@addSnapshotListener
            }
            onSnapshot?.invoke(snapshot)
        }
    }

    private fun updateSubListeners(
        currentIds: List<String>,
        registry: MutableMap<String, ListenerRegistration>,
        create: (String) -> ListenerRegistration
    ) {
        val currentSet = currentIds.toSet()
        currentSet.filterNot { registry.containsKey(it) }.forEach { id ->
            registry[id] = create(id)
        }
        val iterator = registry.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!currentSet.contains(entry.key)) {
                entry.value.remove()
                iterator.remove()
            }
        }
    }

    private const val TAG = "OfflineCachePrimer"
}