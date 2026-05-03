package com.example.chikauto.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chikauto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val adminUsername = "admin"
    val adminPassword = "admin123"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(30.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painterResource(id = R.drawable.chickauto_logo),
                    contentDescription = "Logo ChickAuto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Bienvenue sur ChickAuto",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "Louez la voiture qu’il vous faut",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = emailOrUsername,
                    onValueChange = {
                        emailOrUsername = it
                        message = ""
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        message = ""
                    },
                    label = { Text("Mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )

                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        message = ""

                        if (emailOrUsername.isBlank() || password.isBlank()) {
                            message = "Veuillez remplir tous les champs."
                            return@Button
                        }

                        if (emailOrUsername.trim() == adminUsername && password == adminPassword) {
                            navController.navigate("admin_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                            return@Button
                        }

                        if (!emailOrUsername.contains("@")) {
                            message = "Veuillez entrer un email valide."
                            return@Button
                        }

                        loading = true

                        auth.signInWithEmailAndPassword(emailOrUsername.trim(), password)
                            .addOnSuccessListener {
                                val uid = auth.currentUser?.uid ?: ""

                                if (uid.isBlank()) {
                                    loading = false
                                    message = "Utilisateur introuvable."
                                    auth.signOut()
                                    return@addOnSuccessListener
                                }

                                db.collection("users")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        loading = false

                                        if (!document.exists()) {
                                            message = "Compte introuvable dans la base de données."
                                            auth.signOut()
                                            return@addOnSuccessListener
                                        }

                                        val role = document.getString("role") ?: ""
                                        val status = document.getString("status") ?: "active"

                                        when {
                                            status == "disabled" || status == "blocked" || status == "inactive" -> {
                                                message = "Votre compte a été désactivé par l’administrateur."
                                                auth.signOut()
                                            }

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
                                                message = "Votre compte agence est en attente de validation."
                                                auth.signOut()
                                            }

                                            role == "agency" && status == "refused" -> {
                                                message = "Votre demande agence a été refusée."
                                                auth.signOut()
                                            }

                                            role == "admin" && status == "active" -> {
                                                navController.navigate("admin_dashboard") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }

                                            else -> {
                                                message = "Compte invalide ou non autorisé."
                                                auth.signOut()
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        loading = false
                                        message = "Erreur vérification compte : ${it.message}"
                                        auth.signOut()
                                    }
                            }
                            .addOnFailureListener {
                                loading = false
                                message = "Email ou mot de passe incorrect."
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !loading,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (loading) "Connexion..." else "Se connecter")
                }

                TextButton(
                    onClick = { navController.navigate("register") }
                ) {
                    Text("Vous n’avez pas de compte ? S’inscrire")
                }
            }
        }
    }
}