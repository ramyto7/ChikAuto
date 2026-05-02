package com.example.chikauto.ui.agency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chikauto.ai.PredictionEngine
import com.example.chikauto.data.model.Car
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AgencyDashboardScreen() {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val agencyId = auth.currentUser?.uid ?: ""

    var cars by remember { mutableStateOf(listOf<Car>()) }

    LaunchedEffect(Unit) {
        db.collection("cars")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { result ->
                cars = result.documents.mapNotNull { document ->
                    document.toObject(Car::class.java)?.copy(id = document.id)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Tableau de bord agence",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Gestion de flotte et prédiction IA",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Nombre de voitures : ${cars.size}")

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cars) { car ->
                val prediction = PredictionEngine.predictCarDemand(
                    previousRentals = car.previousRentals,
                    pricePerDay = car.pricePerDay,
                    ratingAverage = car.ratingAverage,
                    availableDays = 25
                )

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${car.brandName} ${car.modelName}",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text("Ville : ${car.city}")
                        Text("Prix : ${car.pricePerDay} DA / jour")
                        Text("Statut : ${car.status}")

                        Text("Prédiction IA : ${prediction.rentals} locations le mois prochain")
                        Text("Demande prévue : ${prediction.demandLevel}")
                        Text("Conseil : ${prediction.advice}")
                    }
                }
            }
        }
    }
}