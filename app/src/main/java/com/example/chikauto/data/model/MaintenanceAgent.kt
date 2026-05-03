package com.example.chikauto.data.model

data class MaintenanceAgent(
    val id: String = "",
    val agencyId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val location: String = "",
    val available: Boolean = true,
    val profileImageUrl: String = ""
)