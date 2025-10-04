package com.example.prog7314_universe.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prog7314_universe.Models.Exam
import com.example.prog7314_universe.repo.ExamRepository

class ExamViewModel : ViewModel() {

    private val repo = ExamRepository()

    private val _exams = MutableLiveData<List<Exam>>(emptyList())
    val exams: LiveData<List<Exam>> = _exams

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    init {
        loadExams()
    }

    private fun loadExams() {
        repo.observeExams().observeForever { examList ->
            _exams.value = examList
        }
    }

    fun addExam(exam: Exam) {
        _loading.value = true
        repo.addExam(exam) { success, errorMessage ->
            _loading.value = false
            if (success) {
                _operationSuccess.value = true
            } else {
                _error.value = errorMessage ?: "Failed to add exam"
            }
        }
    }

    fun updateExam(exam: Exam) {
        _loading.value = true
        repo.updateExam(exam) { success, errorMessage ->
            _loading.value = false
            if (success) {
                _operationSuccess.value = true
            } else {
                _error.value = errorMessage ?: "Failed to update exam"
            }
        }
    }

    fun deleteExam(examId: String) {
        _loading.value = true
        repo.deleteExam(examId) { success, errorMessage ->
            _loading.value = false
            if (success) {
                _operationSuccess.value = true
            } else {
                _error.value = errorMessage ?: "Failed to delete exam"
            }
        }
    }

    fun toggleCompleted(examId: String, isCompleted: Boolean) {
        repo.toggleCompleted(examId, isCompleted) { success, errorMessage ->
            if (!success) {
                _error.value = errorMessage ?: "Failed to update exam"
            }
        }
    }
}