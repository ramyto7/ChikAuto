package com.example.chikauto.ai

data class PredictionResult(
    val rentals: Int,
    val demandLevel: String,
    val advice: String
)

object PredictionEngine {

    fun predictCarDemand(
        previousRentals: Int,
        pricePerDay: Double,
        ratingAverage: Double,
        availableDays: Int
    ): PredictionResult {
        var score = previousRentals

        if (pricePerDay <= 6000) {
            score += 3
        } else if (pricePerDay <= 10000) {
            score += 2
        } else {
            score += 1
        }

        if (ratingAverage >= 4.5) {
            score += 3
        } else if (ratingAverage >= 3.5) {
            score += 2
        }

        if (availableDays >= 20) {
            score += 2
        }

        val rentals = score.coerceIn(0, 20)

        val demandLevel = when {
            rentals >= 10 -> "Forte"
            rentals >= 5 -> "Moyenne"
            else -> "Faible"
        }

        val advice = when (demandLevel) {
            "Forte" -> "Prévoir la disponibilité du véhicule et éviter les entretiens pendant les week-ends."
            "Moyenne" -> "Maintenir le prix actuel et améliorer la visibilité de l'offre."
            else -> "Réduire légèrement le prix ou améliorer la description du véhicule."
        }

        return PredictionResult(
            rentals = rentals,
            demandLevel = demandLevel,
            advice = advice
        )
    }
}