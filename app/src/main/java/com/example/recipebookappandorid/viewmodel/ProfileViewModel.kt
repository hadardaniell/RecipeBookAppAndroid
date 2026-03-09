package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.User
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(application)

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    fun loadCurrentUser() {
        val currentUser = authRepository.getCurrentUser() ?: return
        val uid = currentUser.uid

        viewModelScope.launch {
            val localUser = userRepository.getUser(uid)
            _user.postValue(localUser)
        }
    }
}