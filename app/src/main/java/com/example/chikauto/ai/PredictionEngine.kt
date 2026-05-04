package com.example.chikauto.ai

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PredictionResult(
    val rentals: Double,
    val demandLevel: String,
    val advice: String,
    val explanation: String,
    val confidence: Int
)

object PredictionEngine {

    fun predictCarDemand(
        previousRentals: Long,
        pricePerDay: Double,
        ratingAverage: Double,
        availableDays: Int,
        mileage: Int = 0,
        year: Int = 2020,
        acceptedReservations: Int = 0,
        refusedReservations: Int = 0,
        city: String = "",
        fuel: String = "",
        gearbox: String = "",
        carType: String = ""
    ): PredictionResult {

        var score = 0.0

        // 1. Historique des locations
        score += previousRentals * 0.55

        // 2. Réservations acceptées
        score += acceptedReservations * 0.75

        // 3. Réservations refusées : ça montre une demande, mais non finalisée
        score += refusedReservations * 0.20

        // 4. Note moyenne
        score += ratingAverage * 1.15

        // 5. Disponibilité du véhicule
        score += availableDays * 0.10

        // 6. Prix
        score += when {
            pricePerDay <= 4000 -> 2.0
            pricePerDay <= 7000 -> 1.2
            pricePerDay <= 10000 -> 0.5
            else -> -0.5
        }

        // 7. Kilométrage
        score += when {
            mileage <= 30000 -> 1.4
            mileage <= 80000 -> 0.8
            mileage <= 150000 -> 0.2
            else -> -0.8
        }

        // 8. Année
        score += when {
            year >= 2024 -> 1.6
            year >= 2020 -> 1.0
            year >= 2016 -> 0.4
            else -> -0.4
        }

        // 9. Carburant
        score += when (fuel.lowercase()) {
            "diesel" -> 0.8
            "essence" -> 0.5
            "hybride" -> 1.0
            "électrique", "electrique" -> 0.7
            else -> 0.0
        }

        // 10. Boîte
        score += when (gearbox.lowercase()) {
            "automatic" -> 1.0
            "manuelle" -> 0.4
            else -> 0.0
        }

        // 11. Type de véhicule
        score += when (carType.lowercase()) {
            "suv" -> 1.0
            "berline" -> 0.8
            "citadine" -> 0.7
            "utilitaire" -> 0.6
            else -> 0.3
        }

        // 12. Ville : petit bonus si la voiture est dans une ville active
        score += when (city.lowercase()) {
            "alger", "oran", "constantine", "béjaïa", "bejaia", "sétif", "setif" -> 0.8
            else -> 0.3
        }

        val prediction = min(max(score, 0.0), 30.0)

        val roundedPrediction = (prediction * 10.0).roundToInt() / 10.0

        val demandLevel = when {
            roundedPrediction >= 12 -> "Très élevée"
            roundedPrediction >= 8 -> "Élevée"
            roundedPrediction >= 4 -> "Moyenne"
            else -> "Faible"
        }

        val advice = when {
            roundedPrediction >= 12 ->
                "Ce véhicule est très demandé. Vous pouvez garder le prix actuel ou l’augmenter légèrement."

            roundedPrediction >= 8 ->
                "Bonne demande prévue. Gardez le véhicule disponible et évitez les longues périodes d’entretien."

            roundedPrediction >= 4 ->
                "Demande moyenne. Une petite réduction ou de meilleures photos peuvent améliorer les réservations."

            else ->
                "Demande faible. Il est conseillé de revoir le prix, les photos ou la description du véhicule."
        }

        val explanation =
            ""

        val confidence = when {
            previousRentals >= 10 -> 88
            previousRentals >= 5 -> 76
            previousRentals >= 2 -> 64
            else -> 52
        }

        return PredictionResult(
            rentals = roundedPrediction,
            demandLevel = demandLevel,
            advice = advice,
            explanation = explanation,
            confidence = confidence
        )
    }
}