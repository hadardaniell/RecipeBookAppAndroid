package com.example.recipebookappandorid.model

data class Recipe(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val prepTime: String = "",
    val difficulty: String = "",
    val category: String = "",
    val ingredients: String = "",
    val steps: String = "",
    val notes: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Long = 0L
)