package com.example.chikauto.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chikauto.data.model.AppOptions
import com.example.chikauto.data.model.Brand
import com.example.chikauto.data.model.CarModel
import com.example.chikauto.ui.components.AppDropdown
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

data class AdminUserUi(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "",
    val role: String = "",
    val status: String = "active",
    val profileImageUrl: String = ""
)

data class AdminAgencyUi(
    val id: String = "",
    val ownerId: String = "",
    val agencyName: String = "",
    val city: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val status: String = "active",
    val profileImageUrl: String = "",
    val ratingAverage: Double = 0.0,
    val totalReviews: Long = 0L
)

data class AdminCarUi(
    val id: String = "",
    val agencyId: String = "",
    val agencyName: String = "",
    val brandName: String = "",
    val modelName: String = "",
    val city: String = "",
    val type: String = "",
    val fuel: String = "",
    val gearbox: String = "",
    val pricePerDay: Double = 0.0,
    val mileage: Int = 0,
    val status: String = "",
    val available: Boolean = false,
    val imageUrl: String = "",
    val ratingAverage: Double = 0.0,
    val totalReviews: Long = 0L
)

@Composable
fun AdminDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var selectedTab by remember { mutableStateOf("home") }
    var detailsPage by remember { mutableStateOf("") }

    var usersCount by remember { mutableIntStateOf(0) }
    var clientsCount by remember { mutableIntStateOf(0) }
    var agenciesCount by remember { mutableIntStateOf(0) }
    var carsCount by remember { mutableIntStateOf(0) }

    var users by remember { mutableStateOf(listOf<AdminUserUi>()) }
    var clients by remember { mutableStateOf(listOf<AdminUserUi>()) }
    var agencies by remember { mutableStateOf(listOf<AdminAgencyUi>()) }
    var cars by remember { mutableStateOf(listOf<AdminCarUi>()) }
    var pendingAgencies by remember { mutableStateOf(listOf<AdminAgencyUi>()) }

    var brands by remember { mutableStateOf(listOf<Brand>()) }
    var models by remember { mutableStateOf(listOf<CarModel>()) }

    var message by remember { mutableStateOf("") }

    fun loadData() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val loadedUsers = result.documents.map { doc ->
                    AdminUserUi(
                        id = doc.id,
                        fullName = doc.getString("fullName") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        city = doc.getString("city") ?: "",
                        role = doc.getString("role") ?: "",
                        status = doc.getString("status") ?: "active",
                        profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    )
                }

                users = loadedUsers
                clients = loadedUsers.filter { it.role == "client" }

                usersCount = loadedUsers.size
                clientsCount = loadedUsers.count { it.role == "client" }
            }
            .addOnFailureListener {
                message = "Erreur chargement utilisateurs : ${it.message}"
            }

        db.collection("agencies")
            .get()
            .addOnSuccessListener { result ->
                val loadedAgencies = result.documents.map { doc ->
                    AdminAgencyUi(
                        id = doc.id,
                        ownerId = doc.getString("ownerId") ?: "",
                        agencyName = doc.getString("agencyName") ?: "",
                        city = doc.getString("city") ?: "",
                        address = doc.getString("address") ?: "",
                        phone = doc.getString("phone") ?: "",
                        email = doc.getString("email") ?: "",
                        status = doc.getString("status") ?: "active",
                        profileImageUrl = doc.getString("profileImageUrl") ?: "",
                        ratingAverage = doc.getDouble("ratingAverage") ?: 0.0,
                        totalReviews = doc.getLong("totalReviews") ?: 0L
                    )
                }

                agencies = loadedAgencies
                agenciesCount = loadedAgencies.size
                pendingAgencies = loadedAgencies.filter { it.status == "pending" }
            }
            .addOnFailureListener {
                message = "Erreur chargement agences : ${it.message}"
            }

        db.collection("cars")
            .get()
            .addOnSuccessListener { result ->
                val loadedCars = result.documents.map { doc ->
                    AdminCarUi(
                        id = doc.id,
                        agencyId = doc.getString("agencyId") ?: "",
                        agencyName = doc.getString("agencyName") ?: "",
                        brandName = doc.getString("brandName") ?: "",
                        modelName = doc.getString("modelName") ?: "",
                        city = doc.getString("city") ?: "",
                        type = doc.getString("type") ?: "",
                        fuel = doc.getString("fuel") ?: "",
                        gearbox = doc.getString("gearbox") ?: "",
                        pricePerDay = doc.getDouble("pricePerDay") ?: 0.0,
                        mileage = (doc.getLong("mileage") ?: 0L).toInt(),
                        status = doc.getString("status") ?: "",
                        available = doc.getBoolean("available") ?: false,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        ratingAverage = doc.getDouble("ratingAverage") ?: 0.0,
                        totalReviews = doc.getLong("totalReviews") ?: 0L
                    )
                }

                cars = loadedCars
                carsCount = loadedCars.size
            }
            .addOnFailureListener {
                message = "Erreur chargement voitures : ${it.message}"
            }

        db.collection("carBrands")
            .get()
            .addOnSuccessListener { result ->
                brands = result.documents.mapNotNull {
                    it.toObject(Brand::class.java)?.copy(id = it.id)
                }
            }

        db.collection("carModels")
            .get()
            .addOnSuccessListener { result ->
                models = result.documents.mapNotNull {
                    it.toObject(CarModel::class.java)?.copy(id = it.id)
                }
            }
    }

    fun validateAgency(agency: AdminAgencyUi) {
        db.collection("agencies")
            .document(agency.id)
            .update("status", "active")
            .addOnSuccessListener {
                val userId = agency.ownerId.ifBlank { agency.id }

                db.collection("users")
                    .document(userId)
                    .update("status", "active")
                    .addOnSuccessListener {
                        message = "Agence validée."
                        loadData()
                    }
                    .addOnFailureListener {
                        message = "Agence validée, mais erreur utilisateur : ${it.message}"
                        loadData()
                    }
            }
            .addOnFailureListener {
                message = "Erreur validation agence : ${it.message}"
            }
    }

    fun refuseAgency(agency: AdminAgencyUi) {
        db.collection("agencies")
            .document(agency.id)
            .update("status", "refused")
            .addOnSuccessListener {
                val userId = agency.ownerId.ifBlank { agency.id }

                db.collection("users")
                    .document(userId)
                    .update("status", "refused")
                    .addOnSuccessListener {
                        message = "Agence refusée."
                        loadData()
                    }
                    .addOnFailureListener {
                        message = "Agence refusée, mais erreur utilisateur : ${it.message}"
                        loadData()
                    }
            }
            .addOnFailureListener {
                message = "Erreur refus agence : ${it.message}"
            }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (detailsPage.isBlank()) 78.dp else 0.dp)
        ) {
            if (detailsPage.isNotBlank()) {
                AdminDetailsScreen(
                    type = detailsPage,
                    users = users,
                    clients = clients,
                    agencies = agencies,
                    cars = cars,
                    message = message,
                    onHome = { detailsPage = "" },
                    onRefresh = { loadData() }
                )
            } else {
                when (selectedTab) {
                    "home" -> AdminHomeScreen(
                        usersCount = usersCount,
                        clientsCount = clientsCount,
                        agenciesCount = agenciesCount,
                        carsCount = carsCount,
                        pendingAgenciesCount = pendingAgencies.size,
                        message = message,
                        onOpenUsers = { detailsPage = "users" },
                        onOpenClients = { detailsPage = "clients" },
                        onOpenAgencies = { detailsPage = "agencies" },
                        onOpenCars = { detailsPage = "cars" },
                        onRefresh = { loadData() },
                        onLogout = {
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo("admin_dashboard") {
                                    inclusive = true
                                }
                            }
                        }
                    )

                    "database" -> AdminCatalogScreen(
                        brands = brands,
                        models = models,
                        onReload = { loadData() }
                    )

                    "requests" -> AdminAgencyRequestsScreen(
                        pendingAgencies = pendingAgencies,
                        message = message,
                        onValidateAgency = { validateAgency(it) },
                        onRefuseAgency = { refuseAgency(it) },
                        onRefresh = { loadData() }
                    )
                }
            }
        }

        if (detailsPage.isBlank()) {
            AdminBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun AdminHomeScreen(
    usersCount: Int,
    clientsCount: Int,
    agenciesCount: Int,
    carsCount: Int,
    pendingAgenciesCount: Int,
    message: String,
    onOpenUsers: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenAgencies: () -> Unit,
    onOpenCars: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Administration ChikAuto",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Tableau de bord général",
                color = Color.Gray
            )
        }

        if (message.isNotEmpty()) {
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStatCard(
                    title = "Utilisateurs",
                    value = usersCount.toString(),
                    icon = "👤",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenUsers
                )

                AdminStatCard(
                    title = "Clients",
                    value = clientsCount.toString(),
                    icon = "🧑",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenClients
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStatCard(
                    title = "Agences",
                    value = agenciesCount.toString(),
                    icon = "🏢",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAgencies
                )

                AdminStatCard(
                    title = "Voitures",
                    value = carsCount.toString(),
                    icon = "🚗",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCars
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Résumé rapide",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Demandes d'agences en attente : $pendingAgenciesCount")
                    Text("Cliquez sur une carte en haut pour voir les détails.")

                }
            }
        }

        item {
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
            ) {
                Text("Actualiser")
            }
        }

        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Se déconnecter")
            }
        }
    }
}

@Composable
fun AdminDetailsScreen(
    type: String,
    users: List<AdminUserUi>,
    clients: List<AdminUserUi>,
    agencies: List<AdminAgencyUi>,
    cars: List<AdminCarUi>,
    message: String,
    onHome: () -> Unit,
    onRefresh: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var localMessage by remember { mutableStateOf("") }

    var userToEdit by remember { mutableStateOf<AdminUserUi?>(null) }
    var agencyToEdit by remember { mutableStateOf<AdminAgencyUi?>(null) }
    var carToEdit by remember { mutableStateOf<AdminCarUi?>(null) }

    var deleteType by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf("") }
    var deleteName by remember { mutableStateOf("") }

    fun isDisabledStatus(status: String): Boolean {
        return status == "disabled" || status == "blocked" || status == "inactive"
    }

    fun deleteDocument(collection: String, id: String) {
        db.collection(collection)
            .document(id)
            .delete()
            .addOnSuccessListener {
                localMessage = "Suppression effectuée."
                deleteType = ""
                deleteId = ""
                deleteName = ""
                onRefresh()
            }
            .addOnFailureListener {
                localMessage = "Erreur suppression : ${it.message}"
            }
    }

    fun toggleUserStatus(user: AdminUserUi) {
        val currentStatus = user.status.ifBlank { "active" }
        val newStatus = if (isDisabledStatus(currentStatus)) "active" else "disabled"

        db.collection("users")
            .document(user.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                localMessage = if (newStatus == "active") {
                    "Compte activé avec succès."
                } else {
                    "Compte désactivé avec succès."
                }
                onRefresh()
            }
            .addOnFailureListener {
                localMessage = "Erreur changement statut : ${it.message}"
            }
    }

    fun toggleAgencyStatus(agency: AdminAgencyUi) {
        val currentStatus = agency.status.ifBlank { "active" }
        val newStatus = if (isDisabledStatus(currentStatus)) "active" else "disabled"
        val ownerId = agency.ownerId.ifBlank { agency.id }

        db.collection("agencies")
            .document(agency.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                db.collection("users")
                    .document(ownerId)
                    .update("status", newStatus)
                    .addOnSuccessListener {
                        localMessage = if (newStatus == "active") {
                            "Agence activée avec succès."
                        } else {
                            "Agence désactivée avec succès."
                        }
                        onRefresh()
                    }
                    .addOnFailureListener {
                        localMessage = "Agence modifiée, mais erreur compte utilisateur : ${it.message}"
                        onRefresh()
                    }
            }
            .addOnFailureListener {
                localMessage = "Erreur changement statut agence : ${it.message}"
            }
    }

    val title = when (type) {
        "users" -> "Tous les utilisateurs"
        "clients" -> "Tous les clients"
        "agencies" -> "Toutes les agences"
        "cars" -> "Tous les véhicules"
        else -> "Détails"
    }

    val subtitle = when (type) {
        "users" -> "Liste complète des comptes utilisateurs"
        "clients" -> "Liste complète des clients inscrits"
        "agencies" -> "Liste complète des agences"
        "cars" -> "Liste complète des véhicules dans la base de données"
        else -> ""
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onHome,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) {
                    Text("⌂ Home")
                }

                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Actualiser")
                }
            }
        }

        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                color = Color.Gray
            )
        }

        if (message.isNotEmpty()) {
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (localMessage.isNotEmpty()) {
            item {
                Text(
                    text = localMessage,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        when (type) {
            "users" -> {
                if (users.isEmpty()) {
                    item { SimpleCard("Aucun utilisateur trouvé.") }
                } else {
                    items(users) { user ->
                        AdminEditableUserCard(
                            user = user,
                            onEdit = { userToEdit = user },
                            onToggleStatus = { toggleUserStatus(user) },
                            onDelete = {
                                deleteType = "users"
                                deleteId = user.id
                                deleteName = user.fullName.ifBlank { user.email.ifBlank { "Utilisateur" } }
                            }
                        )
                    }
                }
            }

            "clients" -> {
                if (clients.isEmpty()) {
                    item { SimpleCard("Aucun client trouvé.") }
                } else {
                    items(clients) { user ->
                        AdminEditableUserCard(
                            user = user,
                            onEdit = { userToEdit = user },
                            onToggleStatus = { toggleUserStatus(user) },
                            onDelete = {
                                deleteType = "users"
                                deleteId = user.id
                                deleteName = user.fullName.ifBlank { user.email.ifBlank { "Client" } }
                            }
                        )
                    }
                }
            }

            "agencies" -> {
                if (agencies.isEmpty()) {
                    item { SimpleCard("Aucune agence trouvée.") }
                } else {
                    items(agencies) { agency ->
                        AdminEditableAgencyCard(
                            agency = agency,
                            onEdit = { agencyToEdit = agency },
                            onToggleStatus = { toggleAgencyStatus(agency) },
                            onDelete = {
                                deleteType = "agencies"
                                deleteId = agency.id
                                deleteName = agency.agencyName.ifBlank { "Agence" }
                            }
                        )
                    }
                }
            }

            "cars" -> {
                if (cars.isEmpty()) {
                    item { SimpleCard("Aucun véhicule trouvé.") }
                } else {
                    items(cars) { car ->
                        AdminEditableCarCard(
                            car = car,
                            onEdit = { carToEdit = car },
                            onDelete = {
                                deleteType = "cars"
                                deleteId = car.id
                                deleteName = "${car.brandName} ${car.modelName}".trim().ifBlank { "Véhicule" }
                            }
                        )
                    }
                }
            }
        }
    }

    userToEdit?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { userToEdit = null },
            onSaved = {
                localMessage = "Utilisateur modifié."
                userToEdit = null
                onRefresh()
            }
        )
    }

    agencyToEdit?.let { agency ->
        EditAgencyDialog(
            agency = agency,
            onDismiss = { agencyToEdit = null },
            onSaved = {
                localMessage = "Agence modifiée."
                agencyToEdit = null
                onRefresh()
            }
        )
    }

    carToEdit?.let { car ->
        EditCarDialog(
            car = car,
            onDismiss = { carToEdit = null },
            onSaved = {
                localMessage = "Véhicule modifié."
                carToEdit = null
                onRefresh()
            }
        )
    }

    if (deleteType.isNotBlank() && deleteId.isNotBlank()) {
        AlertDialog(
            onDismissRequest = {
                deleteType = ""
                deleteId = ""
                deleteName = ""
            },
            title = {
                Text("Confirmer la suppression")
            },
            text = {
                Text("Voulez-vous vraiment supprimer : $deleteName ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteDocument(deleteType, deleteId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        deleteType = ""
                        deleteId = ""
                        deleteName = ""
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun AdminEditableUserCard(
    user: AdminUserUi,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isDisabled = user.status == "disabled" || user.status == "blocked" || user.status == "inactive"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminAvatar(
                    text = user.fullName.take(1).ifBlank { "U" },
                    imageUrl = user.profileImageUrl,
                    icon = "👤"
                )

                Column {
                    Text(
                        text = user.fullName.ifBlank { "Utilisateur" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = user.role.ifBlank { "Rôle non précisé" },
                        color = Color(0xFF1DA1F2)
                    )
                }
            }

            Text("Email : ${user.email.ifBlank { "Non précisé" }}")
            Text("Téléphone : ${user.phone.ifBlank { "Non précisé" }}")
            Text("Wilaya : ${user.city.ifBlank { "Non précisée" }}")

            Text(
                text = if (isDisabled) "Statut : désactivé" else "Statut : ${user.status.ifBlank { "active" }}",
                color = if (isDisabled) Color(0xFFD32F2F) else Color(0xFF13A10E),
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) {
                    Text("Modifier")
                }

                OutlinedButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isDisabled) "Activer" else "Désactiver")
                }
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Supprimer")
            }
        }
    }
}

@Composable
fun AdminEditableAgencyCard(
    agency: AdminAgencyUi,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isDisabled = agency.status == "disabled" || agency.status == "blocked" || agency.status == "inactive"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminAvatar(
                    text = agency.agencyName.take(1).ifBlank { "A" },
                    imageUrl = agency.profileImageUrl,
                    icon = "🏢"
                )

                Column {
                    Text(
                        text = agency.agencyName.ifBlank { "Agence" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "⭐ ${String.format(Locale.US, "%.1f", agency.ratingAverage)} • ${agency.totalReviews} avis",
                        color = Color(0xFF1DA1F2)
                    )
                }
            }

            Text("Wilaya : ${agency.city.ifBlank { "Non précisée" }}")
            Text("Adresse : ${agency.address.ifBlank { "Non précisée" }}")
            Text("Téléphone : ${agency.phone.ifBlank { "Non précisé" }}")
            Text("Email : ${agency.email.ifBlank { "Non précisé" }}")

            Text(
                text = if (isDisabled) "Statut : désactivée" else "Statut : ${agency.status.ifBlank { "active" }}",
                color = if (isDisabled) Color(0xFFD32F2F) else Color(0xFF13A10E),
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) {
                    Text("Modifier")
                }

                OutlinedButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isDisabled) "Activer" else "Désactiver")
                }
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Supprimer")
            }
        }
    }
}

@Composable
fun AdminEditableCarCard(
    car: AdminCarUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (car.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = car.imageUrl,
                    contentDescription = "Voiture",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE9EEF3)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🚗",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            Text(
                text = "${car.brandName} ${car.modelName}".trim().ifBlank { "Voiture" },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Agence : ${car.agencyName.ifBlank { "Non précisée" }}")
            Text("Wilaya : ${car.city.ifBlank { "Non précisée" }}")
            Text("Prix : ${car.pricePerDay} DA / jour")
            Text("Kilométrage : ${car.mileage} km")
            Text("Statut : ${car.status.ifBlank { "Non précisé" }}")
            Text("Disponible : ${if (car.available) "Oui" else "Non"}")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(car.type.ifBlank { "Type" }) }
                )

                AssistChip(
                    onClick = {},
                    label = { Text(car.fuel.ifBlank { "Carburant" }) }
                )

                AssistChip(
                    onClick = {},
                    label = { Text(car.gearbox.ifBlank { "Boîte" }) }
                )
            }

            Text(
                text = "⭐ ${String.format(Locale.US, "%.1f", car.ratingAverage)} • ${car.totalReviews} avis",
                color = Color(0xFF1DA1F2),
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) {
                    Text("Modifier")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Supprimer")
                }
            }
        }
    }
}

@Composable
fun EditUserDialog(
    user: AdminUserUi,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var fullName by remember { mutableStateOf(user.fullName) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone) }
    var city by remember { mutableStateOf(user.city) }
    var role by remember { mutableStateOf(user.role) }
    var status by remember { mutableStateOf(user.status.ifBlank { "active" }) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier utilisateur") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nom complet") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Wilaya") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    AppDropdown(
                        label = "Rôle",
                        value = role,
                        items = listOf("client", "agency", "admin"),
                        onItemSelected = { role = it }
                    )
                }

                item {
                    AppDropdown(
                        label = "Statut",
                        value = status,
                        items = listOf("active", "disabled", "pending", "refused", "blocked"),
                        onItemSelected = { status = it }
                    )
                }

                if (error.isNotEmpty()) {
                    item {
                        Text(error, color = Color.Red)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (user.id.isBlank()) {
                        error = "ID utilisateur introuvable."
                        return@Button
                    }

                    db.collection("users")
                        .document(user.id)
                        .update(
                            mapOf(
                                "fullName" to fullName.trim(),
                                "email" to email.trim(),
                                "phone" to phone.trim(),
                                "city" to city.trim(),
                                "role" to role.trim(),
                                "status" to status.trim()
                            )
                        )
                        .addOnSuccessListener {
                            onSaved()
                        }
                        .addOnFailureListener {
                            error = "Erreur modification : ${it.message}"
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditAgencyDialog(
    agency: AdminAgencyUi,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var agencyName by remember { mutableStateOf(agency.agencyName) }
    var city by remember { mutableStateOf(agency.city) }
    var address by remember { mutableStateOf(agency.address) }
    var phone by remember { mutableStateOf(agency.phone) }
    var email by remember { mutableStateOf(agency.email) }
    var status by remember { mutableStateOf(agency.status.ifBlank { "active" }) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier agence") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = agencyName,
                        onValueChange = { agencyName = it },
                        label = { Text("Nom de l'agence") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Wilaya") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adresse") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    AppDropdown(
                        label = "Statut",
                        value = status,
                        items = listOf("active", "disabled", "pending", "refused", "blocked"),
                        onItemSelected = { status = it }
                    )
                }

                if (error.isNotEmpty()) {
                    item {
                        Text(error, color = Color.Red)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (agency.id.isBlank()) {
                        error = "ID agence introuvable."
                        return@Button
                    }

                    db.collection("agencies")
                        .document(agency.id)
                        .update(
                            mapOf(
                                "agencyName" to agencyName.trim(),
                                "city" to city.trim(),
                                "address" to address.trim(),
                                "phone" to phone.trim(),
                                "email" to email.trim(),
                                "status" to status.trim()
                            )
                        )
                        .addOnSuccessListener {
                            val ownerId = agency.ownerId.ifBlank { agency.id }

                            db.collection("users")
                                .document(ownerId)
                                .update("status", status.trim())
                                .addOnSuccessListener {
                                    onSaved()
                                }
                                .addOnFailureListener {
                                    onSaved()
                                }
                        }
                        .addOnFailureListener {
                            error = "Erreur modification : ${it.message}"
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditCarDialog(
    car: AdminCarUi,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var brandName by remember { mutableStateOf(car.brandName) }
    var modelName by remember { mutableStateOf(car.modelName) }
    var agencyName by remember { mutableStateOf(car.agencyName) }
    var city by remember { mutableStateOf(car.city) }
    var type by remember { mutableStateOf(car.type) }
    var fuel by remember { mutableStateOf(car.fuel) }
    var gearbox by remember { mutableStateOf(car.gearbox) }
    var pricePerDay by remember { mutableStateOf(car.pricePerDay.toString()) }
    var mileage by remember { mutableStateOf(car.mileage.toString()) }
    var status by remember { mutableStateOf(car.status) }
    var availableText by remember { mutableStateOf(if (car.available) "Oui" else "Non") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier véhicule") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = brandName,
                        onValueChange = { brandName = it },
                        label = { Text("Marque") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Modèle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = agencyName,
                        onValueChange = { agencyName = it },
                        label = { Text("Agence") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Wilaya") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = fuel,
                        onValueChange = { fuel = it },
                        label = { Text("Carburant") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = gearbox,
                        onValueChange = { gearbox = it },
                        label = { Text("Boîte") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = pricePerDay,
                        onValueChange = { pricePerDay = it },
                        label = { Text("Prix par jour") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = mileage,
                        onValueChange = { mileage = it },
                        label = { Text("Kilométrage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    AppDropdown(
                        label = "Statut",
                        value = status,
                        items = listOf("active", "pending", "refused", "blocked", "reserved", "maintenance", "available"),
                        onItemSelected = { status = it }
                    )
                }

                item {
                    AppDropdown(
                        label = "Disponible",
                        value = availableText,
                        items = listOf("Oui", "Non"),
                        onItemSelected = { availableText = it }
                    )
                }

                if (error.isNotEmpty()) {
                    item {
                        Text(error, color = Color.Red)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (car.id.isBlank()) {
                        error = "ID véhicule introuvable."
                        return@Button
                    }

                    val price = pricePerDay.toDoubleOrNull()
                    val km = mileage.toIntOrNull()

                    if (price == null) {
                        error = "Le prix doit être un nombre."
                        return@Button
                    }

                    if (km == null) {
                        error = "Le kilométrage doit être un nombre."
                        return@Button
                    }

                    db.collection("cars")
                        .document(car.id)
                        .update(
                            mapOf(
                                "brandName" to brandName.trim(),
                                "modelName" to modelName.trim(),
                                "agencyName" to agencyName.trim(),
                                "city" to city.trim(),
                                "type" to type.trim(),
                                "fuel" to fuel.trim(),
                                "gearbox" to gearbox.trim(),
                                "pricePerDay" to price,
                                "mileage" to km,
                                "status" to status.trim(),
                                "available" to (availableText == "Oui")
                            )
                        )
                        .addOnSuccessListener {
                            onSaved()
                        }
                        .addOnFailureListener {
                            error = "Erreur modification : ${it.message}"
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun AdminCatalogScreen(
    brands: List<Brand>,
    models: List<CarModel>,
    onReload: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var newBrand by remember { mutableStateOf("") }
    var selectedBrandName by remember { mutableStateOf("") }
    var selectedBrandId by remember { mutableStateOf("") }
    var newModelName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Base de données",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Gestion des marques et modèles",
                color = Color.Gray
            )
        }

        if (message.isNotEmpty()) {
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "➕ Ajouter une marque",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = newBrand,
                        onValueChange = {
                            newBrand = it
                            message = ""
                        },
                        label = { Text("Exemple : Audi") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (newBrand.isBlank()) {
                                message = "Veuillez saisir une marque."
                                return@Button
                            }

                            val exists = brands.any {
                                it.name.equals(newBrand.trim(), ignoreCase = true)
                            }

                            if (exists) {
                                message = "Cette marque existe déjà."
                                return@Button
                            }

                            db.collection("carBrands")
                                .add(
                                    hashMapOf(
                                        "name" to newBrand.trim(),
                                        "active" to true
                                    )
                                )
                                .addOnSuccessListener {
                                    newBrand = ""
                                    message = "Marque ajoutée."
                                    onReload()
                                }
                                .addOnFailureListener {
                                    message = "Erreur ajout marque : ${it.message}"
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                    ) {
                        Text("Ajouter la marque")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🚘 Ajouter un modèle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    AppDropdown(
                        label = "Marque",
                        value = selectedBrandName,
                        items = brands.map { it.name },
                        onItemSelected = { value ->
                            selectedBrandName = value
                            message = ""

                            val brand = brands.firstOrNull { it.name == value }
                            selectedBrandId = brand?.id ?: ""
                        }
                    )

                    OutlinedTextField(
                        value = newModelName,
                        onValueChange = {
                            newModelName = it
                            message = ""
                        },
                        label = { Text("Exemple : A3") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    AppDropdown(
                        label = "Type",
                        value = selectedType,
                        items = AppOptions.carTypes,
                        onItemSelected = {
                            selectedType = it
                            message = ""
                        }
                    )

                    Button(
                        onClick = {
                            if (selectedBrandId.isBlank()) {
                                message = "Veuillez choisir une marque valide."
                                return@Button
                            }

                            if (newModelName.isBlank()) {
                                message = "Veuillez saisir le modèle."
                                return@Button
                            }

                            if (!AppOptions.carTypes.contains(selectedType)) {
                                message = "Veuillez choisir un type valide."
                                return@Button
                            }

                            val exists = models.any {
                                it.brandId == selectedBrandId &&
                                        it.name.equals(newModelName.trim(), ignoreCase = true)
                            }

                            if (exists) {
                                message = "Ce modèle existe déjà pour cette marque."
                                return@Button
                            }

                            db.collection("carModels")
                                .add(
                                    hashMapOf(
                                        "brandId" to selectedBrandId,
                                        "brandName" to selectedBrandName,
                                        "name" to newModelName.trim(),
                                        "type" to selectedType,
                                        "active" to true
                                    )
                                )
                                .addOnSuccessListener {
                                    newModelName = ""
                                    selectedType = ""
                                    message = "Modèle ajouté."
                                    onReload()
                                }
                                .addOnFailureListener {
                                    message = "Erreur ajout modèle : ${it.message}"
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                    ) {
                        Text("Ajouter le modèle")
                    }
                }
            }
        }

        item {
            Text(
                text = "Marques existantes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (brands.isEmpty()) {
            item {
                SimpleCard("Aucune marque ajoutée.")
            }
        } else {
            items(brands) { brand ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🚗 ${brand.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        val brandModels = models.filter { it.brandId == brand.id }

                        if (brandModels.isEmpty()) {
                            Text("Aucun modèle.")
                        } else {
                            brandModels.forEach {
                                Text("- ${it.name} (${it.type})")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAgencyRequestsScreen(
    pendingAgencies: List<AdminAgencyUi>,
    message: String,
    onValidateAgency: (AdminAgencyUi) -> Unit,
    onRefuseAgency: (AdminAgencyUi) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Demandes d'agences",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Validation ou refus des agences",
                color = Color.Gray
            )
        }

        if (message.isNotEmpty()) {
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
            ) {
                Text("Actualiser les demandes")
            }
        }

        if (pendingAgencies.isEmpty()) {
            item {
                SimpleCard("Aucune demande agence pour le moment.")
            }
        } else {
            items(pendingAgencies) { agency ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AdminAvatar(
                                text = agency.agencyName.take(1).ifBlank { "A" },
                                imageUrl = agency.profileImageUrl,
                                icon = "🏢"
                            )

                            Column {
                                Text(
                                    text = agency.agencyName.ifBlank { "Agence" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text("Statut : ${agency.status.ifBlank { "pending" }}")
                            }
                        }

                        Text("Wilaya : ${agency.city.ifBlank { "Non précisée" }}")
                        Text("Adresse : ${agency.address.ifBlank { "Non précisée" }}")
                        Text("Téléphone : ${agency.phone.ifBlank { "Non précisé" }}")
                        Text("Email : ${agency.email.ifBlank { "Non précisé" }}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onValidateAgency(agency) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13A10E))
                            ) {
                                Text("Valider")
                            }

                            OutlinedButton(
                                onClick = { onRefuseAgency(agency) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
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
fun AdminAvatar(
    text: String,
    imageUrl: String,
    icon: String
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .width(48.dp)
            .clip(CircleShape)
            .background(Color(0xFF1DA1F2)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = text.uppercase().ifBlank { icon },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = title,
                color = Color.Gray
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1DA1F2)
            )

            Text(
                text = "Voir détails",
                color = Color(0xFF1DA1F2),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun AdminBottomBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminBottomItem(
                key = "home",
                label = "Accueil",
                icon = "⌂",
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            AdminBottomItem(
                key = "database",
                label = "Base",
                icon = "▣",
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            AdminBottomItem(
                key = "requests",
                label = "Demandes",
                icon = "◇",
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    }
}

@Composable
fun AdminBottomItem(
    key: String,
    label: String,
    icon: String,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val selected = selectedTab == key

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            onTabSelected(key)
        }
    ) {
        Text(
            text = icon,
            color = if (selected) Color(0xFF1DA1F2) else Color.LightGray,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            color = if (selected) Color(0xFF1DA1F2) else Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SimpleCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = Color.Gray
        )
    }
}