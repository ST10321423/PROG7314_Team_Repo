package com.example.task_api.task

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = ["*"]) // Allow Android app to access
class TaskController(private val taskRepository: TaskRepository) {

    // Get all tasks for a user
    @GetMapping("/user/{userId}")
    fun getTasksByUserId(@PathVariable userId: String): ResponseEntity<List<Task>> {
        val tasks = taskRepository.findByUserId(userId)
        return ResponseEntity.ok(tasks)
    }

    // Create a new task
    @PostMapping
    fun createTask(@RequestBody task: Task): ResponseEntity<Task> {
        task.createdAt = java.time.LocalDateTime.now()
        task.updatedAt = java.time.LocalDateTime.now()
        val savedTask = taskRepository.save(task)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask)
    }

    // Update a task
    @PutMapping("/{id}")
    fun updateTask(@PathVariable id: Long, @RequestBody taskDetails: Task): ResponseEntity<Task> {
        val task = taskRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        task.apply {
            title = taskDetails.title
            description = taskDetails.description
            dueDate = taskDetails.dueDate
            priority = taskDetails.priority
            isCompleted = taskDetails.isCompleted
            updatedAt = java.time.LocalDateTime.now()
        }

        val updatedTask = taskRepository.save(task)
        return ResponseEntity.ok(updatedTask)
    }

    // Delete a task
    @DeleteMapping("/{id}")
    fun deleteTask(@PathVariable id: Long): ResponseEntity<Void> {
        return if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // Toggle task completion
    @PatchMapping("/{id}/complete")
    fun toggleComplete(@PathVariable id: Long): ResponseEntity<Task> {
        val task = taskRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        task.isCompleted = !task.isCompleted
        task.updatedAt = java.time.LocalDateTime.now()
        val updatedTask = taskRepository.save(task)
        return ResponseEntity.ok(updatedTask)
    }
}