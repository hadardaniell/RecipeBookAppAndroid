package com.example.recipebookappandorid.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.User
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.StorageRepository
import com.example.recipebookappandorid.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(application)
    private val storageRepository = StorageRepository(application)

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _nameError = MutableLiveData<String?>()
    val nameError: LiveData<String?> = _nameError

    private val _saveError = MutableLiveData<String?>()
    val saveError: LiveData<String?> = _saveError

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadCurrentUser() {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        val uid = firebaseUser.uid

        viewModelScope.launch {
            // First check what we have in the local DB/Firestore
            val localUser = userRepository.getUser(uid)

            if (localUser != null) {
                // If it exists but has a different profileImageUrl in Firebase Auth, we could sync it, 
                // but let's trust our Firestore/Room database as the source of truth for the image URL.
                _user.postValue(localUser)
            } else {
                val fallbackUser = User(
                    uid = uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
                )
                userRepository.saveUser(fallbackUser)
                _user.postValue(fallbackUser)
            }
        }
    }

    fun updateProfile(name: String, imageUri: Uri?) {
        _nameError.value = null
        _saveError.value = null

        val current = _user.value ?: return

        if (name.isBlank()) {
            _nameError.value = "Name is required"
            return
        }

        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                var imageUrl = current.profileImageUrl

                if (imageUri != null) {
                    // Start upload
                    Log.d("ProfileViewModel", "Starting image upload for: $imageUri")
                    val uploadedUrl = storageRepository.uploadProfileImage(imageUri)
                    
                    if (uploadedUrl != null) {
                        Log.d("ProfileViewModel", "Image uploaded successfully. URL: $uploadedUrl")
                        imageUrl = uploadedUrl
                    } else {
                        Log.e("ProfileViewModel", "Upload returned null URL")
                        throw IllegalStateException("Failed to get URL after upload")
                    }
                }

                // Create the updated user object with the NEW imageUrl
                val updatedUser = current.copy(name = name, profileImageUrl = imageUrl)
                
                // Save it to Room AND Firestore!
                userRepository.updateUser(updatedUser)
                
                // Push the new user to the UI immediately
                _user.postValue(updatedUser)
                
                _saveSuccess.postValue(true)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile", e)
                _saveError.postValue(e.message ?: "Failed to update profile")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
