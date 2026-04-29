package com.example.recipebookappandorid.model

data class SharedRecipeBook(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val memberIds: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val memberEmails: List<String> = emptyList(),
    val memberRoles: List<String> = emptyList(),
    val createdAt: Long = 0L
) {
    fun members(): List<SharedBookMember> {
        return memberIds.indices.map { index ->
            SharedBookMember(
                userId = memberIds.getOrElse(index) { "" },
                name = memberNames.getOrElse(index) { "" },
                email = memberEmails.getOrElse(index) { "" },
                role = memberRoles.getOrElse(index) {
                    if (memberIds.getOrElse(index) { "" } == ownerId) SharedBookRole.OWNER else SharedBookRole.EDITOR
                }
            )
        }
    }

    fun roleFor(userId: String): String? {
        val index = memberIds.indexOf(userId)
        if (index == -1) return null
        return memberRoles.getOrElse(index) {
            if (userId == ownerId) SharedBookRole.OWNER else SharedBookRole.EDITOR
        }
    }
}
