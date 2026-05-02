package com.example.chikauto.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chikauto.data.model.AppOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun EditProfileDialog(
    role: String,
    currentName: String,
    currentEmail: String,
    currentPhone: String,
    currentCity: String,
    currentImageUrl: String,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    val uid = auth.currentUser?.uid ?: ""

    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var phone by remember { mutableStateOf(currentPhone) }
    var city by remember { mutableStateOf(currentCity) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var finalImageUrl by remember { mutableStateOf(currentImageUrl) }

    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            message = "Photo sélectionnée."
        } else {
            message = "Aucune photo sélectionnée."
        }
    }

    fun updateFirestoreProfile(imageUrl: String) {
        if (uid.isBlank()) {
            loading = false
            message = "Erreur : utilisateur non connecté."
            return
        }

        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanCity = city.trim()

        val userUpdates = hashMapOf<String, Any>(
            "fullName" to cleanName,
            "email" to cleanEmail,
            "phone" to cleanPhone,
            "city" to cleanCity,
            "profileImageUrl" to imageUrl
        )

        db.collection("users")
            .document(uid)
            .update(userUpdates)
            .addOnSuccessListener {
                if (role == "agency") {
                    val agencyUpdates = hashMapOf<String, Any>(
                        "agencyName" to cleanName,
                        "email" to cleanEmail,
                        "phone" to cleanPhone,
                        "city" to cleanCity,
                        "profileImageUrl" to imageUrl
                    )

                    db.collection("agencies")
                        .document(uid)
                        .update(agencyUpdates)
                        .addOnSuccessListener {
                            loading = false
                            onUpdated()
                        }
                        .addOnFailureListener { exception ->
                            loading = false
                            message = "Erreur modification agence : ${exception.message}"
                        }
                } else {
                    loading = false
                    onUpdated()
                }
            }
            .addOnFailureListener { exception ->
                loading = false
                message = "Erreur modification profil : ${exception.message}"
            }
    }

    fun uploadImageThenUpdateProfile() {
        val imageUri = selectedImageUri

        if (uid.isBlank()) {
            loading = false
            message = "Erreur : utilisateur non connecté."
            return
        }

        if (imageUri == null) {
            updateFirestoreProfile(finalImageUrl)
            return
        }

        val fileName = "profile_${System.currentTimeMillis()}.jpg"

        val ref = storage.reference
            .child("profile_images")
            .child(uid)
            .child(fileName)

        ref.putFile(imageUri)
            .addOnProgressListener {
                message = "Téléversement de la photo..."
            }
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        finalImageUrl = downloadUri.toString()
                        updateFirestoreProfile(downloadUri.toString())
                    }
                    .addOnFailureListener { exception ->
                        loading = false
                        message = "Photo envoyée, mais lien introuvable : ${exception.message}"
                    }
            }
            .addOnFailureListener { exception ->
                loading = false
                message = "Erreur upload photo : ${exception.message}"
            }
    }

    AlertDialog(
        onDismissRequest = {
            if (!loading) {
                onDismiss()
            }
        },
        title = {
            Text("Modifier le profil")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(95.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(95.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1DA1F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedImageUri != null) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Nouvelle photo de profil",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (currentImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = currentImageUrl,
                                        contentDescription = "Photo de profil actuelle",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = name.take(1).ifBlank { "P" }.uppercase(),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            if (!loading) {
                                imagePicker.launch("image/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choisir une nouvelle photo")
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            message = ""
                        },
                        label = {
                            Text(if (role == "agency") "Nom de l'agence" else "Nom complet")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            message = ""
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            message = ""
                        },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    AppDropdown(
                        label = "Wilaya",
                        value = city,
                        items = AppOptions.wilayas,
                        onItemSelected = {
                            city = it
                            message = ""
                        }
                    )
                }

                if (message.isNotEmpty()) {
                    item {
                        Text(
                            text = message,
                            color = if (
                                message.contains("sélectionnée", ignoreCase = true) ||
                                message.contains("Téléversement", ignoreCase = true)
                            ) {
                                Color(0xFF1DA1F2)
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (loading) return@Button

                    if (name.isBlank() || email.isBlank() || phone.isBlank() || city.isBlank()) {
                        message = "Veuillez remplir tous les champs."
                        return@Button
                    }

                    if (!email.contains("@") || !email.contains(".")) {
                        message = "Email invalide."
                        return@Button
                    }

                    if (!AppOptions.wilayas.contains(city)) {
                        message = "Veuillez choisir une wilaya valide."
                        return@Button
                    }

                    loading = true
                    message = ""
                    uploadImageThenUpdateProfile()
                },
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1DA1F2)
                )
            ) {
                Text(if (loading) "Enregistrement..." else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!loading) {
                        onDismiss()
                    }
                }
            ) {
                Text("Annuler")
            }
        }
    )
}