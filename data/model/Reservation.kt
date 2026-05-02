package com.example.chikauto.data.model

data class Reservation(
    val id: String = "",
    val clientId: String = "",
    val agencyId: String = "",
    val carId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val totalPrice: Double = 0.0,
    val status: String = "pending" // pending, accepted, refused, cancelled, finished
)