package com.example.prog7314_universe.repo

import com.example.prog7314_universe.Models.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TaskDao {

    private val db = FirebaseFirestore.getInstance()
    private val tasksCollection = db.collection("tasks")

    // Add new task
    suspend fun addTask(task: Task) {
        val docRef = tasksCollection.document()
        task.id = docRef.id
        docRef.set(task).await()
    }

    // Update task
    suspend fun updateTask(task: Task) {
        if (task.id.isNotEmpty()) {
            tasksCollection.document(task.id).set(task).await()
        }
    }

    // Delete task
    suspend fun deleteTask(taskId: String) {
        tasksCollection.document(taskId).delete().await()
    }

    // Get all tasks (cached + real-time updates)
    fun listenForTasks(onTasksChanged: (List<Task>) -> Unit) {
        tasksCollection.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val tasks = snapshot?.toObjects(Task::class.java) ?: emptyList()
            onTasksChanged(tasks)
        }
    }
}
