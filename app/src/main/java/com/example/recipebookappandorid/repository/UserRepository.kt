package com.example.recipebookappandorid.repository

import android.content.Context
import com.example.recipebookappandorid.data.local.AppDatabase
import com.example.recipebookappandorid.data.local.entity.UserEntity
import com.example.recipebookappandorid.model.User

class UserRepository(context: Context) {

    private val userDao = AppDatabase.getInstance(context).userDao()

    suspend fun saveUser(user: User) {
        val entity = UserEntity(
            uid = user.uid,
            name = user.name,
            email = user.email,
            profileImageUrl = user.profileImageUrl
        )
        userDao.insertUser(entity)
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

    suspend fun clearUsers() {
        userDao.clearUsers()
    }
}