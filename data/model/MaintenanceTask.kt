package com.example.chikauto.data.model

data class MaintenanceTask(
    val id: String = "",
    val carId: String = "",
    val agencyId: String = "",
    val agentId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = "",
    val status: String = "planned" // planned, in_progress, finished
)