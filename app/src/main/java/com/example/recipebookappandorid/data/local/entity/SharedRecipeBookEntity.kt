package com.example.recipebookappandorid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_books")
data class SharedRecipeBookEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val ownerId: String,
    val ownerName: String,
    val memberIds: List<String>,
    val memberNames: List<String>,
    val memberEmails: List<String>,
    val memberRoles: List<String>,
    val createdAt: Long
)
