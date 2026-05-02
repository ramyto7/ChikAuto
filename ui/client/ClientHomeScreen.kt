package com.example.chikauto.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chikauto.data.model.Car
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ClientHomeScreen() {
    val db = FirebaseFirestore.getInstance()

    var cars by remember { mutableStateOf(listOf<Car>()) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("cars")
            .whereEqualTo("available", true)
            .get()
            .addOnSuccessListener { result ->
                cars = result.documents.mapNotNull { document ->
                    document.toObject(Car::class.java)?.copy(id = document.id)
                }
            }
    }

    val filteredCars = cars.filter { car ->
        search.isBlank() ||
                car.brandName.contains(search, ignoreCase = true) ||
                car.modelName.contains(search, ignoreCase = true) ||
                car.city.contains(search, ignoreCase = true) ||
                car.type.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ChikAuto",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Trouvez une voiture de location",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Recherche : ville, marque, modèle...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredCars) { car ->
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
                        Text("Type : ${car.type}")
                        Text("Carburant : ${car.fuel}")
                        Text("Boîte : ${car.gearbox}")
                        Text("Prix : ${car.pricePerDay} DA / jour")
                        Text("Note : ${car.ratingAverage}/5")

                        Button(
                            onClick = {
                                // réservation après
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Demander réservation")
                        }
                    }
                }
            }
        }
    }
}