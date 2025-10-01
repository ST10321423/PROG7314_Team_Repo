package com.example.prog7314_universe.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prog7314_universe.Models.SavingsGoal
import com.example.prog7314_universe.repo.SavingsGoalRepository

class SavingsGoalViewModel : ViewModel() {
    private val repo = SavingsGoalRepository()
    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    fun getSavingsGoals(userId: String): LiveData<List<SavingsGoal>> =
        repo.observeGoals(userId)

    fun insertSavingsGoal(goal: SavingsGoal, onDone: (Boolean) -> Unit) {
        repo.insert(goal) { ok -> _operationSuccess.value = ok; onDone(ok) }
    }

    fun saveGoal(userId: String, goal: SavingsGoal) {
        repo.update(userId, goal) { ok -> _operationSuccess.value = ok }
    }

    fun deleteGoal(userId: String, goalId: String) {
        repo.delete(userId, goalId) { ok -> _operationSuccess.value = ok }
    }
}
