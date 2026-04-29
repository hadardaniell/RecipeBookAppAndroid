package com.example.recipebookappandorid.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.recipebookappandorid.data.local.AppDatabase
import com.example.recipebookappandorid.data.local.entity.SharedBookInviteEntity
import com.example.recipebookappandorid.data.local.entity.SharedRecipeBookEntity
import com.example.recipebookappandorid.model.SharedBookInvite
import com.example.recipebookappandorid.model.SharedBookInviteStatus
import com.example.recipebookappandorid.model.SharedBookMember
import com.example.recipebookappandorid.model.SharedBookRole
import com.example.recipebookappandorid.model.SharedRecipeBook
import com.example.recipebookappandorid.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SharedRecipeBookRepository(context: Context) {

    private val bookDao = AppDatabase.getInstance(context).sharedRecipeBookDao()
    private val inviteDao = AppDatabase.getInstance(context).sharedBookInviteDao()
    private val booksCollection = FirebaseFirestore.getInstance().collection("shared_books")
    private val invitesCollection = FirebaseFirestore.getInstance().collection("shared_book_invites")

    fun getCachedBooks(): LiveData<List<SharedRecipeBook>> {
        return bookDao.getAllBooks().map { books -> books.map { it.toModel() } }
    }

    fun getCachedInvites(): LiveData<List<SharedBookInvite>> {
        return inviteDao.getAllInvites().map { invites -> invites.map { it.toModel() } }
    }

    suspend fun syncForUser(userId: String, email: String) {
        val normalizedEmail = email.trim().lowercase()
        val booksSnapshot = booksCollection
            .whereArrayContains("memberIds", userId)
            .get()
            .await()

        val books = booksSnapshot.documents.mapNotNull { it.toObject(SharedRecipeBook::class.java) }
        bookDao.clearBooks()
        bookDao.insertBooks(books.map { it.toEntity() })

        val inviteQueries = listOfNotNull(
            invitesCollection.whereEqualTo("inviteeUserId", userId),
            normalizedEmail.takeIf { it.isNotBlank() }?.let {
                invitesCollection.whereEqualTo("inviteeEmail", it)
            }
        )

        val invites = inviteQueries
            .flatMap { query -> query.get().await().documents }
            .mapNotNull { it.toObject(SharedBookInvite::class.java) }
            .filter { it.status == SharedBookInviteStatus.PENDING }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }

        inviteDao.clearInvites()
        inviteDao.insertInvites(invites.map { it.toEntity() })
    }

    suspend fun createSharedBook(
        name: String,
        owner: User,
        invitations: List<Pair<String, String>>
    ): SharedRecipeBook {
        val ownerName = owner.name.ifBlank { owner.email }
        val book = SharedRecipeBook(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            ownerId = owner.uid,
            ownerName = ownerName,
            memberIds = listOf(owner.uid),
            memberNames = listOf(ownerName),
            memberEmails = listOf(owner.email),
            memberRoles = listOf(SharedBookRole.OWNER),
            createdAt = System.currentTimeMillis()
        )

        booksCollection.document(book.id).set(book).await()
        bookDao.insertBook(book.toEntity())

        invitations.forEach { (email, role) ->
            createInvite(book, owner, email, role)
        }

        return book
    }

    suspend fun createInvite(
        book: SharedRecipeBook,
        inviter: User,
        inviteeEmail: String,
        role: String
    ): SharedBookInvite {
        val normalizedEmail = inviteeEmail.trim().lowercase()
        val invite = SharedBookInvite(
            id = UUID.randomUUID().toString(),
            bookId = book.id,
            bookName = book.name,
            inviterId = inviter.uid,
            inviterName = inviter.name.ifBlank { inviter.email },
            inviteeEmail = normalizedEmail,
            role = role,
            status = SharedBookInviteStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        invitesCollection.document(invite.id).set(invite).await()
        return invite
    }

    suspend fun getBookById(bookId: String): SharedRecipeBook? {
        val cached = bookDao.getBookById(bookId)?.toModel()
        if (cached != null) return cached

        return booksCollection.document(bookId).get().await().toObject(SharedRecipeBook::class.java)
    }

    suspend fun getBooksForUser(userId: String, email: String): List<SharedRecipeBook> {
        syncForUser(userId, email)
        return bookDao.getAllBooksOnce().map { it.toModel() }
    }

    suspend fun acceptInvite(invite: SharedBookInvite, user: User): SharedRecipeBook {
        val book = getBookById(invite.bookId)
            ?: throw IllegalArgumentException("Shared book not found")

        val updatedBook = addOrUpdateMember(book, user, invite.role)
        booksCollection.document(updatedBook.id).set(updatedBook).await()
        invitesCollection.document(invite.id).update(
            mapOf(
                "status" to SharedBookInviteStatus.ACCEPTED,
                "inviteeUserId" to user.uid
            )
        ).await()
        bookDao.insertBook(updatedBook.toEntity())
        return updatedBook
    }

    suspend fun declineInvite(inviteId: String) {
        invitesCollection.document(inviteId)
            .update("status", SharedBookInviteStatus.DECLINED)
            .await()
    }

    suspend fun inviteMember(
        bookId: String,
        inviter: User,
        inviteeEmail: String,
        role: String
    ) {
        val book = getBookById(bookId) ?: throw IllegalArgumentException("Shared book not found")
        if (book.memberEmails.any { it.equals(inviteeEmail.trim(), ignoreCase = true) }) {
            throw IllegalArgumentException("This user is already a member")
        }
        createInvite(book, inviter, inviteeEmail, role)
    }

    suspend fun updateMemberRole(bookId: String, memberId: String, role: String): SharedRecipeBook {
        val book = getBookById(bookId) ?: throw IllegalArgumentException("Shared book not found")
        if (memberId == book.ownerId) {
            throw IllegalArgumentException("The owner role cannot be changed")
        }

        val index = book.memberIds.indexOf(memberId)
        if (index == -1) throw IllegalArgumentException("Member not found")

        val roles = book.memberRoles.toMutableList()
        roles[index] = role
        val updatedBook = book.copy(memberRoles = roles)
        booksCollection.document(bookId).set(updatedBook).await()
        bookDao.insertBook(updatedBook.toEntity())
        return updatedBook
    }

    suspend fun removeMember(bookId: String, memberId: String): SharedRecipeBook {
        val book = getBookById(bookId) ?: throw IllegalArgumentException("Shared book not found")
        if (memberId == book.ownerId) {
            throw IllegalArgumentException("The owner cannot be removed")
        }

        val updatedBook = book.removeMember(memberId)
        booksCollection.document(bookId).set(updatedBook).await()
        bookDao.insertBook(updatedBook.toEntity())
        return updatedBook
    }

    suspend fun leaveBook(bookId: String, userId: String): SharedRecipeBook {
        val book = getBookById(bookId) ?: throw IllegalArgumentException("Shared book not found")
        if (userId == book.ownerId) {
            throw IllegalArgumentException("The owner cannot leave the book")
        }

        val updatedBook = book.removeMember(userId)
        booksCollection.document(bookId).set(updatedBook).await()
        bookDao.insertBook(updatedBook.toEntity())
        return updatedBook
    }

    private fun addOrUpdateMember(
        book: SharedRecipeBook,
        user: User,
        role: String
    ): SharedRecipeBook {
        val index = book.memberIds.indexOf(user.uid)
        return if (index >= 0) {
            val roles = book.memberRoles.toMutableList()
            while (roles.size <= index) {
                roles.add(SharedBookRole.EDITOR)
            }
            roles[index] = role
            book.copy(memberRoles = roles)
        } else {
            book.copy(
                memberIds = book.memberIds + user.uid,
                memberNames = book.memberNames + user.name.ifBlank { user.email },
                memberEmails = book.memberEmails + user.email,
                memberRoles = book.memberRoles + role
            )
        }
    }

    private fun SharedRecipeBook.removeMember(userId: String): SharedRecipeBook {
        val index = memberIds.indexOf(userId)
        if (index == -1) return this

        return copy(
            memberIds = memberIds.toMutableList().also { it.removeAt(index) },
            memberNames = memberNames.toMutableList().also { if (it.size > index) it.removeAt(index) },
            memberEmails = memberEmails.toMutableList().also { if (it.size > index) it.removeAt(index) },
            memberRoles = memberRoles.toMutableList().also { if (it.size > index) it.removeAt(index) }
        )
    }

    private fun SharedRecipeBook.toEntity(): SharedRecipeBookEntity {
        return SharedRecipeBookEntity(
            id = id,
            name = name,
            ownerId = ownerId,
            ownerName = ownerName,
            memberIds = memberIds,
            memberNames = memberNames,
            memberEmails = memberEmails,
            memberRoles = memberRoles,
            createdAt = createdAt
        )
    }

    private fun SharedRecipeBookEntity.toModel(): SharedRecipeBook {
        return SharedRecipeBook(
            id = id,
            name = name,
            ownerId = ownerId,
            ownerName = ownerName,
            memberIds = memberIds,
            memberNames = memberNames,
            memberEmails = memberEmails,
            memberRoles = memberRoles,
            createdAt = createdAt
        )
    }

    private fun SharedBookInvite.toEntity(): SharedBookInviteEntity {
        return SharedBookInviteEntity(
            id = id,
            bookId = bookId,
            bookName = bookName,
            inviterId = inviterId,
            inviterName = inviterName,
            inviteeEmail = inviteeEmail,
            inviteeUserId = inviteeUserId,
            role = role,
            status = status,
            createdAt = createdAt
        )
    }

    private fun SharedBookInviteEntity.toModel(): SharedBookInvite {
        return SharedBookInvite(
            id = id,
            bookId = bookId,
            bookName = bookName,
            inviterId = inviterId,
            inviterName = inviterName,
            inviteeEmail = inviteeEmail,
            inviteeUserId = inviteeUserId,
            role = role,
            status = status,
            createdAt = createdAt
        )
    }
}
