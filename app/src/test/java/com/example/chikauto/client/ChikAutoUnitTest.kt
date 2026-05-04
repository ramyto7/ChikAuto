package com.example.chikauto

import org.junit.Assert.*
import org.junit.Test

class ChikAutoUnitTest {

    private fun isValidLogin(email: String, password: String): Boolean {
        return email.contains("@") && password.length >= 6
    }

    private fun isValidRegister(
        fullName: String,
        email: String,
        password: String,
        phone: String
    ): Boolean {
        return fullName.isNotBlank()
                && email.contains("@")
                && password.length >= 6
                && phone.length >= 8
    }

    private fun filterCars(
        cars: List<TestCar>,
        search: String,
        city: String,
        maxPrice: Double?
    ): List<TestCar> {
        return cars.filter { car ->
            val searchOk = search.isBlank()
                    || car.brand.contains(search, ignoreCase = true)
                    || car.model.contains(search, ignoreCase = true)

            val cityOk = city.isBlank()
                    || city == "Algérie"
                    || car.city.equals(city, ignoreCase = true)

            val priceOk = maxPrice == null || car.pricePerDay <= maxPrice

            searchOk && cityOk && priceOk
        }
    }

    private fun isValidReservationDates(start: Long, end: Long): Boolean {
        return start > 0 && end > 0 && end >= start
    }

    private fun dateRangesOverlap(
        newStart: Long,
        newEnd: Long,
        existingStart: Long,
        existingEnd: Long
    ): Boolean {
        return newStart <= existingEnd && existingStart <= newEnd
    }

    private fun isValidMessage(message: String): Boolean {
        return message.trim().isNotEmpty()
    }

    @Test
    fun login_should_be_valid_when_email_and_password_are_correct() {
        assertTrue(isValidLogin("client@gmail.com", "123456"))
    }

    @Test
    fun login_should_be_invalid_when_password_is_too_short() {
        assertFalse(isValidLogin("client@gmail.com", "123"))
    }

    @Test
    fun register_should_be_valid_when_all_fields_are_correct() {
        assertTrue(
            isValidRegister(
                fullName = "Ramy",
                email = "ramy@gmail.com",
                password = "123456",
                phone = "0550123456"
            )
        )
    }

    @Test
    fun filter_should_return_only_cars_matching_search_city_and_price() {
        val cars = listOf(
            TestCar("Audi", "A3", "Bejaia", 9000.0),
            TestCar("BMW", "X5", "Alger", 20000.0),
            TestCar("Fiat", "Doblo", "Bejaia", 7000.0)
        )

        val result = filterCars(
            cars = cars,
            search = "fiat",
            city = "Bejaia",
            maxPrice = 10000.0
        )

        assertEquals(1, result.size)
        assertEquals("Fiat", result[0].brand)
    }

    @Test
    fun reservation_dates_should_be_valid_when_end_date_is_after_start_date() {
        val startDate = 1000L
        val endDate = 2000L

        assertTrue(isValidReservationDates(startDate, endDate))
    }

    @Test
    fun reservation_dates_should_be_invalid_when_end_date_is_before_start_date() {
        val startDate = 3000L
        val endDate = 1000L

        assertFalse(isValidReservationDates(startDate, endDate))
    }

    @Test
    fun reservation_conflict_should_be_detected_when_dates_overlap() {
        val existingStart = 9L
        val existingEnd = 11L

        val newStart = 10L
        val newEnd = 13L

        assertTrue(
            dateRangesOverlap(
                newStart = newStart,
                newEnd = newEnd,
                existingStart = existingStart,
                existingEnd = existingEnd
            )
        )
    }

    @Test
    fun reservation_conflict_should_not_exist_when_dates_do_not_overlap() {
        val existingStart = 9L
        val existingEnd = 11L

        val newStart = 12L
        val newEnd = 14L

        assertFalse(
            dateRangesOverlap(
                newStart = newStart,
                newEnd = newEnd,
                existingStart = existingStart,
                existingEnd = existingEnd
            )
        )
    }

    @Test
    fun message_should_be_valid_when_text_is_not_empty() {
        assertTrue(isValidMessage("Bonjour, est-ce que la voiture est disponible ?"))
    }

    @Test
    fun message_should_be_invalid_when_text_is_empty() {
        assertFalse(isValidMessage("   "))
    }
}

data class TestCar(
    val brand: String,
    val model: String,
    val city: String,
    val pricePerDay: Double
)