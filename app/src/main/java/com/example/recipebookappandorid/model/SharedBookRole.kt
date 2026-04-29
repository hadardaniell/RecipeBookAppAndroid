package com.example.recipebookappandorid.model

object SharedBookRole {
    const val OWNER = "owner"
    const val EDITOR = "editor"
    const val VIEWER = "viewer"

    val all = listOf(OWNER, EDITOR, VIEWER)
}
