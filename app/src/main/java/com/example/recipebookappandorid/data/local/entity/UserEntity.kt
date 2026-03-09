package com.example.recipebookappandorid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(

    @PrimaryKey
    val uid: String,

    val name: String,

    val email: String,

    val profileImageUrl: String
)