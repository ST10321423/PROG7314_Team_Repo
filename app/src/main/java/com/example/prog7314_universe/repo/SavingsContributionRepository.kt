package com.example.prog7314_universe.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.prog7314_universe.Models.SavingsContribution
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SavingsContributionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeContributions(userId: String, goalId: String): LiveData<List<SavingsContribution>> {
        val live = MutableLiveData<List<SavingsContribution>>(emptyList())
        db.collection("users").document(userId)
            .collection("savingsGoals").document(goalId)
            .collection("contributions")
            .orderBy("contributionDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                live.value = snap.documents.mapNotNull { it.toObject(SavingsContribution::class.java)?.apply { id = it.id } }
            }
        return live
    }

    fun add(userId: String, goalId: String, c: SavingsContribution, onDone: (Boolean) -> Unit) {
        val col = db.collection("users").document(userId)
            .collection("savingsGoals").document(goalId)
            .collection("contributions")
        if (c.id.isBlank()) {
            col.add(c).addOnSuccessListener { onDone(true) }.addOnFailureListener { onDone(false) }
        } else {
            col.document(c.id).set(c).addOnSuccessListener { onDone(true) }.addOnFailureListener { onDone(false) }
        }
    }

    fun delete(userId: String, goalId: String, contributionId: String, onDone: (Boolean) -> Unit) {
        db.collection("users").document(userId)
            .collection("savingsGoals").document(goalId)
            .collection("contributions").document(contributionId).delete()
            .addOnSuccessListener { onDone(true) }.addOnFailureListener { onDone(false) }
    }
}
