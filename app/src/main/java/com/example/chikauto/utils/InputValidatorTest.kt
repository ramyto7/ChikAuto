package com.example.chikauto.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    @Test
    fun valid_email_should_return_true() {
        assertTrue(InputValidator.isValidEmail("test@gmail.com"))
    }

    @Test
    fun invalid_email_should_return_false() {
        assertFalse(InputValidator.isValidEmail("test"))
    }

    @Test
    fun valid_password_should_return_true() {
        assertTrue(InputValidator.isValidPassword("123456"))
    }

    @Test
    fun short_password_should_return_false() {
        assertFalse(InputValidator.isValidPassword("123"))
    }

    @Test
    fun valid_phone_should_return_true() {
        assertTrue(InputValidator.isValidPhone("0555555555"))
    }
}