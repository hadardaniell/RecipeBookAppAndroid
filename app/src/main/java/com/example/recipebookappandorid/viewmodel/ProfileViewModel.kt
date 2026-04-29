package com.example.recipebookappandorid.viewmodel

import android.app.Application
import android.net.Uri
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
            val localUser = userRepository.getUser(uid)

            if (localUser != null) {
                _user.postValue(localUser)
            } else {
                val fallbackUser = User(
                    uid = uid,
                    name = "",
                    email = firebaseUser.email ?: "",
                    profileImageUrl = ""
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
                    imageUrl = storageRepository.uploadProfileImage(imageUri)
                        ?: throw IllegalStateException("Failed to upload profile image")
                }

                val updatedUser = current.copy(name = name, profileImageUrl = imageUrl)
                userRepository.updateUser(updatedUser)
                _user.postValue(updatedUser)
                _saveSuccess.postValue(true)
            } catch (e: Exception) {
                _saveError.postValue(e.message ?: "Failed to update profile")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
