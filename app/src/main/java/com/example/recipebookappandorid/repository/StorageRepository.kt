package com.example.recipebookappandorid.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class StorageRepository(context: Context) {

    private val appContext = context.applicationContext
    // Removed the hardcoded bucket URL. Using getInstance() with default configuration is safer
    // and prevents bucket URL mismatch errors.
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadProfileImage(uri: Uri): String? {
        return uploadImage(uri = uri, folder = "profiles")
    }

    suspend fun uploadRecipeImage(uri: Uri): String? {
        return uploadImage(uri = uri, folder = "recipes")
    }

    private suspend fun uploadImage(uri: Uri, folder: String): String? {
        var tempFile: File? = null
        return try {
            tempFile = copyUriToTempFile(uri, folder)
            val extension = tempFile.extension.ifBlank { "jpg" }
            val metadata = StorageMetadata.Builder()
                .setContentType(appContext.contentResolver.getType(uri) ?: "image/jpeg")
                .build()
            val ref = storage.reference.child("$folder/${UUID.randomUUID()}.$extension")

            ref.putFile(Uri.fromFile(tempFile), metadata).await()
            ref.downloadUrl.await().toString()
        } catch (error: StorageException) {
            throw IllegalStateException(storageErrorMessage(error), error)
        } catch (error: Exception) {
            throw IllegalStateException(error.message ?: "Image upload failed", error)
        } finally {
            tempFile?.delete()
        }
    }

    private fun copyUriToTempFile(uri: Uri, folder: String): File {
        val resolver = appContext.contentResolver
        val extension = resolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: "jpg"
        val tempFile = File.createTempFile("${folder}_upload_", ".$extension", appContext.cacheDir)

        resolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to read selected image")

        return tempFile
    }

    private fun storageErrorMessage(error: StorageException): String {
        return when (error.errorCode) {
            StorageException.ERROR_BUCKET_NOT_FOUND -> "Storage bucket was not found"
            StorageException.ERROR_NOT_AUTHENTICATED -> "You must be logged in to upload images"
            StorageException.ERROR_NOT_AUTHORIZED -> "Storage permission denied"
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> "Image upload timed out"
            else -> error.message ?: "Image upload failed"
        }
    }
}
