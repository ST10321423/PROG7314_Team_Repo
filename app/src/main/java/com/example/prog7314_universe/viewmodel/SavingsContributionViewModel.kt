package com.example.prog7314_universe.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prog7314_universe.Models.SavingsContribution
import com.example.prog7314_universe.repo.SavingsContributionRepository

class SavingsContributionViewModel : ViewModel() {
    private val repo = SavingsContributionRepository()
    private val _opSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _opSuccess

    fun getContributions(userId: String, goalId: String): LiveData<List<SavingsContribution>> =
        repo.observeContributions(userId, goalId)

    fun addContribution(userId: String, goalId: String, c: SavingsContribution, onDone: (Boolean) -> Unit) {
        repo.add(userId, goalId, c) { ok -> _opSuccess.value = ok; onDone(ok) }
    }

    fun deleteContribution(userId: String, goalId: String, contributionId: String, onDone: (Boolean) -> Unit) {
        repo.delete(userId, goalId, contributionId) { ok -> _opSuccess.value = ok; onDone(ok) }
    }
}
