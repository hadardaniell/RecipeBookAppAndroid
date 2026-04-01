package com.example.recipebookappandorid.validation

data class AuthValidationResult(
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
) {
    val isValid: Boolean
        get() = listOf(nameError, emailError, passwordError, confirmPasswordError).all { it == null }
}

object AuthFormValidator {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateLogin(email: String, password: String): AuthValidationResult {
        return AuthValidationResult(
            emailError = validateEmail(email),
            passwordError = validatePassword(password)
        )
    }

    fun validateRegister(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): AuthValidationResult {
        return AuthValidationResult(
            nameError = if (name.isBlank()) "Name is required" else null,
            emailError = validateEmail(email),
            passwordError = validatePassword(password),
            confirmPasswordError = when {
                confirmPassword.isBlank() -> "Please confirm password"
                password != confirmPassword -> "Passwords do not match"
                else -> null
            }
        )
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !emailRegex.matches(email) -> "Invalid email"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }
}
