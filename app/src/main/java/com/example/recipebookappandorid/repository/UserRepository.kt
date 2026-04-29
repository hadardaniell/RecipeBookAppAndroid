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
        val localUser = userDao.getUser(uid)?.let { entity ->
            User(
                uid = entity.uid,
                name = entity.name,
                email = entity.email,
                profileImageUrl = entity.profileImageUrl
            )
        }

        if (localUser != null) {
            return localUser
        }

        val remoteUser = usersCollection.document(uid).get().await().toObject(User::class.java)
        if (remoteUser != null) {
            userDao.insertUser(
                UserEntity(
                    uid = remoteUser.uid,
                    name = remoteUser.name,
                    email = remoteUser.email,
                    profileImageUrl = remoteUser.profileImageUrl
                )
            )
        }

        return remoteUser
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
        val normalizedEmail = email.trim().lowercase()
        val snapshot = usersCollection
            .whereEqualTo("email", normalizedEmail)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }
}
