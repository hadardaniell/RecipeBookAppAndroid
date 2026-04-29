package com.example.recipebookappandorid.model

data class SharedBookInvite(
    val id: String = "",
    val bookId: String = "",
    val bookName: String = "",
    val inviterId: String = "",
    val inviterName: String = "",
    val inviteeEmail: String = "",
    val inviteeUserId: String = "",
    val role: String = SharedBookRole.EDITOR,
    val status: String = SharedBookInviteStatus.PENDING,
    val createdAt: Long = 0L
)
