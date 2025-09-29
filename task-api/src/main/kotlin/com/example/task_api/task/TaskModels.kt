package com.example.taskapi.task

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val dueAt: Instant? = null
)

data class CreateTaskRequest(
    @field:NotBlank(message = "Title is required")
    val title: String,
    val description: String? = null,
    val dueAt: Instant? = null
)

data class UpdateTaskRequest(
    @field:NotBlank(message = "Title is required")
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val dueAt: Instant? = null
)
