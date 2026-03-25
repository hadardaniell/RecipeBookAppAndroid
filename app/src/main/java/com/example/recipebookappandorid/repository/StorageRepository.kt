package com.example.recipebookappandorid.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository {

    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadProfileImage(uri: Uri): String? {
        return try {
            val fileName = "profiles/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)
            
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun uploadRecipeImage(uri: Uri): String? {
        return try {
            val fileName = "recipes/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)
            
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}