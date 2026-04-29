package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.SharedBookInvite
import com.example.recipebookappandorid.model.SharedBookRole
import com.example.recipebookappandorid.model.SharedRecipeBook
import com.example.recipebookappandorid.model.User
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.SharedRecipeBookRepository
import com.example.recipebookappandorid.repository.UserRepository
import kotlinx.coroutines.launch

class SharedBooksViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(application)
    private val sharedRecipeBookRepository = SharedRecipeBookRepository(application)

    val books: LiveData<List<SharedRecipeBook>> = sharedRecipeBookRepository.getCachedBooks().map { books ->
        books.filterNot { it.`private` }
    }
    val invites: LiveData<List<SharedBookInvite>> = sharedRecipeBookRepository.getCachedInvites()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        sync()
    }

    fun sync() {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                sharedRecipeBookRepository.syncForUser(
                    userId = firebaseUser.uid,
                    email = firebaseUser.email.orEmpty()
                )
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to sync shared books")
            }
            _isLoading.postValue(false)
        }
    }

    fun createBook(name: String, invitationsInput: List<Pair<String, String>>) {
        _message.value = null
        if (name.isBlank()) {
            _message.value = "Book name is required"
            return
        }

        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser == null) {
            _message.value = "You must be logged in"
            return
        }

        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                val owner = currentUserModel(firebaseUser.uid, firebaseUser.email.orEmpty(), firebaseUser.displayName.orEmpty())
                sharedRecipeBookRepository.createSharedBook(name, owner, invitationsInput)
                sharedRecipeBookRepository.syncForUser(firebaseUser.uid, firebaseUser.email.orEmpty())
            }.onSuccess {
                _message.postValue("Shared book created")
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to create shared book")
            }
            _isLoading.postValue(false)
        }
    }

    fun acceptInvite(invite: SharedBookInvite) {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                val user = currentUserModel(firebaseUser.uid, firebaseUser.email.orEmpty(), firebaseUser.displayName.orEmpty())
                sharedRecipeBookRepository.acceptInvite(invite, user)
                sharedRecipeBookRepository.syncForUser(firebaseUser.uid, firebaseUser.email.orEmpty())
            }.onSuccess {
                _message.postValue("Invitation accepted")
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to accept invitation")
            }
            _isLoading.postValue(false)
        }
    }

    fun declineInvite(invite: SharedBookInvite) {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                sharedRecipeBookRepository.declineInvite(invite.id)
                sharedRecipeBookRepository.syncForUser(firebaseUser.uid, firebaseUser.email.orEmpty())
            }.onSuccess {
                _message.postValue("Invitation declined")
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to decline invitation")
            }
            _isLoading.postValue(false)
        }
    }

    fun defaultInviteRole(): String = SharedBookRole.EDITOR

    private suspend fun currentUserModel(uid: String, email: String, displayName: String): User {
        return userRepository.getUser(uid) ?: User(
            uid = uid,
            name = displayName,
            email = email,
            profileImageUrl = ""
        )
    }
}
