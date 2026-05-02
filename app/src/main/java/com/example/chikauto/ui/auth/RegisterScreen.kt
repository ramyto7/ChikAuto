package com.example.chikauto.ui.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chikauto.data.model.AppOptions
import com.example.chikauto.ui.components.AppDropdown
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun RegisterScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("client") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    fun createAccount(profileImageUrl: String) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
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
                    "fullName" to fullName.trim(),
                    "email" to cleanEmail,
                    "phone" to phone.trim(),
                    "city" to city.trim(),
                    "profileImageUrl" to profileImageUrl,
                    "role" to role,
                    "status" to status
                )

                db.collection("users")
                    .document(uid)
                    .set(user)
                    .addOnSuccessListener {
                        if (role == "agency") {
                            val agency = hashMapOf(
                                "id" to uid,
                                "ownerId" to uid,
                                "agencyName" to fullName.trim(),
                                "city" to city.trim(),
                                "address" to address.trim(),
                                "phone" to phone.trim(),
                                "email" to cleanEmail,
                                "profileImageUrl" to profileImageUrl,
                                "status" to "pending",
                                "ratingAverage" to 0.0,
                                "totalReviews" to 0
                            )

                            db.collection("agencies")
                                .document(uid)
                                .set(agency)
                                .addOnSuccessListener {
                                    loading = false
                                    message = "Demande agence envoyée. Attendez la validation de l'administrateur."
                                }
                                .addOnFailureListener {
                                    loading = false
                                    message = "Erreur création agence : ${it.message}"
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
                        message = "Erreur enregistrement compte : ${it.message}"
                    }
            }
            .addOnFailureListener { exception ->
                loading = false

                message = when {
                    exception.message?.contains("email address is badly formatted", ignoreCase = true) == true ->
                        "Email invalide. Exemple : test@gmail.com"

                    exception.message?.contains("email address is already in use", ignoreCase = true) == true ->
                        "Cet email est déjà utilisé."

                    exception.message?.contains("password", ignoreCase = true) == true ->
                        "Le mot de passe doit contenir au moins 6 caractères."

                    else ->
                        "Erreur : ${exception.message}"
                }
            }
    }

    fun uploadImageThenCreateAccount() {
        val imageUri = selectedImageUri

        if (imageUri == null) {
            createAccount(profileImageUrl = "")
            return
        }

        val tempName = System.currentTimeMillis().toString()

        val ref = storage.reference
            .child("profile_images")
            .child("$tempName.jpg")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        createAccount(profileImageUrl = downloadUri.toString())
                    }
                    .addOnFailureListener {
                        loading = false
                        message = "Erreur récupération image : ${it.message}"
                    }
            }
            .addOnFailureListener {
                loading = false
                message = "Erreur upload image : ${it.message}"
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Créer un compte",
                    style = MaterialTheme.typography.headlineMedium
                )

                Box(
                    modifier = Modifier
                        .height(95.dp)
                        .width(95.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1DA1F2)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Photo de profil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "Photo",
                            color = Color.White
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        imagePicker.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choisir une photo de profil")
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        message = ""
                    },
                    label = {
                        Text(if (role == "agency") "Nom de l'agence" else "Nom complet")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        message = ""
                    },
                    label = { Text("Téléphone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                AppDropdown(
                    label = "Wilaya",
                    value = city,
                    items = AppOptions.wilayas,
                    onItemSelected = {
                        city = it
                        message = ""
                    }
                )

                if (role == "agency") {
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            message = ""
                        },
                        label = { Text("Adresse exacte de l'agence") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        message = ""
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
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
                    shape = RoundedCornerShape(16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = role == "client",
                        onClick = {
                            role = "client"
                            message = ""
                        }
                    )
                    Text("Client")

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = role == "agency",
                        onClick = {
                            role = "agency"
                            message = ""
                        }
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
                        val cleanEmail = email.trim()
                        val cleanPassword = password.trim()

                        message = ""

                        if (
                            fullName.isBlank() ||
                            phone.isBlank() ||
                            city.isBlank() ||
                            cleanEmail.isBlank() ||
                            cleanPassword.isBlank()
                        ) {
                            message = "Veuillez remplir tous les champs obligatoires."
                            return@Button
                        }

                        if (!AppOptions.wilayas.contains(city)) {
                            message = "Veuillez choisir une wilaya valide dans la liste."
                            return@Button
                        }

                        if (role == "agency" && address.isBlank()) {
                            message = "Veuillez saisir l'adresse exacte de l'agence."
                            return@Button
                        }

                        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                            message = "Email invalide. Exemple : test@gmail.com"
                            return@Button
                        }

                        if (cleanPassword.length < 6) {
                            message = "Le mot de passe doit contenir au moins 6 caractères."
                            return@Button
                        }

                        loading = true
                        uploadImageThenCreateAccount()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                ) {
                    Text(if (loading) "Création..." else "S'inscrire")
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