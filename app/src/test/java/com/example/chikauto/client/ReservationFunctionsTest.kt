package com.example.chikauto.client

import com.example.chikauto.ui.client.calculateDays
import com.example.chikauto.ui.client.dateRangesOverlap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationFunctionsTest {

    @Test
    fun calculateDays_sameDay_returnsOneDay() {
        val start = 1_700_000_000_000L
        val end = start

        val result = calculateDays(start, end)

        assertEquals(1, result)
    }

    @Test
    fun calculateDays_twoDays_returnsTwoDays() {
        val start = 1_700_000_000_000L
        val end = start + 86_400_000L

        val result = calculateDays(start, end)

        assertEquals(2, result)
    }

    @Test
    fun calculateDays_endBeforeStart_returnsZero() {
        val start = 1_700_000_000_000L
        val end = start - 86_400_000L

        val result = calculateDays(start, end)

        assertEquals(0, result)
    }

    @Test
    fun dateRangesOverlap_overlappingDates_returnsTrue() {
        val result = dateRangesOverlap(
            start1 = 1000L,
            end1 = 5000L,
            start2 = 3000L,
            end2 = 7000L
        )

        assertTrue(result)
    }

    @Test
    fun dateRangesOverlap_separateDates_returnsFalse() {
        val result = dateRangesOverlap(
            start1 = 1000L,
            end1 = 3000L,
            start2 = 4000L,
            end2 = 7000L
        )

        assertFalse(result)
    }
}