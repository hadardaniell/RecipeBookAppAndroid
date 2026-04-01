package com.example.recipebookappandorid.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFormValidatorTest {

    @Test
    fun `login validation rejects invalid email and short password`() {
        val result = AuthFormValidator.validateLogin(
            email = "bad-email",
            password = "123"
        )

        assertEquals("Invalid email", result.emailError)
        assertEquals("Password must be at least 6 characters", result.passwordError)
        assertTrue(!result.isValid)
    }

    @Test
    fun `register validation rejects empty name and mismatched password confirmation`() {
        val result = AuthFormValidator.validateRegister(
            name = "",
            email = "chef@example.com",
            password = "secret1",
            confirmPassword = "secret2"
        )

        assertEquals("Name is required", result.nameError)
        assertEquals("Passwords do not match", result.confirmPasswordError)
        assertTrue(!result.isValid)
    }

    @Test
    fun `register validation accepts valid form`() {
        val result = AuthFormValidator.validateRegister(
            name = "Hadar",
            email = "chef@example.com",
            password = "secret1",
            confirmPassword = "secret1"
        )

        assertTrue(result.isValid)
    }
}
