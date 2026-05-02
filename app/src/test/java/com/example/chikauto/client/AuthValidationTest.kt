package com.example.chikauto.client

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginValidationTest {

    private fun isValidEmail(email: String): Boolean {
        val cleanEmail = email.trim()
        return cleanEmail.contains("@") && cleanEmail.contains(".")
    }

    private fun isValidPassword(password: String): Boolean {
        return password.trim().length >= 6
    }

    private fun canLogin(
        email: String,
        password: String
    ): Boolean {
        if (email.isBlank()) return false
        if (password.isBlank()) return false
        if (!isValidEmail(email)) return false
        if (!isValidPassword(password)) return false

        return true
    }

    @Test
    fun validLogin_returnsTrue() {
        val result = canLogin(
            email = "client@gmail.com",
            password = "123456"
        )

        assertTrue(result)
    }

    @Test
    fun loginWithoutEmail_returnsFalse() {
        val result = canLogin(
            email = "",
            password = "123456"
        )

        assertFalse(result)
    }

    @Test
    fun loginWithoutPassword_returnsFalse() {
        val result = canLogin(
            email = "client@gmail.com",
            password = ""
        )

        assertFalse(result)
    }

    @Test
    fun loginWithInvalidEmail_returnsFalse() {
        val result = canLogin(
            email = "clientgmail.com",
            password = "123456"
        )

        assertFalse(result)
    }

    @Test
    fun loginWithShortPassword_returnsFalse() {
        val result = canLogin(
            email = "client@gmail.com",
            password = "123"
        )

        assertFalse(result)
    }
}