package com.example.chikauto.client

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchLogicTest {

    private data class SearchCar(
        val brand: String,
        val model: String,
        val agencyName: String
    )

    private fun searchMatches(
        search: String,
        car: SearchCar
    ): Boolean {
        return search.isBlank()
                || car.brand.contains(search, ignoreCase = true)
                || car.model.contains(search, ignoreCase = true)
                || car.agencyName.contains(search, ignoreCase = true)
    }

    @Test
    fun searchByBrand_returnsTrue() {
        val car = SearchCar(
            brand = "Audi",
            model = "A3",
            agencyName = "ChikAuto Bejaia"
        )

        val result = searchMatches(
            search = "Audi",
            car = car
        )

        assertTrue(result)
    }

    @Test
    fun searchByModel_returnsTrue() {
        val car = SearchCar(
            brand = "BMW",
            model = "X5",
            agencyName = "Dz Cars"
        )

        val result = searchMatches(
            search = "X5",
            car = car
        )

        assertTrue(result)
    }

    @Test
    fun searchByAgency_returnsTrue() {
        val car = SearchCar(
            brand = "Mercedes",
            model = "GLC",
            agencyName = "Auto Luxe Alger"
        )

        val result = searchMatches(
            search = "Auto Luxe",
            car = car
        )

        assertTrue(result)
    }

    @Test
    fun searchWithUnknownWord_returnsFalse() {
        val car = SearchCar(
            brand = "Audi",
            model = "A3",
            agencyName = "ChikAuto Bejaia"
        )

        val result = searchMatches(
            search = "Toyota",
            car = car
        )

        assertFalse(result)
    }

    @Test
    fun emptySearch_returnsTrue() {
        val car = SearchCar(
            brand = "Peugeot",
            model = "208",
            agencyName = "Bejaia Rent"
        )

        val result = searchMatches(
            search = "",
            car = car
        )

        assertTrue(result)
    }
}