package com.example.recipebookappandorid.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.User
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.UserRepository
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

        var isValid = true

        if (email.isBlank()) {
            _emailError.value = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Invalid email"
            isValid = false
        }

        if (password.isBlank()) {
            _passwordError.value = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            _passwordError.value = "Password must be at least 6 characters"
            isValid = false
        }

        if (!isValid) return

        _loading.value = true

        authRepository.login(
            email = email,
            password = password,
            onSuccess = {
                val firebaseUser = authRepository.getCurrentUser()
                val uid = firebaseUser?.uid.orEmpty()

                viewModelScope.launch {
                    val existingUser = if (uid.isNotEmpty()) userRepository.getUser(uid) else null

                    val user = User(
                        uid = uid,
                        name = existingUser?.name ?: "",
                        email = firebaseUser?.email ?: email,
                        profileImageUrl = existingUser?.profileImageUrl ?: ""
                    )

                    userRepository.saveUser(user)
                    _loading.postValue(false)
                    _loginSuccess.postValue(true)
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

        var isValid = true

        if (name.isBlank()) {
            _nameError.value = "Name is required"
            isValid = false
        }

        if (email.isBlank()) {
            _emailError.value = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Invalid email"
            isValid = false
        }

        if (password.isBlank()) {
            _passwordError.value = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            _passwordError.value = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isBlank()) {
            _confirmPasswordError.value = "Please confirm password"
            isValid = false
        } else if (password != confirmPassword) {
            _confirmPasswordError.value = "Passwords do not match"
            isValid = false
        }

        if (!isValid) return

        _loading.value = true

        authRepository.register(
            email = email,
            password = password,
            onSuccess = { firebaseUser ->
                val uid = firebaseUser?.uid.orEmpty()

                val user = User(
                    uid = uid,
                    name = name,
                    email = email,
                    profileImageUrl = ""
                )

                viewModelScope.launch {
                    userRepository.saveUser(user)
                    _loading.postValue(false)
                    _registerSuccess.postValue(true)
                }
            },
            onError = { errorMessage ->
                _loading.postValue(false)
                _registerError.postValue(errorMessage)
            }
        )
    }

    fun logout() {
        authRepository.logout()
    }
}