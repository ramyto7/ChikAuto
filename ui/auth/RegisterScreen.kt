package com.example.chikauto.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
fun RegisterScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("client") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Créer un compte",
                    style = MaterialTheme.typography.headlineMedium
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nom complet / Nom agence") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Ville") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (role == "agency") {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adresse de l'agence") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = role == "client",
                        onClick = { role = "client" }
                    )
                    Text("Client")

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = role == "agency",
                        onClick = { role = "agency" }
                    )
                    Text("Agence")
                }

                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        message = ""

                        if (fullName.isBlank() || phone.isBlank() || city.isBlank() || email.isBlank() || password.isBlank()) {
                            message = "Veuillez remplir tous les champs obligatoires."
                            return@Button
                        }

                        if (role == "agency" && address.isBlank()) {
                            message = "Veuillez saisir l'adresse de l'agence."
                            return@Button
                        }

                        loading = true

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val uid = auth.currentUser?.uid

                                if (uid == null) {
                                    loading = false
                                    message = "Erreur utilisateur."
                                    return@addOnSuccessListener
                                }

                                val status = if (role == "agency") "pending" else "active"

                                val user = hashMapOf(
                                    "id" to uid,
                                    "fullName" to fullName,
                                    "email" to email,
                                    "phone" to phone,
                                    "role" to role,
                                    "status" to status
                                )

                                db.collection("users").document(uid).set(user)
                                    .addOnSuccessListener {
                                        if (role == "agency") {
                                            val agency = hashMapOf(
                                                "id" to uid,
                                                "ownerId" to uid,
                                                "agencyName" to fullName,
                                                "city" to city,
                                                "address" to address,
                                                "phone" to phone,
                                                "email" to email,
                                                "status" to "pending",
                                                "ratingAverage" to 0.0,
                                                "totalReviews" to 0
                                            )

                                            db.collection("agencies").document(uid).set(agency)
                                                .addOnSuccessListener {
                                                    loading = false
                                                    message = "Demande agence envoyée. Attendez la validation de l'administrateur."
                                                }
                                                .addOnFailureListener {
                                                    loading = false
                                                    message = "Erreur lors de la création de l'agence."
                                                }
                                        } else {
                                            loading = false
                                            navController.navigate("client_home") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        loading = false
                                        message = "Erreur lors de l'enregistrement du compte."
                                    }
                            }
                            .addOnFailureListener { exception ->
                                loading = false
                                message = when {
                                    exception.message?.contains("email address is already in use", ignoreCase = true) == true ->
                                        "Cet email est déjà utilisé."

                                    exception.message?.contains("badly formatted", ignoreCase = true) == true ->
                                        "L'adresse email est invalide."

                                    exception.message?.contains("password", ignoreCase = true) == true ->
                                        "Le mot de passe doit contenir au moins 6 caractères."

                                    else ->
                                        "Erreur lors de l'inscription."
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                ) {
                    if (loading) {
                        Text("Création...")
                    } else {
                        Text("S'inscrire")
                    }
                }

                TextButton(
                    onClick = {
                        navController.navigate("login")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("J'ai déjà un compte")
                }
            }
        }
    }
}