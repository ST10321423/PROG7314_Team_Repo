package com.example.prog7314_universe.Models

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    val isCompleted: Boolean = false
)
