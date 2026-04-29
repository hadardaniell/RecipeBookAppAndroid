package com.example.recipebookappandorid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_book_invites")
data class SharedBookInviteEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val bookName: String,
    val inviterId: String,
    val inviterName: String,
    val inviteeEmail: String,
    val inviteeUserId: String,
    val role: String,
    val status: String,
    val createdAt: Long
)
