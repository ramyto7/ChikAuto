package com.example.chikauto.client

import com.example.chikauto.ui.client.calculateDays
import com.example.chikauto.ui.client.starsText
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientFunctionsTest {

    @Test
    fun calculateDays_sameDay_returnsOneDay() {
        val start = 1_700_000_000_000L
        val end = start

        val result = calculateDays(start, end)

        assertEquals(1, result)
    }

    @Test
    fun calculateDays_endBeforeStart_returnsZero() {
        val start = 1_700_000_000_000L
        val end = start - 86_400_000L

        val result = calculateDays(start, end)

        assertEquals(0, result)
    }

    @Test
    fun starsText_threeStars_returnsThreeFullStars() {
        val result = starsText(3)

        assertEquals("★★★☆☆", result)
    }

    @Test
    fun starsText_moreThanFive_returnsFiveStars() {
        val result = starsText(8)

        assertEquals("★★★★★", result)
    }
}