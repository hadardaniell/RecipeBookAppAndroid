package com.example.recipebookappandorid.repository

import android.content.Context
import com.example.recipebookappandorid.data.local.AppDatabase
import com.example.recipebookappandorid.data.local.entity.UserEntity
import com.example.recipebookappandorid.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(context: Context) {

    private val userDao = AppDatabase.getInstance(context).userDao()
    private val usersCollection = FirebaseFirestore.getInstance().collection("users")

    suspend fun saveUser(user: User) {
        val entity = UserEntity(
            uid = user.uid,
            name = user.name,
            email = user.email,
            profileImageUrl = user.profileImageUrl
        )
        userDao.insertUser(entity)
        usersCollection.document(user.uid).set(user).await()
    }

    suspend fun getUser(uid: String): User? {
        val entity = userDao.getUser(uid) ?: return null

        return User(
            uid = entity.uid,
            name = entity.name,
            email = entity.email,
            profileImageUrl = entity.profileImageUrl
        )
    }

    suspend fun updateUser(user: User) {
        val entity = UserEntity(
            uid = user.uid,
            name = user.name,
            email = user.email,
            profileImageUrl = user.profileImageUrl
        )
        userDao.updateUser(entity)
        usersCollection.document(user.uid).set(user).await()
    }

    suspend fun clearUsers() {
        userDao.clearUsers()
    }

    suspend fun findUserByEmail(email: String): User? {
        val snapshot = usersCollection
            .whereEqualTo("email", email.trim())
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }
}
