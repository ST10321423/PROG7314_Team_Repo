package com.example.prog7314_universe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.prog7314_universe.Models.Exam
import com.example.prog7314_universe.repo.ExamRepository
import com.example.prog7314_universe.utils.ReminderScheduler

class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ExamRepository()
    private val reminderScheduler = ReminderScheduler(application.applicationContext)

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
                // Schedule notifications for the new exam
                scheduleExamNotifications(exam)
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
                // Cancel old notifications and reschedule
                reminderScheduler.cancelExamReminders(exam.id)
                scheduleExamNotifications(exam)
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
                // Cancel all notifications for this exam
                reminderScheduler.cancelExamReminders(examId)
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
            } else {
                // If exam is marked as completed, cancel its reminders
                if (isCompleted) {
                    reminderScheduler.cancelExamReminders(examId)
                }
            }
        }
    }

    /**
     * Schedule notifications for an exam
     * Notifies at: 7 days, 3 days, 1 day before, and on the exam day
     */
    private fun scheduleExamNotifications(exam: Exam) {
        reminderScheduler.scheduleExamReminder(
            examId = exam.id,
            examSubject = exam.subject,
            examDate = exam.date,
            examStartTime = exam.startTime,
            reminderDaysBefore = listOf(7, 3, 1) // Customize as needed
        )
    }
}