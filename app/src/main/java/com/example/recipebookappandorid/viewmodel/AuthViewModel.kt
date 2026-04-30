package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.User
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.UserRepository
import com.example.recipebookappandorid.validation.AuthFormValidator
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(application)

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    private val _nameError = MutableLiveData<String?>()
    val nameError: LiveData<String?> = _nameError

    private val _confirmPasswordError = MutableLiveData<String?>()
    val confirmPasswordError: LiveData<String?> = _confirmPasswordError

    private val _loginError = MutableLiveData<String?>()
    val loginError: LiveData<String?> = _loginError

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _registerError = MutableLiveData<String?>()
    val registerError: LiveData<String?> = _registerError

    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    fun login(email: String, password: String) {
        _emailError.value = null
        _passwordError.value = null
        _loginError.value = null

        val validation = AuthFormValidator.validateLogin(email, password)
        _emailError.value = validation.emailError
        _passwordError.value = validation.passwordError
        if (!validation.isValid) return

        val normalizedEmail = email.trim().lowercase()
        _loading.value = true

        authRepository.login(
            email = normalizedEmail,
            password = password,
            onSuccess = {
                val firebaseUser = authRepository.getCurrentUser()
                val uid = firebaseUser?.uid.orEmpty()

                viewModelScope.launch {
                    try {
                        val existingUser = if (uid.isNotEmpty()) userRepository.getUser(uid) else null

                        if (uid.isNotEmpty() && existingUser == null) {
                            val user = User(
                                uid = uid,
                                name = firebaseUser?.displayName.orEmpty(),
                                email = firebaseUser?.email?.trim()?.lowercase() ?: normalizedEmail,
                                profileImageUrl = ""
                            )
                            userRepository.saveUser(user)
                        }

                        _loading.postValue(false)
                        _loginSuccess.postValue(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _loading.postValue(false)
                        // If it fails to fetch/save from firestore, still log them in
                        _loginSuccess.postValue(true) 
                    }
                }
            },
            onError = { errorMessage ->
                _loading.postValue(false)
                _loginError.postValue(errorMessage)
            }
        )
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        _nameError.value = null
        _emailError.value = null
        _passwordError.value = null
        _confirmPasswordError.value = null
        _registerError.value = null

        val validation = AuthFormValidator.validateRegister(name, email, password, confirmPassword)
        _nameError.value = validation.nameError
        _emailError.value = validation.emailError
        _passwordError.value = validation.passwordError
        _confirmPasswordError.value = validation.confirmPasswordError
        if (!validation.isValid) return

        _loading.value = true

        viewModelScope.launch {
            val normalizedEmail = email.trim().lowercase()

            authRepository.register(
                email = normalizedEmail,
                password = password,
                onSuccess = { firebaseUser ->
                    val uid = firebaseUser?.uid.orEmpty()

                    val user = User(
                        uid = uid,
                        name = name,
                        email = normalizedEmail,
                        profileImageUrl = ""
                    )

                    viewModelScope.launch {
                        try {
                            userRepository.saveUser(user)
                            _loading.postValue(false)
                            _registerSuccess.postValue(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            _loading.postValue(false)
                            // Even if saving to Firestore fails, the user is created in Auth
                            _registerSuccess.postValue(true) 
                        }
                    }
                },
                onError = { errorMessage ->
                    _loading.postValue(false)
                    if (errorMessage.contains("already registered", ignoreCase = true) || 
                        errorMessage.contains("already in use", ignoreCase = true)) {
                        _emailError.postValue("This email address is already registered")
                    } else {
                        _registerError.postValue(errorMessage)
                    }
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
