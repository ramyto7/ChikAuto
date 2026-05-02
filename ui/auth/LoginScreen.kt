package com.example.chikauto.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val adminUsername = "admin"
    val adminPassword = "admin123"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ChikAuto",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "Location de voitures en Algérie",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = emailOrUsername,
                    onValueChange = {
                        emailOrUsername = it
                        error = ""
                    },
                    label = { Text("Email ou nom admin") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = ""
                    },
                    label = { Text("Mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        error = ""

                        if (emailOrUsername.isBlank() || password.isBlank()) {
                            error = "Veuillez remplir tous les champs."
                            return@Button
                        }

                        loading = true

                        if (emailOrUsername == adminUsername && password == adminPassword) {
                            loading = false
                            navController.navigate("admin_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                            return@Button
                        }

                        auth.signInWithEmailAndPassword(emailOrUsername, password)
                            .addOnSuccessListener {
                                val uid = auth.currentUser?.uid

                                if (uid == null) {
                                    loading = false
                                    error = "Erreur utilisateur."
                                    return@addOnSuccessListener
                                }

                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { document ->
                                        loading = false

                                        if (!document.exists()) {
                                            error = "Compte introuvable dans la base de données."
                                            return@addOnSuccessListener
                                        }

                                        val role = document.getString("role")
                                        val status = document.getString("status")

                                        when {
                                            role == "client" && status == "active" -> {
                                                navController.navigate("client_home") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }

                                            role == "agency" && status == "active" -> {
                                                navController.navigate("agency_dashboard") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }

                                            role == "agency" && status == "pending" -> {
                                                error = "Votre compte agence est en attente de validation."
                                            }

                                            role == "agency" && status == "refused" -> {
                                                error = "Votre demande d'agence a été refusée."
                                            }

                                            else -> {
                                                error = "Compte invalide."
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        loading = false
                                        error = "Erreur lors de la récupération du compte."
                                    }
                            }
                            .addOnFailureListener {
                                loading = false
                                error = "Email ou mot de passe incorrect."
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                ) {
                    if (loading) {
                        Text("Connexion...")
                    } else {
                        Text("Se connecter")
                    }
                }

                TextButton(
                    onClick = {
                        navController.navigate("register")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Créer un compte")
                }
            }
        }
    }
}