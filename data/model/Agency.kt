package com.example.chikauto.data.model

data class Agency(
    val id: String = "",
    val ownerId: String = "",
    val agencyName: String = "",
    val city: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val status: String = "pending",
    val ratingAverage: Double = 0.0,
    val totalReviews: Int = 0
)