package com.example.recipebookappandorid.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("AuthRepository", "Login failed", exception)
                onError(mapAuthError(exception, fallbackMessage = "Login failed"))
            }
    }

    fun register(
        email: String,
        password: String,
        onSuccess: (FirebaseUser?) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                onSuccess(result.user)
            }
            .addOnFailureListener { exception ->
                Log.e("AuthRepository", "Register failed", exception)
                onError(mapAuthError(exception, fallbackMessage = "Register failed"))
            }
    }

    fun getCurrentUser() = auth.currentUser

    fun logout() {
        auth.signOut()
    }

    private fun mapAuthError(exception: Exception, fallbackMessage: String): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> {
                "No account found for this email (${exception.errorCode})"
            }

            is FirebaseAuthInvalidCredentialsException -> {
                when (exception.errorCode) {
                    "ERROR_WRONG_PASSWORD" -> "Incorrect password"
                    "ERROR_INVALID_EMAIL" -> "Invalid email format"
                    else -> "Invalid credentials (${exception.errorCode})"
                }
            }

            is FirebaseAuthException -> {
                "${exception.localizedMessage ?: fallbackMessage} (${exception.errorCode})"
            }

            else -> exception.localizedMessage ?: fallbackMessage
        }
    }
}
