package com.example.chikauto.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionEngineTest {

    @Test
    fun prediction_should_return_valid_number_of_rentals() {
        val result = PredictionEngine.predictCarDemand(
            previousRentals = 6,
            pricePerDay = 8000.0,
            ratingAverage = 4.5,
            availableDays = 25
        )

        assertTrue(result.rentals >= 0)
        assertTrue(result.demandLevel in listOf("Faible", "Moyenne", "Forte"))
        assertTrue(result.advice.isNotBlank())
    }

    @Test
    fun expensive_car_should_have_prediction_result() {
        val result = PredictionEngine.predictCarDemand(
            previousRentals = 2,
            pricePerDay = 20000.0,
            ratingAverage = 3.0,
            availableDays = 10
        )

        assertTrue(result.rentals >= 0)
        assertTrue(result.demandLevel in listOf("Faible", "Moyenne", "Forte"))
    }
}