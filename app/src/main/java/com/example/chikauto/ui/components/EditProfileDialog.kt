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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun EditProfileDialog(
    role: String, // "client" ou "agency"
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

    val user = auth.currentUser
    val uid = user?.uid ?: ""

    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var phone by remember { mutableStateOf(currentPhone) }
    var city by remember { mutableStateOf(currentCity) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf(currentImageUrl) }

    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    fun updateFirestore(finalImageUrl: String) {
        val userUpdates = hashMapOf<String, Any>(
            "fullName" to name.trim(),
            "email" to email.trim(),
            "phone" to phone.trim(),
            "city" to city.trim(),
            "profileImageUrl" to finalImageUrl
        )

        db.collection("users")
            .document(uid)
            .update(userUpdates)
            .addOnSuccessListener {
                if (role == "agency") {
                    val agencyUpdates = hashMapOf<String, Any>(
                        "agencyName" to name.trim(),
                        "email" to email.trim(),
                        "phone" to phone.trim(),
                        "city" to city.trim(),
                        "profileImageUrl" to finalImageUrl
                    )

                    db.collection("agencies")
                        .document(uid)
                        .update(agencyUpdates)
                        .addOnSuccessListener {
                            loading = false
                            onUpdated()
                            onDismiss()
                        }
                        .addOnFailureListener {
                            loading = false
                            message = "Erreur modification agence : ${it.message}"
                        }
                } else {
                    loading = false
                    onUpdated()
                    onDismiss()
                }
            }
            .addOnFailureListener {
                loading = false
                message = "Erreur modification profil : ${it.message}"
            }
    }

    fun uploadImageAndUpdate() {
        val uri = selectedImageUri

        if (uri == null) {
            updateFirestore(imageUrl)
            return
        }

        val ref = storage.reference
            .child("profile_images")
            .child("$uid.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        imageUrl = downloadUri.toString()
                        updateFirestore(downloadUri.toString())
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

    fun saveProfile() {
        if (uid.isBlank() || user == null) {
            message = "Utilisateur introuvable."
            return
        }

        if (name.isBlank() || email.isBlank() || phone.isBlank() || city.isBlank()) {
            message = "Veuillez remplir tous les champs."
            return
        }

        if (currentPassword.isBlank()) {
            message = "Veuillez entrer votre mot de passe actuel."
            return
        }

        if (!email.contains("@") || !email.contains(".")) {
            message = "Email invalide."
            return
        }

        if (newPassword.isNotBlank() && newPassword.length < 6) {
            message = "Le nouveau mot de passe doit contenir au moins 6 caractères."
            return
        }

        loading = true
        message = ""

        val credential = EmailAuthProvider.getCredential(
            user.email ?: currentEmail,
            currentPassword
        )

        user.reauthenticate(credential)
            .addOnSuccessListener {
                val emailChanged = email.trim() != (user.email ?: "")
                val passwordChanged = newPassword.isNotBlank()

                fun afterAuthUpdates() {
                    uploadImageAndUpdate()
                }

                if (emailChanged) {
                    user.updateEmail(email.trim())
                        .addOnSuccessListener {
                            if (passwordChanged) {
                                user.updatePassword(newPassword)
                                    .addOnSuccessListener { afterAuthUpdates() }
                                    .addOnFailureListener {
                                        loading = false
                                        message = "Erreur mot de passe : ${it.message}"
                                    }
                            } else {
                                afterAuthUpdates()
                            }
                        }
                        .addOnFailureListener {
                            loading = false
                            message = "Erreur email : ${it.message}"
                        }
                } else {
                    if (passwordChanged) {
                        user.updatePassword(newPassword)
                            .addOnSuccessListener { afterAuthUpdates() }
                            .addOnFailureListener {
                                loading = false
                                message = "Erreur mot de passe : ${it.message}"
                            }
                    } else {
                        afterAuthUpdates()
                    }
                }
            }
            .addOnFailureListener {
                loading = false
                message = "Mot de passe actuel incorrect."
            }
    }

    AlertDialog(
        onDismissRequest = {
            if (!loading) onDismiss()
        },
        title = {
            Text("Modifier le profil")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(90.dp)
                        .width(90.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1DA1F2)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedImageUri != null -> {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Photo sélectionnée",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        imageUrl.isNotBlank() -> {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Photo profil",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> {
                            Text(
                                text = name.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
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
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(if (role == "agency") "Nom de l'agence" else "Nom complet")
                    },
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
                    label = { Text("Wilaya") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Nouvel email") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nouveau mot de passe facultatif") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Mot de passe actuel obligatoire") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { saveProfile() },
                enabled = !loading
            ) {
                Text(if (loading) "Modification..." else "Enregistrer")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !loading
            ) {
                Text("Annuler")
            }
        }
    )
}