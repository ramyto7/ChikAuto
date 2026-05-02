package com.example.chikauto.utils

object InputValidator {

    fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.length >= 9
    }
}