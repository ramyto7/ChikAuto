package com.example.chikauto.data.model

data class Review(
    val id: String = "",
    val clientId: String = "",
    val agencyId: String = "",
    val carId: String = "",
    val reservationId: String = "",
    val carRating: Int = 0,
    val agencyRating: Int = 0,
    val comment: String = "",
    val status: String = "visible" // visible, hidden
)