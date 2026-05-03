package com.example.chikauto.data.model

data class Car(
    val id: String = "",
    val agencyId: String = "",
    val agencyName: String = "",
    val brandId: String = "",
    val brandName: String = "",
    val modelId: String = "",
    val modelName: String = "",
    val year: Int = 2020,
    val city: String = "",
    val type: String = "",
    val fuel: String = "",
    val gearbox: String = "",
    val pricePerDay: Double = 0.0,
    val mileage: Int = 0,
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val available: Boolean = true,
    val status: String = "available", // available, maintenance, inactive
    val ratingAverage: Double = 0.0,
    val totalReviews: Int = 0,
    val previousRentals: Long = 0L
)