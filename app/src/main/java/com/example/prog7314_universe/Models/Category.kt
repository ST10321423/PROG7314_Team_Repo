package com.example.prog7314_universe.Models

data class Category(
    var id: String = "",
    var userOwnerId: String = "",
    var name: String = "",
    var description: String = "",
    var limit: Double = 0.0,
    var spendType: String = ""
)
