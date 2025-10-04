package com.example.prog7314_universe.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.prog7314_universe.Models.Exam
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ExamRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun examsCollection() = getCurrentUserId()?.let { uid ->
        db.collection("users").document(uid).collection("exams")
    }

    fun observeExams(): LiveData<List<Exam>> {
        val liveData = MutableLiveData<List<Exam>>(emptyList())

        examsCollection()?.orderBy("date", Query.Direction.ASCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                val exams = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Exam::class.java)?.apply { id = doc.id }
                }
                liveData.value = exams
            }

        return liveData
    }

    fun addExam(exam: Exam, onComplete: (Boolean, String?) -> Unit) {
        val collection = examsCollection()
        if (collection == null) {
            onComplete(false, "User not logged in")
            return
        }

        val examData = exam.copy(
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        collection.add(examData)
            .addOnSuccessListener { documentReference ->
                onComplete(true, documentReference.id)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message)
            }
    }

    fun updateExam(exam: Exam, onComplete: (Boolean, String?) -> Unit) {
        val collection = examsCollection()
        if (collection == null) {
            onComplete(false, "User not logged in")
            return
        }

        if (exam.id.isBlank()) {
            onComplete(false, "Exam ID is required")
            return
        }

        val examData = exam.copy(updatedAt = Timestamp.now())

        collection.document(exam.id)
            .set(examData)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message)
            }
    }

    fun deleteExam(examId: String, onComplete: (Boolean, String?) -> Unit) {
        val collection = examsCollection()
        if (collection == null) {
            onComplete(false, "User not logged in")
            return
        }

        collection.document(examId)
            .delete()
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message)
            }
    }

    fun toggleCompleted(examId: String, isCompleted: Boolean, onComplete: (Boolean, String?) -> Unit) {
        val collection = examsCollection()
        if (collection == null) {
            onComplete(false, "User not logged in")
            return
        }

        collection.document(examId)
            .update(mapOf(
                "isCompleted" to isCompleted,
                "updatedAt" to Timestamp.now()
            ))
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.message)
            }
    }
}