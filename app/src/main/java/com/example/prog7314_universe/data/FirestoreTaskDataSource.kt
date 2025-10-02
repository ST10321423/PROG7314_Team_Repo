package com.example.prog7314_universe.data

import com.example.prog7314_universe.Models.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date

class FirestoreTaskDataSource {

    private val auth get() = Firebase.auth
    private val db = Firebase.firestore

    private fun col() = db.collection("tasks")
        .document(auth.currentUser!!.uid)
        .collection("items")

    // --- READ (list)
    suspend fun list(): List<Task> {
        val snap = col()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snap.documents.map { d ->
            Task(
                id = d.id,
                title = d.getString("title") ?: "",
                description = d.getString("description") ?: "",
                // Convert Firestore Timestamp -> ISO-8601 string (or "" if missing)
                dueDate = d.getTimestamp("dueDate")
                    ?.toDate()
                    ?.toInstant()
                    ?.toString()
                    ?: "",
                isCompleted = d.getBoolean("completed") == true
            )
        }
    }

    // --- CREATE (returns the created Task with generated id)
    suspend fun add(title: String, description: String?, dueIso: String?): Task {
        // Convert incoming ISO string (or null/blank) -> Firestore Timestamp
        val dueTs: Timestamp? = dueIso?.takeIf { it.isNotBlank() }?.let {
            Timestamp(Date.from(Instant.parse(it)))
        }

        val data = hashMapOf(
            "title" to title,
            "description" to (description ?: ""),
            "dueDate" to dueTs,                              // can be null
            "completed" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val ref = col().add(data).await()
        return Task(
            id = ref.id,
            title = title,
            description = description ?: "",
            dueDate = dueIso ?: "",
            isCompleted = false
        )
    }

    // --- UPDATE (partial via merge, expects Task.id not empty)
    suspend fun update(task: Task): Task {
        require(task.id.isNotBlank()) { "Task id required" }

        val patch = hashMapOf<String, Any>(
            "title" to task.title,
            "description" to task.description,
            "completed" to task.isCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // Handle dueDate: empty string means "no due date"
        if (task.dueDate.isBlank()) {
            patch["dueDate"] = FieldValue.delete()
        } else {
            patch["dueDate"] = Timestamp(Date.from(Instant.parse(task.dueDate)))
        }

        val docRef = col().document(task.id)
        docRef.set(patch, SetOptions.merge()).await()

        // Return the updated local model (no extra read needed)
        return task
    }

    // --- DELETE
    suspend fun delete(id: String) {
        require(id.isNotBlank()) { "Task id required" }
        col().document(id).delete().await()
    }
}
