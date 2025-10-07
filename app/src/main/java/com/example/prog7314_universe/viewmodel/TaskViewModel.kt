package com.example.prog7314_universe.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prog7314_universe.data.createTaskApi
import com.example.prog7314_universe.Models.Task
import com.example.prog7314_universe.data.TaskApiDataSource
import com.example.prog7314_universe.repo.TaskRepository
import kotlinx.coroutines.launch

class TaskViewModel : ViewModel() {
    private val repo = TaskRepository(
        TaskApiDataSource(createTaskApi())
    )

    val tasks = MutableLiveData<List<Task>>(emptyList())
    val loading = MutableLiveData(false)
    val error = MutableLiveData<String?>(null)

    fun refresh() = viewModelScope.launch {
        loading.value = true
        error.value = null
        try { tasks.value = repo.list() }
        catch (e: Exception) { error.value = e.message }
        finally { loading.value = false }
    }

    fun addTask(title: String, desc: String?, dueIso: String?) = viewModelScope.launch {
        try {
            repo.add(title, desc, dueIso)
            refresh()
        } catch (e: Exception) {
            error.value = e.message
        }
    }

    fun toggleCompleted(task: Task) = viewModelScope.launch {
        try {
            repo.update(task.copy(isCompleted = !task.isCompleted))
            refresh()
        } catch (e: Exception) { error.value = e.message }
    }

    fun remove(id: String) = viewModelScope.launch {
        try {
            repo.delete(id)
            refresh()
        } catch (e: Exception) { error.value = e.message }
    }
}
