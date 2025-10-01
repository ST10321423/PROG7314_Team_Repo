package com.example.prog7314_universe.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.prog7314_universe.Models.SavingsGoal
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SavingsGoalRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeGoals(userId: String): LiveData<List<SavingsGoal>> {
        val live = MutableLiveData<List<SavingsGoal>>(emptyList())
        db.collection("users").document(userId).collection("savingsGoals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                live.value = snap.documents.mapNotNull { it.toObject(SavingsGoal::class.java)?.apply { id = it.id } }
            }
        return live
    }

    fun insert(goal: SavingsGoal, onDone: (Boolean) -> Unit) {
        val col = db.collection("users").document(goal.userOwnerId).collection("savingsGoals")
        if (goal.id.isBlank()) {
            col.add(goal).addOnSuccessListener { onDone(true) }.addOnFailureListener { onDone(false) }
        } else {
            col.document(goal.id).set(goal).addOnSuccessListener { onDone(true) }.addOnFailureListener { onDone(false) }
        }
    }

    fun update(userId: String, goal: SavingsGoal, onDone: (Boolean) -> Unit) {
        db.collection("users").document(userId).collection("savingsGoals")
            .document(goal.id).set(goal)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    fun delete(userId: String, goalId: String, onDone: (Boolean) -> Unit) {
        db.collection("users").document(userId).collection("savingsGoals")
            .document(goalId).delete()
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }
}
