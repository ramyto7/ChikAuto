package com.example.chikauto.data.model

data class Reservation(
    val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val clientProfileImageUrl: String = "",

    val agencyId: String = "",
    val agencyName: String = "",

    val carId: String = "",
    val carName: String = "",
    val carImageUrl: String = "",

    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    val startDateText: String = "",
    val endDateText: String = "",
    val totalDays: Int = 0,
    val totalPrice: Double = 0.0,

    val status: String = "pending" // pending, accepted, refused, cancelled, finished
)