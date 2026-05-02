package com.example.chikauto.client

import com.example.chikauto.ui.client.starsText
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewFunctionsTest {

    @Test
    fun starsText_zero_returnsFiveEmptyStars() {
        val result = starsText(0)

        assertEquals("☆☆☆☆☆", result)
    }

    @Test
    fun starsText_three_returnsThreeFullStars() {
        val result = starsText(3)

        assertEquals("★★★☆☆", result)
    }

    @Test
    fun starsText_five_returnsFiveFullStars() {
        val result = starsText(5)

        assertEquals("★★★★★", result)
    }

    @Test
    fun starsText_moreThanFive_returnsFiveFullStars() {
        val result = starsText(8)

        assertEquals("★★★★★", result)
    }

    @Test
    fun starsText_negative_returnsFiveEmptyStars() {
        val result = starsText(-2)

        assertEquals("☆☆☆☆☆", result)
    }
}