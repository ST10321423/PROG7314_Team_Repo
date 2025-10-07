package com.example.task_api.task

// ✅ USE THESE (Jakarta)
import jakarta.persistence.*
import java.time.LocalDateTime


@Entity
@Table(name = "tasks")
data class Task(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var userId: String = "",

    @Column(nullable = false)
    var title: String = "",

    @Column(length = 1000)
    var description: String? = null,

    var dueDate: String? = null,

    var priority: String = "Medium",

    var isCompleted: Boolean = false,

    @Column(updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)