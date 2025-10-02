package com.example.prog7314_universe.Models

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",      // keep as ISO-8601 string ("" = no due date)
    val isCompleted: Boolean = false
)
