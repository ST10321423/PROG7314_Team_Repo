package com.example.prog7314_universe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.prog7314_universe.data.createTaskApi
import com.example.prog7314_universe.Models.Task
import com.example.prog7314_universe.data.TaskApiDataSource
import com.example.prog7314_universe.repo.TaskRepository
import com.example.prog7314_universe.utils.ReminderScheduler
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = TaskRepository(
        TaskApiDataSource(createTaskApi())
    )

    private val reminderScheduler = ReminderScheduler(application.applicationContext)

    val tasks = MutableLiveData<List<Task>>(emptyList())
    val loading = MutableLiveData(false)
    val error = MutableLiveData<String?>(null)

    fun refresh() = viewModelScope.launch {
        loading.value = true
        error.value = null
        try {
            tasks.value = repo.list()
        } catch (e: Exception) {
            error.value = e.message
        } finally {
            loading.value = false
        }
    }

    fun addTask(title: String, desc: String?, dueIso: String?) = viewModelScope.launch {
        try {
            repo.add(title, desc, dueIso)
            refresh()

            // Schedule notifications if due date is provided
            if (!dueIso.isNullOrBlank()) {
                // Get the newly added task to get its ID
                val newTasks = repo.list()
                val newTask = newTasks.firstOrNull {
                    it.title == title && it.description == desc
                }

                newTask?.let { task ->
                    scheduleTaskNotifications(task)
                }
            }
        } catch (e: Exception) {
            error.value = e.message
        }
    }

    fun toggleCompleted(task: Task) = viewModelScope.launch {
        try {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            repo.update(updatedTask)

            // Cancel notifications if task is marked as completed
            if (updatedTask.isCompleted) {
                reminderScheduler.cancelTaskReminders(task.id)
            } else if (!task.due.isNullOrBlank()) {
                // Reschedule if uncompleted
                scheduleTaskNotifications(updatedTask)
            }

            refresh()
        } catch (e: Exception) {
            error.value = e.message
        }
    }

    fun remove(id: String) = viewModelScope.launch {
        try {
            // Cancel notifications before deleting
            reminderScheduler.cancelTaskReminders(id)
            repo.delete(id)
            refresh()
        } catch (e: Exception) {
            error.value = e.message
        }
    }

    /**
     * Schedule notifications for a task
     * Notifies at: 24 hours, 6 hours, and 1 hour before due
     */
    private fun scheduleTaskNotifications(task: Task) {
        if (!task.due.isNullOrBlank()) {
            reminderScheduler.scheduleTaskReminder(
                taskId = task.id,
                taskTitle = task.title,
                dueDate = task.due,
                reminderHoursBefore = listOf(24, 6, 1) // Customize as needed
            )
        }
    }
}