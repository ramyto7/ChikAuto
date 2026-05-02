package com.example.chikauto.data.model

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "", // client, agency, admin
    val status: String = "active" // active, pending, refused
)