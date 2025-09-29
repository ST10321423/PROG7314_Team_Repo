package com.example.taskapi.task

import org.springframework.stereotype.Service

@Service
class TaskService(private val repo: TaskRepository) {

    fun list(): List<Task> = repo.list()

    fun create(req: CreateTaskRequest): Task {
        val title = req.title.trim()
        require(title.isNotBlank()) { "Title is required" }
        return repo.create(Task(title = title, description = req.description, dueAt = req.dueAt))
    }

    fun update(id: String, req: UpdateTaskRequest): Task {
        val existing = repo.get(id) ?: throw NoSuchElementException("Task not found")
        val title = req.title.trim()
        require(title.isNotBlank()) { "Title is required" }
        val updated = existing.copy(
            title = title,
            description = req.description,
            isCompleted = req.isCompleted,
            dueAt = req.dueAt
        )
        return repo.update(id, updated) ?: throw NoSuchElementException("Task not found")
    }

    fun delete(id: String): Boolean = repo.delete(id)
}
