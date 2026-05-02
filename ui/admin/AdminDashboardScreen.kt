package com.example.chikauto.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chikauto.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminDashboardScreen() {
    val db = FirebaseFirestore.getInstance()

    var pendingAgencies by remember { mutableStateOf(listOf<User>()) }

    var usersCount by remember { mutableIntStateOf(0) }
    var clientsCount by remember { mutableIntStateOf(0) }
    var agenciesCount by remember { mutableIntStateOf(0) }
    var carsCount by remember { mutableIntStateOf(0) }

    fun loadData() {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                usersCount = result.size()
                clientsCount = result.documents.count { it.getString("role") == "client" }
                agenciesCount = result.documents.count { it.getString("role") == "agency" }

                pendingAgencies = result.documents
                    .filter {
                        it.getString("role") == "agency" &&
                                it.getString("status") == "pending"
                    }
                    .mapNotNull { document ->
                        document.toObject(User::class.java)?.copy(id = document.id)
                    }
            }

        db.collection("cars").get()
            .addOnSuccessListener { result ->
                carsCount = result.size()
            }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Administration ChikAuto",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Validation des agences et statistiques générales",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(title = "Utilisateurs", value = usersCount.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Clients", value = clientsCount.toString(), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(title = "Agences", value = agenciesCount.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Voitures", value = carsCount.toString(), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Demandes d'agences en attente",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pendingAgencies) { agency ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = agency.fullName,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text("Email : ${agency.email}")
                        Text("Téléphone : ${agency.phone}")
                        Text("Statut : ${agency.status}")

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    db.collection("users").document(agency.id)
                                        .update("status", "active")
                                        .addOnSuccessListener {
                                            db.collection("agencies").document(agency.id)
                                                .update("status", "active")
                                                .addOnSuccessListener {
                                                    loadData()
                                                }
                                        }
                                }
                            ) {
                                Text("Valider")
                            }

                            OutlinedButton(
                                onClick = {
                                    db.collection("users").document(agency.id)
                                        .update("status", "refused")
                                        .addOnSuccessListener {
                                            db.collection("agencies").document(agency.id)
                                                .update("status", "refused")
                                                .addOnSuccessListener {
                                                    loadData()
                                                }
                                        }
                                }
                            ) {
                                Text("Refuser")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}