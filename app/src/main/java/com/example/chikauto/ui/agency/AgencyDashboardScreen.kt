package com.example.chikauto.ui.agency

import android.net.Uri
import androidx.activity.compose.BackHandler
import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chikauto.ai.PredictionEngine
import com.example.chikauto.ai.PredictionResult
import com.example.chikauto.data.model.AppOptions
import com.example.chikauto.data.model.Brand
import com.example.chikauto.data.model.Car
import com.example.chikauto.data.model.CarModel
import com.example.chikauto.data.model.MaintenanceAgent
import com.example.chikauto.ui.components.AppDropdown
import com.example.chikauto.ui.components.EditProfileDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class AgencyReservationUi(
    val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val clientProfileImageUrl: String = "",
    val carId: String = "",
    val carName: String = "",
    val carImageUrl: String = "",
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    val startDateText: String = "",
    val endDateText: String = "",
    val totalDays: Int = 0,
    val totalPrice: Double = 0.0,
    val status: String = "pending"
)

data class AgencyConversationUi(
    val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val agencyId: String = "",
    val agencyName: String = "",
    val carId: String = "",
    val carName: String = "",
    val lastMessage: String = "",
    val updatedAt: Long = 0L,
    val unreadForAgency: Long = 0L
)

data class AgencyChatMessageUi(
    val id: String = "",
    val senderId: String = "",
    val senderRole: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)

@Composable
fun AgencyDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val agencyId = auth.currentUser?.uid ?: ""

    var selectedTab by remember { mutableStateOf("dashboard") }
    var showReservationHistory by remember { mutableStateOf(false) }

    var agencyName by remember { mutableStateOf("Agence") }
    var email by remember { mutableStateOf(auth.currentUser?.email ?: "") }
    var agencyPhone by remember { mutableStateOf("") }
    var agencyCity by remember { mutableStateOf("") }
    var agencyImageUrl by remember { mutableStateOf("") }

    var brands by remember { mutableStateOf(listOf<Brand>()) }
    var models by remember { mutableStateOf(listOf<CarModel>()) }
    var cars by remember { mutableStateOf(listOf<Car>()) }
    var agents by remember { mutableStateOf(listOf<MaintenanceAgent>()) }
    var reservations by remember { mutableStateOf(listOf<AgencyReservationUi>()) }
    var conversations by remember { mutableStateOf(listOf<AgencyConversationUi>()) }
    var selectedConversation by remember { mutableStateOf<AgencyConversationUi?>(null) }
    var openMessages by remember { mutableStateOf(false) }

    var message by remember { mutableStateOf("") }

    var carToEdit by remember { mutableStateOf<Car?>(null) }
    var carToPlanning by remember { mutableStateOf<Car?>(null) }
    var agentToEdit by remember { mutableStateOf<MaintenanceAgent?>(null) }
    var showEditProfile by remember { mutableStateOf(false) }
    var conflictReservation by remember { mutableStateOf<AgencyReservationUi?>(null) }

    fun loadData() {
        if (agencyId.isBlank()) return

        db.collection("agencies")
            .document(agencyId)
            .get()
            .addOnSuccessListener { doc ->
                agencyName = doc.getString("agencyName") ?: "Agence"
                email = doc.getString("email") ?: auth.currentUser?.email.orEmpty()
                agencyPhone = doc.getString("phone") ?: ""
                agencyCity = doc.getString("city") ?: ""
                agencyImageUrl = doc.getString("profileImageUrl") ?: ""
            }
            .addOnFailureListener {
                message = "Erreur chargement agence : ${it.message}"
            }

        db.collection("carBrands")
            .get()
            .addOnSuccessListener { result ->
                brands = result.documents.mapNotNull {
                    it.toObject(Brand::class.java)?.copy(id = it.id)
                }
            }
            .addOnFailureListener {
                message = "Erreur chargement marques : ${it.message}"
            }

        db.collection("carModels")
            .get()
            .addOnSuccessListener { result ->
                models = result.documents.mapNotNull {
                    it.toObject(CarModel::class.java)?.copy(id = it.id)
                }
            }
            .addOnFailureListener {
                message = "Erreur chargement modèles : ${it.message}"
            }

        db.collection("cars")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { result ->
                cars = result.documents.mapNotNull {
                    it.toObject(Car::class.java)?.copy(id = it.id)
                }
            }
            .addOnFailureListener {
                message = "Erreur chargement voitures : ${it.message}"
            }

        db.collection("maintenanceAgents")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { result ->
                agents = result.documents.mapNotNull {
                    it.toObject(MaintenanceAgent::class.java)?.copy(id = it.id)
                }
            }
            .addOnFailureListener {
                message = "Erreur chargement agents : ${it.message}"
            }

        db.collection("reservations")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { result ->
                reservations = result.documents.map { doc ->
                    AgencyReservationUi(
                        id = doc.id,
                        clientId = doc.getString("clientId") ?: "",
                        clientName = doc.getString("clientName") ?: "",
                        clientPhone = doc.getString("clientPhone") ?: "",
                        clientEmail = doc.getString("clientEmail") ?: "",
                        clientProfileImageUrl = doc.getString("clientProfileImageUrl") ?: "",
                        carId = doc.getString("carId") ?: "",
                        carName = doc.getString("carName") ?: "",
                        carImageUrl = doc.getString("carImageUrl") ?: "",
                        startDateMillis = doc.getLong("startDateMillis") ?: 0L,
                        endDateMillis = doc.getLong("endDateMillis") ?: 0L,
                        startDateText = doc.getString("startDateText") ?: "",
                        endDateText = doc.getString("endDateText") ?: "",
                        totalDays = (doc.getLong("totalDays") ?: 0L).toInt(),
                        totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                        status = doc.getString("status") ?: "pending"
                    )
                }
            }
            .addOnFailureListener {
                message = "Erreur chargement réservations : ${it.message}"
            }

        db.collection("conversations")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { result ->
                conversations = result.documents.map { doc ->
                    AgencyConversationUi(
                        id = doc.id,
                        clientId = doc.getString("clientId") ?: "",
                        clientName = doc.getString("clientName") ?: "Client",
                        agencyId = doc.getString("agencyId") ?: "",
                        agencyName = doc.getString("agencyName") ?: agencyName,
                        carId = doc.getString("carId") ?: "",
                        carName = doc.getString("carName") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        updatedAt = doc.getLong("updatedAt") ?: 0L,
                        unreadForAgency = doc.getLong("unreadForAgency") ?: 0L
                    )
                }.sortedByDescending { it.updatedAt }
            }
            .addOnFailureListener {
                message = "Erreur chargement messagerie : ${it.message}"
            }
    }

    fun acceptReservation(reservation: AgencyReservationUi) {
        db.collection("reservations")
            .whereEqualTo("carId", reservation.carId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                val conflict = result.documents.any { doc ->
                    if (doc.id == reservation.id) return@any false
                    val s = doc.getLong("startDateMillis") ?: 0L
                    val e = doc.getLong("endDateMillis") ?: 0L
                    reservation.startDateMillis <= e && s <= reservation.endDateMillis
                }

                if (conflict) {
                    conflictReservation = reservation
                    message = "Vous ne pouvez pas accepter cette demande : la voiture a été réservée dans ces dates."
                    return@addOnSuccessListener
                }

                db.collection("reservations")
                    .document(reservation.id)
                    .update("status", "accepted")
                    .addOnSuccessListener {
                        val carRef = db.collection("cars").document(reservation.carId)

                        db.runTransaction { transaction ->
                            val snapshot = transaction.get(carRef)
                            val previous = snapshot.getLong("previousRentals") ?: 0L

                            transaction.update(carRef, "previousRentals", previous + 1)
                        }.addOnSuccessListener {
                            message = "Réservation acceptée."
                            loadData()
                        }.addOnFailureListener {
                            message = "Réservation acceptée, mais erreur MAJ véhicule : ${it.message}"
                            loadData()
                        }
                    }
                    .addOnFailureListener {
                        message = "Erreur acceptation : ${it.message}"
                    }
            }
            .addOnFailureListener {
                message = "Erreur vérification disponibilité : ${it.message}"
            }
    }

    fun refuseReservation(reservation: AgencyReservationUi) {
        db.collection("reservations")
            .document(reservation.id)
            .update("status", "refused")
            .addOnSuccessListener {
                message = "Réservation refusée."
                loadData()
            }
            .addOnFailureListener {
                message = "Erreur refus : ${it.message}"
            }
    }

    fun openConversationFromReservation(reservation: AgencyReservationUi) {
        db.collection("conversations")
            .whereEqualTo("agencyId", agencyId)
            .whereEqualTo("clientId", reservation.clientId)
            .whereEqualTo("carId", reservation.carId)
            .get()
            .addOnSuccessListener { result ->
                val existing = result.documents.firstOrNull()
                if (existing != null) {
                    selectedConversation = AgencyConversationUi(
                        id = existing.id,
                        clientId = existing.getString("clientId") ?: reservation.clientId,
                        clientName = existing.getString("clientName") ?: reservation.clientName,
                        agencyId = agencyId,
                        agencyName = agencyName,
                        carId = existing.getString("carId") ?: reservation.carId,
                        carName = existing.getString("carName") ?: reservation.carName,
                        lastMessage = existing.getString("lastMessage") ?: "",
                        updatedAt = existing.getLong("updatedAt") ?: 0L,
                        unreadForAgency = existing.getLong("unreadForAgency") ?: 0L
                    )
                    openMessages = true
                } else {
                    val ref = db.collection("conversations").document()
                    val data = hashMapOf(
                        "clientId" to reservation.clientId,
                        "clientName" to reservation.clientName,
                        "agencyId" to agencyId,
                        "agencyName" to agencyName,
                        "carId" to reservation.carId,
                        "carName" to reservation.carName,
                        "lastMessage" to "",
                        "updatedAt" to System.currentTimeMillis(),
                        "unreadForClient" to 0L,
                        "unreadForAgency" to 0L
                    )
                    ref.set(data).addOnSuccessListener {
                        selectedConversation = AgencyConversationUi(
                            id = ref.id, clientId = reservation.clientId, clientName = reservation.clientName,
                            agencyId = agencyId, agencyName = agencyName, carId = reservation.carId, carName = reservation.carName
                        )
                        openMessages = true
                        loadData()
                    }
                }
            }
            .addOnFailureListener { message = "Erreur ouverture message : ${it.message}" }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (openMessages) {
        AgencyMessagesScreen(
            agencyId = agencyId,
            agencyName = agencyName,
            conversations = conversations,
            selectedConversation = selectedConversation,
            onBack = {
                selectedConversation = null
                openMessages = false
                loadData()
            },
            onSelectConversation = { selectedConversation = it },
            onRefresh = { loadData() }
        )
        return
    }

    if (showReservationHistory) {
        ReservationHistoryScreen(
            reservations = reservations,
            onBack = { showReservationHistory = false },
            onRefresh = { loadData() }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 78.dp)
        ) {
            when (selectedTab) {
                "dashboard" -> AgencyDashboardTab(
                    agencyName = agencyName,
                    agencyImageUrl = agencyImageUrl,
                    cars = cars,
                    agents = agents,
                    reservations = reservations,
                    message = message,
                    onAccept = { acceptReservation(it) },
                    onRefuse = { refuseReservation(it) },
                    onMessage = { openConversationFromReservation(it) },
                    onOpenHistory = { showReservationHistory = true },
                    onOpenMessages = { openMessages = true },
                    unreadMessagesCount = conversations.sumOf { it.unreadForAgency }.toInt(),
                    onRefresh = { loadData() }
                )

                "add_car" -> AgencyAddCarTab(
                    brands = brands,
                    models = models,
                    agencyId = agencyId,
                    agencyName = agencyName,
                    onCarAdded = {
                        message = "Voiture ajoutée avec succès."
                        loadData()
                        selectedTab = "fleet"
                    }
                )

                "fleet" -> AgencyFleetTab(
                    cars = cars,
                    agents = agents,
                    reservations = reservations,
                    onOpenPlanning = { carToPlanning = it },
                    onEditCar = { carToEdit = it },
                    onDeleteCar = { car ->
                        db.collection("cars")
                            .document(car.id)
                            .delete()
                            .addOnSuccessListener {
                                message = "Voiture supprimée."
                                loadData()
                            }
                            .addOnFailureListener {
                                message = "Erreur suppression : ${it.message}"
                            }
                    },
                    onMakeAvailable = { car ->
                        db.collection("cars")
                            .document(car.id)
                            .update(
                                mapOf(
                                    "available" to true,
                                    "status" to "available"
                                )
                            )
                            .addOnSuccessListener {
                                message = "Voiture disponible."
                                loadData()
                            }
                            .addOnFailureListener {
                                message = "Erreur disponibilité : ${it.message}"
                            }
                    },
                    onAssignMaintenance = { car, agent ->
                        val task = hashMapOf(
                            "carId" to car.id,
                            "agencyId" to agencyId,
                            "agentId" to agent.id,
                            "agentName" to "${agent.firstName} ${agent.lastName}",
                            "description" to "Entretien / lavage",
                            "status" to "planned"
                        )

                        db.collection("maintenanceTasks")
                            .add(task)
                            .addOnSuccessListener {
                                db.collection("cars")
                                    .document(car.id)
                                    .update(
                                        mapOf(
                                            "available" to false,
                                            "status" to "maintenance"
                                        )
                                    )
                                    .addOnSuccessListener {
                                        message = "Voiture assignée à l’entretien."
                                        loadData()
                                    }
                                    .addOnFailureListener {
                                        message = "Erreur MAJ voiture : ${it.message}"
                                    }
                            }
                            .addOnFailureListener {
                                message = "Erreur entretien : ${it.message}"
                            }
                    }
                )

                "agents" -> AgencyAgentsTab(
                    agencyId = agencyId,
                    agents = agents,
                    onAgentAdded = {
                        message = "Agent ajouté."
                        loadData()
                    },
                    onEditAgent = { agentToEdit = it },
                    onDeleteAgent = { agent ->
                        db.collection("maintenanceAgents")
                            .document(agent.id)
                            .delete()
                            .addOnSuccessListener {
                                message = "Agent supprimé."
                                loadData()
                            }
                            .addOnFailureListener {
                                message = "Erreur suppression agent : ${it.message}"
                            }
                    }
                )

                "profile" -> AgencyProfileTab(
                    agencyName = agencyName,
                    email = email,
                    phone = agencyPhone,
                    city = agencyCity,
                    agencyImageUrl = agencyImageUrl,
                    onEditProfile = { showEditProfile = true },
                    onRefresh = { loadData() },
                    onLogout = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("agency_dashboard") {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }

        AgencyBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    carToPlanning?.let { car ->
        CarPlanningDialog(
            car = car,
            reservations = reservations,
            onDismiss = { carToPlanning = null },
            onChanged = {
                message = it
                loadData()
            }
        )
    }

    carToEdit?.let { car ->
        EditCarDialog(
            car = car,
            brands = brands,
            models = models,
            onDismiss = { carToEdit = null },
            onSave = { updated ->
                db.collection("cars")
                    .document(car.id)
                    .update(updated)
                    .addOnSuccessListener {
                        message = "Voiture modifiée."
                        carToEdit = null
                        loadData()
                    }
                    .addOnFailureListener {
                        message = "Erreur modification voiture : ${it.message}"
                    }
            }
        )
    }

    agentToEdit?.let { agent ->
        EditAgentDialog(
            agent = agent,
            onDismiss = { agentToEdit = null },
            onSave = { updated ->
                db.collection("maintenanceAgents")
                    .document(agent.id)
                    .update(updated)
                    .addOnSuccessListener {
                        message = "Agent modifié."
                        agentToEdit = null
                        loadData()
                    }
                    .addOnFailureListener {
                        message = "Erreur modification agent : ${it.message}"
                    }
            }
        )
    }

    if (showEditProfile) {
        EditProfileDialog(
            role = "agency",
            currentName = agencyName,
            currentEmail = email,
            currentPhone = agencyPhone,
            currentCity = agencyCity,
            currentImageUrl = agencyImageUrl,
            onDismiss = { showEditProfile = false },
            onUpdated = {
                showEditProfile = false
                message = "Profil agence modifié avec succès."
                loadData()
            }
        )
    }

    conflictReservation?.let { reservation ->
        AlertDialog(
            onDismissRequest = { conflictReservation = null },
            title = { Text("Conflit de réservation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vous ne pouvez pas accepter cette demande.")
                    Text("La voiture ${reservation.carName.ifBlank { "sélectionnée" }} est déjà réservée dans ces dates.")
                    Text("Période demandée : du ${reservation.startDateText} au ${reservation.endDateText}")
                    Text("Vous pouvez envoyer un message au client pour proposer d’autres dates, ou refuser la demande.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        conflictReservation = null
                        openConversationFromReservation(reservation)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) {
                    Text("Message")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { conflictReservation = null }) {
                        Text("Annuler")
                    }
                    OutlinedButton(
                        onClick = {
                            conflictReservation = null
                            refuseReservation(reservation)
                        }
                    ) {
                        Text("Refuser")
                    }
                }
            }
        )
    }
}

@Composable
fun AgencyDashboardTab(
    agencyName: String,
    agencyImageUrl: String,
    cars: List<Car>,
    agents: List<MaintenanceAgent>,
    reservations: List<AgencyReservationUi>,
    message: String,
    onAccept: (AgencyReservationUi) -> Unit,
    onRefuse: (AgencyReservationUi) -> Unit,
    onMessage: (AgencyReservationUi) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMessages: () -> Unit,
    unreadMessagesCount: Int,
    onRefresh: () -> Unit
) {
    val availableCars = cars.count { it.available && it.status == "available" }
    val maintenanceCars = cars.count { it.status == "maintenance" }
    val pendingRequests = reservations.count { it.status == "pending" }
    val acceptedRequests = reservations.count { it.status == "accepted" }

    val pendingReservations = reservations
        .filter { it.status == "pending" }
        .sortedByDescending { it.startDateMillis }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AgencyHeader(
                title = agencyName,
                subtitle = "Tableau de bord agence",
                imageUrl = agencyImageUrl
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
                AgencyStatCard(
                    title = "Voitures",
                    value = cars.size.toString(),
                    modifier = Modifier.weight(1f)
                )

                AgencyStatCard(
                    title = "Disponibles",
                    value = availableCars.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AgencyStatCard(
                    title = "Entretien",
                    value = maintenanceCars.toString(),
                    modifier = Modifier.weight(1f)
                )

                AgencyStatCard(
                    title = "Demandes",
                    value = pendingRequests.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AgencyStatCard(
                    title = "Agents",
                    value = agents.size.toString(),
                    modifier = Modifier.weight(1f)
                )

                AgencyStatCard(
                    title = "Acceptées",
                    value = acceptedRequests.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                    ) {
                        Text("Actualiser")
                    }

                    OutlinedButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Historique")
                    }
                }

                Button(
                    onClick = onOpenMessages,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292F))
                ) {
                    Text(if (unreadMessagesCount > 0) "✉ Messagerie • $unreadMessagesCount" else "✉ Messagerie")
                }
            }
        }

        item {
            Text(
                text = "Demandes de réservation en attente",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (pendingReservations.isEmpty()) {
            item {
                AgencyEmptyCard("Aucune demande de réservation en attente.")
            }
        } else {
            items(pendingReservations) { reservation ->
                ReservationRequestCard(
                    reservation = reservation,
                    car = cars.firstOrNull { it.id == reservation.carId },
                    onAccept = { onAccept(reservation) },
                    onRefuse = { onRefuse(reservation) },
                    onMessage = { onMessage(reservation) }
                )
            }
        }
    }
}

@Composable
fun ReservationRequestCard(
    reservation: AgencyReservationUi,
    car: Car?,
    onAccept: () -> Unit,
    onRefuse: () -> Unit,
    onMessage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileCircle(
                    text = reservation.clientName.take(1).ifBlank { "C" },
                    imageUrl = reservation.clientProfileImageUrl
                )

                Column {
                    Text(
                        text = reservation.clientName.ifBlank { "Client" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Tél : ${reservation.clientPhone.ifBlank { "Non précisé" }}")
                    Text("Email : ${reservation.clientEmail.ifBlank { "Non précisé" }}")
                }
            }

            if (reservation.carImageUrl.isNotBlank()) {
                AsyncImage(
                    model = reservation.carImageUrl,
                    contentDescription = "Voiture réservée",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = "Voiture : ${
                    reservation.carName.ifBlank {
                        car?.let { "${it.brandName} ${it.modelName}" } ?: "Voiture"
                    }
                }"
            )

            Text("Du : ${reservation.startDateText}")
            Text("Au : ${reservation.endDateText}")
            Text("Durée : ${reservation.totalDays} jour(s)")
            Text("Total : ${reservation.totalPrice} DA")

            Text(
                text = "Statut : en attente",
                color = Color(0xFF1DA1F2),
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13A10E)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Accepter")
                }

                OutlinedButton(
                    onClick = onRefuse,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Refuser")
                }
            }

            OutlinedButton(
                onClick = onMessage,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Message")
            }
        }
    }
}

@Composable
fun ReservationHistoryScreen(
    reservations: List<AgencyReservationUi>,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val history = reservations
        .filter { it.status != "pending" }
        .sortedByDescending { it.startDateMillis }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) {
                    Text("← Retour")
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
                text = "Historique des réservations",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Réservations acceptées, refusées ou annulées",
                color = Color.Gray
            )
        }

        if (history.isEmpty()) {
            item {
                AgencyEmptyCard("Aucune réservation dans l’historique.")
            }
        } else {
            items(history) { reservation ->
                ReservationHistoryCard(reservation = reservation)
            }
        }
    }
}

@Composable
fun ReservationHistoryCard(
    reservation: AgencyReservationUi
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileCircle(
                    text = reservation.clientName.take(1).ifBlank { "C" },
                    imageUrl = reservation.clientProfileImageUrl
                )

                Column {
                    Text(
                        text = reservation.clientName.ifBlank { "Client" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Tél : ${reservation.clientPhone.ifBlank { "Non précisé" }}")
                    Text("Email : ${reservation.clientEmail.ifBlank { "Non précisé" }}")
                }
            }

            if (reservation.carImageUrl.isNotBlank()) {
                AsyncImage(
                    model = reservation.carImageUrl,
                    contentDescription = "Voiture",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Text("Voiture : ${reservation.carName.ifBlank { "Voiture" }}")
            Text("Du : ${reservation.startDateText}")
            Text("Au : ${reservation.endDateText}")
            Text("Durée : ${reservation.totalDays} jour(s)")
            Text("Total : ${reservation.totalPrice} DA")

            Text(
                text = when (reservation.status) {
                    "accepted" -> "Statut : acceptée"
                    "refused" -> "Statut : refusée"
                    "cancelled" -> "Statut : annulée"
                    else -> "Statut : ${reservation.status}"
                },
                color = when (reservation.status) {
                    "accepted" -> Color(0xFF13A10E)
                    "refused" -> Color(0xFFD13438)
                    "cancelled" -> Color.Gray
                    else -> Color(0xFF1DA1F2)
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AgencyAddCarTab(
    brands: List<Brand>,
    models: List<CarModel>,
    agencyId: String,
    agencyName: String,
    onCarAdded: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var selectedBrandName by remember { mutableStateOf("") }
    var selectedBrandId by remember { mutableStateOf("") }

    var selectedModelName by remember { mutableStateOf("") }
    var selectedModelId by remember { mutableStateOf("") }

    var selectedType by remember { mutableStateOf("") }
    var selectedWilaya by remember { mutableStateOf("") }
    var selectedFuel by remember { mutableStateOf("") }
    var selectedGearbox by remember { mutableStateOf("") }

    var year by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    val filteredModels = models.filter { it.brandId == selectedBrandId }

    fun createCar(imageUrl: String) {
        val data = hashMapOf(
            "agencyId" to agencyId,
            "agencyName" to agencyName,
            "brandId" to selectedBrandId,
            "brandName" to selectedBrandName,
            "modelId" to selectedModelId,
            "modelName" to selectedModelName,
            "year" to (year.toIntOrNull() ?: 2020),
            "city" to selectedWilaya,
            "type" to selectedType,
            "fuel" to selectedFuel,
            "gearbox" to selectedGearbox,
            "pricePerDay" to (price.toDoubleOrNull() ?: 0.0),
            "mileage" to (mileage.toIntOrNull() ?: 0),
            "imageUrl" to imageUrl,
            "imageUrls" to listOf(imageUrl).filter { it.isNotBlank() },
            "available" to true,
            "status" to "available",
            "ratingAverage" to 0.0,
            "totalReviews" to 0L,
            "previousRentals" to 0L
        )

        db.collection("cars")
            .add(data)
            .addOnSuccessListener {
                loading = false
                onCarAdded()
            }
            .addOnFailureListener {
                loading = false
                message = "Erreur ajout voiture : ${it.message}"
            }
    }

    fun uploadImageThenCreateCar() {
        val uri = selectedImageUri

        if (uri == null) {
            createCar("")
            return
        }

        val ref = storage.reference
            .child("car_images")
            .child(agencyId)
            .child("${System.currentTimeMillis()}.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        createCar(downloadUri.toString())
                    }
                    .addOnFailureListener {
                        loading = false
                        message = "Erreur récupération photo : ${it.message}"
                    }
            }
            .addOnFailureListener {
                loading = false
                message = "Erreur upload photo : ${it.message}"
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AgencyHeader(
                title = "Ajouter une voiture",
                subtitle = "Publier un véhicule dans votre flotte",
                imageUrl = ""
            )
        }

        if (message.isNotEmpty()) {
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
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
                    AppDropdown(
                        label = "Marque",
                        value = selectedBrandName,
                        items = brands.map { it.name },
                        onItemSelected = { name ->
                            selectedBrandName = name

                            val brand = brands.firstOrNull { it.name == name }

                            selectedBrandId = brand?.id ?: ""
                            selectedModelName = ""
                            selectedModelId = ""
                            selectedType = ""
                        }
                    )

                    AppDropdown(
                        label = "Modèle",
                        value = selectedModelName,
                        items = filteredModels.map { it.name },
                        onItemSelected = { name ->
                            selectedModelName = name

                            val model = filteredModels.firstOrNull { it.name == name }

                            selectedModelId = model?.id ?: ""
                            selectedType = model?.type ?: ""
                        }
                    )

                    AppDropdown(
                        label = "Type",
                        value = selectedType,
                        items = AppOptions.carTypes,
                        onItemSelected = { selectedType = it }
                    )

                    AppDropdown(
                        label = "Wilaya",
                        value = selectedWilaya,
                        items = AppOptions.wilayas,
                        onItemSelected = { selectedWilaya = it }
                    )

                    AppDropdown(
                        label = "Carburant",
                        value = selectedFuel,
                        items = AppOptions.fuels,
                        onItemSelected = { selectedFuel = it }
                    )

                    AppDropdown(
                        label = "Boîte de vitesse",
                        value = selectedGearbox,
                        items = AppOptions.gearboxes,
                        onItemSelected = { selectedGearbox = it }
                    )

                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Année") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Prix par jour en DA") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = mileage,
                        onValueChange = { mileage = it },
                        label = { Text("Kilométrage") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Choisir une photo du véhicule")
                    }

                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Photo véhicule",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Button(
                        onClick = {
                            if (
                                selectedBrandId.isBlank() ||
                                selectedModelId.isBlank() ||
                                selectedWilaya.isBlank() ||
                                selectedFuel.isBlank() ||
                                selectedGearbox.isBlank() ||
                                year.isBlank() ||
                                price.isBlank()
                            ) {
                                message = "Veuillez remplir les champs obligatoires."
                                return@Button
                            }

                            loading = true
                            message = ""
                            uploadImageThenCreateCar()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1DA1F2)
                        )
                    ) {
                        Text(if (loading) "Ajout..." else "Ajouter la voiture")
                    }
                }
            }
        }
    }
}

@Composable
fun AgencyFleetTab(
    cars: List<Car>,
    agents: List<MaintenanceAgent>,
    reservations: List<AgencyReservationUi>,
    onOpenPlanning: (Car) -> Unit,
    onEditCar: (Car) -> Unit,
    onDeleteCar: (Car) -> Unit,
    onMakeAvailable: (Car) -> Unit,
    onAssignMaintenance: (Car, MaintenanceAgent) -> Unit
) {
    val selectedAgents = remember { mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AgencyHeader(
                title = "Ma flotte",
                subtitle = "Gestion des véhicules et prédiction IA",
                imageUrl = ""
            )
        }

        if (cars.isEmpty()) {
            item {
                AgencyEmptyCard("Aucune voiture ajoutée.")
            }
        } else {
            items(cars) { car ->
                val acceptedCount = reservations.count {
                    it.carId == car.id && it.status == "accepted"
                }

                val refusedCount = reservations.count {
                    it.carId == car.id && it.status == "refused"
                }

                val prediction = PredictionEngine.predictCarDemand(
                    previousRentals = car.previousRentals.toLong(),
                    pricePerDay = car.pricePerDay,
                    ratingAverage = car.ratingAverage,
                    availableDays = 25,
                    mileage = car.mileage,
                    year = car.year,
                    acceptedReservations = acceptedCount,
                    refusedReservations = refusedCount,
                    city = car.city,
                    fuel = car.fuel,
                    gearbox = car.gearbox,
                    carType = car.type
                )

                val selectedAgentName = selectedAgents[car.id] ?: ""

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPlanning(car) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (car.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = car.imageUrl,
                                contentDescription = "Image voiture",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(18.dp))
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
                            text = "${car.brandName} ${car.modelName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )



                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Wilaya : ${car.city}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "Prix : ${car.pricePerDay} DA / jour",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "Kilométrage : ${car.mileage} km",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "Année : ${car.year}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "Statut : ${car.status}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "Disponible : ${if (car.available) "Oui" else "Non"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }


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

                        if (agents.isNotEmpty()) {
                            AppDropdown(
                                label = "Choisir agent entretien / lavage",
                                value = selectedAgentName,
                                items = agents.map { "${it.firstName} ${it.lastName}" },
                                onItemSelected = {
                                    selectedAgents[car.id] = it
                                }
                            )

                            Button(
                                onClick = {
                                    val agent = agents.firstOrNull {
                                        "${it.firstName} ${it.lastName}" == selectedAgents[car.id]
                                    }

                                    if (agent != null) {
                                        onAssignMaintenance(car, agent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1DA1F2)
                                )
                            ) {
                                Text("Assigner entretien / lavage")
                            }
                        } else {
                            Text(
                                text = "Ajoutez un agent pour pouvoir assigner un entretien.",
                                color = Color.Gray
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { onEditCar(car) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Modifier")
                            }

                            OutlinedButton(
                                onClick = { onMakeAvailable(car) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(11.dp)
                            ) {
                                Text("Disponible")
                            }
                        }

                        OutlinedButton(
                            onClick = { onDeleteCar(car) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Text("Supprimer")
                        }

                        Divider()

                        AiPredictionCard(prediction = prediction)
                    }
                }
            }
        }
    }
}


fun todayStartMillisAgency(): Long {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

@Composable
fun CarPlanningDialog(
    car: Car,
    reservations: List<AgencyReservationUi>,
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val today = todayStartMillisAgency()

    var localMessage by remember { mutableStateOf("") }
    var selectedReservation by remember { mutableStateOf<AgencyReservationUi?>(null) }
    var reservationToEdit by remember { mutableStateOf<AgencyReservationUi?>(null) }
    var reservationToDelete by remember { mutableStateOf<AgencyReservationUi?>(null) }

    val activeReservations = reservations
        .filter {
            it.carId == car.id &&
                    it.status == "accepted" &&
                    it.endDateMillis >= today
        }
        .sortedBy { it.startDateMillis }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Programme : ${car.brandName} ${car.modelName}")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 460.dp)
            ) {
                item {
                    Text(
                        text = "Cliquez sur une ligne de réservation pour voir les détails.",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1DA1F2)
                    )
                }

                if (localMessage.isNotBlank()) {
                    item {
                        Text(
                            text = localMessage,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (activeReservations.isEmpty()) {
                    item {
                        Text(
                            text = "Aucune réservation active ou prochaine pour cette voiture.",
                            color = Color.Gray
                        )
                    }
                } else {
                    items(activeReservations) { reservation ->
                        val isNowReserved =
                            today >= reservation.startDateMillis &&
                                    today <= reservation.endDateMillis

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReservation = reservation },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6F8))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileCircle(
                                    text = reservation.clientName.take(1).ifBlank { "C" },
                                    imageUrl = reservation.clientProfileImageUrl
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = reservation.clientName.ifBlank { "Client" },
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text("Du ${reservation.startDateText} au ${reservation.endDateText}")

                                    Text(
                                        text = if (isNowReserved) {
                                            "Réservée maintenant"
                                        } else {
                                            "Réservation prochaine"
                                        },
                                        color = if (isNowReserved) Color(0xFFD13438) else Color(0xFF1DA1F2),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "Voir",
                                    color = Color(0xFF1DA1F2),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )

    selectedReservation?.let { reservation ->
        ReservationDetailsDialog(
            reservation = reservation,
            today = today,
            onDismiss = { selectedReservation = null },
            onEdit = {
                selectedReservation = null
                reservationToEdit = reservation
            },
            onDelete = {
                selectedReservation = null
                reservationToDelete = reservation
            }
        )
    }

    reservationToDelete?.let { reservation ->
        AlertDialog(
            onDismissRequest = { reservationToDelete = null },
            title = { Text("Supprimer la réservation") },
            text = {
                Text(
                    "Voulez-vous vraiment supprimer la réservation de ${reservation.clientName.ifBlank { "ce client" }} du ${reservation.startDateText} au ${reservation.endDateText} ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("reservations")
                            .document(reservation.id)
                            .delete()
                            .addOnSuccessListener {
                                reservationToDelete = null
                                localMessage = "Réservation supprimée. La période est maintenant libre."
                                onChanged("Réservation supprimée.")
                            }
                            .addOnFailureListener {
                                localMessage = "Erreur suppression : ${it.message}"
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { reservationToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    reservationToEdit?.let { reservation ->
        EditReservationDatesDialog(
            reservation = reservation,
            car = car,
            allReservations = reservations,
            onDismiss = { reservationToEdit = null },
            onSaved = {
                reservationToEdit = null
                localMessage = "Dates de réservation modifiées."
                onChanged("Dates de réservation modifiées.")
            }
        )
    }
}

@Composable
fun ReservationDetailsDialog(
    reservation: AgencyReservationUi,
    today: Long,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isNowReserved = today >= reservation.startDateMillis && today <= reservation.endDateMillis

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Détails de la réservation") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileCircle(
                            text = reservation.clientName.take(1).ifBlank { "C" },
                            imageUrl = reservation.clientProfileImageUrl
                        )

                        Column {
                            Text(
                                text = reservation.clientName.ifBlank { "Client" },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Client")
                        }
                    }
                }

                item { Text("Téléphone : ${reservation.clientPhone.ifBlank { "Non précisé" }}") }
                item { Text("Email : ${reservation.clientEmail.ifBlank { "Non précisé" }}") }
                item { Text("Voiture : ${reservation.carName.ifBlank { "Voiture" }}") }
                item { Text("Date départ : ${reservation.startDateText}") }
                item { Text("Date fin : ${reservation.endDateText}") }
                item { Text("Durée : ${reservation.totalDays} jour(s)") }
                item { Text("Total : ${reservation.totalPrice} DA") }

                item {
                    Text(
                        text = if (isNowReserved) "État : réservée maintenant" else "État : réservation prochaine",
                        color = if (isNowReserved) Color(0xFFD13438) else Color(0xFF1DA1F2),
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Text(
                        text = "Choisissez une action : modifier les dates, supprimer la réservation, ou annuler pour revenir sans changement.",
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
            ) {
                Text("Modifier")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDelete) {
                    Text("Supprimer")
                }

                OutlinedButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )
}

@Composable
fun EditReservationDatesDialog(
    reservation: AgencyReservationUi,
    car: Car,
    allReservations: List<AgencyReservationUi>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var startDateText by remember { mutableStateOf(reservation.startDateText) }
    var endDateText by remember { mutableStateOf(reservation.endDateText) }
    var startMillis by remember { mutableStateOf(reservation.startDateMillis) }
    var endMillis by remember { mutableStateOf(reservation.endDateMillis) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val totalDays = calculateAgencyReservationDays(startMillis, endMillis)
    val totalPrice = totalDays * car.pricePerDay

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier les dates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = reservation.clientName.ifBlank { "Client" },
                    fontWeight = FontWeight.Bold
                )
                Text("Voiture : ${car.brandName} ${car.modelName}")

                AgencyDateButton(
                    label = "Date début",
                    value = startDateText,
                    onDateSelected = { text, millis ->
                        startDateText = text
                        startMillis = millis
                        error = ""
                    }
                )

                AgencyDateButton(
                    label = "Date fin",
                    value = endDateText,
                    onDateSelected = { text, millis ->
                        endDateText = text
                        endMillis = millis
                        error = ""
                    }
                )

                if (totalDays > 0) {
                    Text("Nouvelle durée : $totalDays jour(s)")
                    Text("Nouveau total : $totalPrice DA")
                }

                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (startMillis <= 0L || endMillis <= 0L) {
                        error = "Veuillez choisir les deux dates."
                        return@Button
                    }

                    if (endMillis < startMillis) {
                        error = "La date fin doit être après la date début."
                        return@Button
                    }

                    if (totalDays <= 0) {
                        error = "Durée invalide."
                        return@Button
                    }

                    val hasConflict = allReservations.any { other ->
                        other.id != reservation.id &&
                                other.carId == reservation.carId &&
                                other.status == "accepted" &&
                                dateRangesOverlapAgency(
                                    startMillis,
                                    endMillis,
                                    other.startDateMillis,
                                    other.endDateMillis
                                )
                    }

                    if (hasConflict) {
                        error = "Cette voiture est déjà réservée dans cette nouvelle période."
                        return@Button
                    }

                    loading = true
                    error = ""

                    db.collection("reservations")
                        .document(reservation.id)
                        .update(
                            mapOf(
                                "startDateMillis" to startMillis,
                                "endDateMillis" to endMillis,
                                "startDateText" to startDateText,
                                "endDateText" to endDateText,
                                "totalDays" to totalDays,
                                "totalPrice" to totalPrice,
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                        .addOnSuccessListener {
                            loading = false
                            onSaved()
                        }
                        .addOnFailureListener {
                            loading = false
                            error = "Erreur modification : ${it.message}"
                        }
                },
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
            ) {
                Text(if (loading) "Modification..." else "Enregistrer")
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
fun AgencyDateButton(
    label: String,
    value: String,
    onDateSelected: (String, Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, day, 0, 0, 0)
                    selectedCalendar.set(Calendar.MILLISECOND, 0)

                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

                    onDateSelected(
                        formatter.format(selectedCalendar.time),
                        selectedCalendar.timeInMillis
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(if (value.isBlank()) "$label 📅" else "$label : $value")
    }
}

fun calculateAgencyReservationDays(
    startMillis: Long,
    endMillis: Long
): Int {
    if (startMillis <= 0L || endMillis <= 0L) return 0
    if (endMillis < startMillis) return 0

    val oneDay = 24 * 60 * 60 * 1000L
    return (((endMillis - startMillis) / oneDay) + 1).toInt()
}

fun dateRangesOverlapAgency(
    start1: Long,
    end1: Long,
    start2: Long,
    end2: Long
): Boolean {
    return start1 <= end2 && start2 <= end1
}

@Composable
fun AiPredictionCard(
    prediction: PredictionResult
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF6E8FF)
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✦",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF9C27B0),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Prédiction IA",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9C27B0),
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Environ ${prediction.rentals} location(s) le mois prochain.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Demande : ${prediction.demandLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7B1FA2),
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Fiabilité : ${prediction.confidence}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )

            Text(
                text = prediction.explanation,
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
            )

            Text(
                text = "Conseil : ${prediction.advice}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
fun AgencyAgentsTab(
    agencyId: String,
    agents: List<MaintenanceAgent>,
    onAgentAdded: () -> Unit,
    onEditAgent: (MaintenanceAgent) -> Unit,
    onDeleteAgent: (MaintenanceAgent) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }

    var message by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    fun saveAgent(imageUrl: String) {
        val data = hashMapOf(
            "agencyId" to agencyId,
            "firstName" to firstName.trim(),
            "lastName" to lastName.trim(),
            "phone" to phone.trim(),
            "location" to location.trim(),
            "available" to true,
            "profileImageUrl" to imageUrl
        )

        db.collection("maintenanceAgents")
            .add(data)
            .addOnSuccessListener {
                firstName = ""
                lastName = ""
                phone = ""
                location = ""
                selectedImageUri = null
                loading = false
                message = ""
                onAgentAdded()
            }
            .addOnFailureListener {
                loading = false
                message = "Erreur ajout agent : ${it.message}"
            }
    }

    fun uploadAgentImageThenSave() {
        val uri = selectedImageUri

        if (uri == null) {
            saveAgent("")
            return
        }

        val ref = storage.reference
            .child("maintenance_agents")
            .child(agencyId)
            .child("${System.currentTimeMillis()}.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        saveAgent(downloadUri.toString())
                    }
                    .addOnFailureListener {
                        loading = false
                        message = "Erreur récupération photo : ${it.message}"
                    }
            }
            .addOnFailureListener {
                loading = false
                message = "Erreur upload photo : ${it.message}"
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AgencyHeader(
                title = "Agents d’entretien",
                subtitle = "Maintenance, lavage et suivi véhicule",
                imageUrl = ""
            )
        }

        if (message.isNotEmpty()) {
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
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
                        text = "Ajouter un agent",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Prénom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Adresse / localisation") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Choisir une photo de l’agent")
                    }

                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Photo agent",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Button(
                        onClick = {
                            if (
                                firstName.isBlank() ||
                                lastName.isBlank() ||
                                phone.isBlank()
                            ) {
                                message = "Prénom, nom et téléphone sont obligatoires."
                                return@Button
                            }

                            loading = true
                            message = ""
                            uploadAgentImageThenSave()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1DA1F2)
                        )
                    ) {
                        Text(if (loading) "Ajout..." else "Ajouter l’agent")
                    }
                }
            }
        }

        item {
            Text(
                text = "Liste des agents",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (agents.isEmpty()) {
            item {
                AgencyEmptyCard("Aucun agent ajouté.")
            }
        } else {
            items(agents) { agent ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileCircle(
                                text = agent.firstName.take(1).ifBlank { "A" },
                                imageUrl = agent.profileImageUrl
                            )

                            Column {
                                Text(
                                    text = "${agent.firstName} ${agent.lastName}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Text(
                                    text = "Agent d’entretien",
                                    color = Color.Gray
                                )
                            }
                        }

                        Text("Téléphone : ${agent.phone}")
                        Text("Localisation : ${agent.location.ifBlank { "Non précisée" }}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onEditAgent(agent) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Modifier")
                            }

                            OutlinedButton(
                                onClick = { onDeleteAgent(agent) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Supprimer")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgencyProfileTab(
    agencyName: String,
    email: String,
    phone: String,
    city: String,
    agencyImageUrl: String,
    onEditProfile: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AgencyHeader(
            title = "Profil agence",
            subtitle = "Compte et paramètres",
            imageUrl = agencyImageUrl
        )

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
                    text = agencyName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = email,
                    color = Color.Gray
                )

                Text("Téléphone : ${phone.ifBlank { "Non précisé" }}")
                Text("Wilaya : ${city.ifBlank { "Non précisée" }}")
            }
        }

        Button(
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
        ) {
            Text("Modifier le profil")
        }

        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Actualiser")
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Se déconnecter")
        }
    }
}

@Composable
fun EditCarDialog(
    car: Car,
    brands: List<Brand>,
    models: List<CarModel>,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var selectedBrandName by remember { mutableStateOf(car.brandName) }
    var selectedBrandId by remember { mutableStateOf(car.brandId) }

    var selectedModelName by remember { mutableStateOf(car.modelName) }
    var selectedModelId by remember { mutableStateOf(car.modelId) }

    var selectedType by remember { mutableStateOf(car.type) }
    var selectedWilaya by remember { mutableStateOf(car.city) }
    var selectedFuel by remember { mutableStateOf(car.fuel) }
    var selectedGearbox by remember { mutableStateOf(car.gearbox) }

    var year by remember { mutableStateOf(car.year.toString()) }
    var price by remember { mutableStateOf(car.pricePerDay.toString()) }
    var mileage by remember { mutableStateOf(car.mileage.toString()) }
    var imageUrl by remember { mutableStateOf(car.imageUrl) }

    var error by remember { mutableStateOf("") }

    val filteredModels = models.filter { it.brandId == selectedBrandId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Modifier véhicule")
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AppDropdown(
                        label = "Marque",
                        value = selectedBrandName,
                        items = brands.map { it.name },
                        onItemSelected = { name ->
                            selectedBrandName = name

                            val brand = brands.firstOrNull { it.name == name }

                            selectedBrandId = brand?.id ?: ""
                            selectedModelName = ""
                            selectedModelId = ""
                        }
                    )
                }

                item {
                    AppDropdown(
                        label = "Modèle",
                        value = selectedModelName,
                        items = filteredModels.map { it.name },
                        onItemSelected = { name ->
                            selectedModelName = name

                            val model = filteredModels.firstOrNull { it.name == name }

                            selectedModelId = model?.id ?: ""
                            selectedType = model?.type ?: selectedType
                        }
                    )
                }

                item {
                    AppDropdown(
                        label = "Type",
                        value = selectedType,
                        items = AppOptions.carTypes,
                        onItemSelected = { selectedType = it }
                    )
                }

                item {
                    AppDropdown(
                        label = "Wilaya",
                        value = selectedWilaya,
                        items = AppOptions.wilayas,
                        onItemSelected = { selectedWilaya = it }
                    )
                }

                item {
                    AppDropdown(
                        label = "Carburant",
                        value = selectedFuel,
                        items = AppOptions.fuels,
                        onItemSelected = { selectedFuel = it }
                    )
                }

                item {
                    AppDropdown(
                        label = "Boîte",
                        value = selectedGearbox,
                        items = AppOptions.gearboxes,
                        onItemSelected = { selectedGearbox = it }
                    )
                }

                item {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Année") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Prix / jour") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = mileage,
                        onValueChange = { mileage = it },
                        label = { Text("Kilométrage") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Lien image") },
                        modifier = Modifier.fillMaxWidth()
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
                    val parsedYear = year.toIntOrNull()
                    val parsedPrice = price.toDoubleOrNull()
                    val parsedMileage = mileage.toIntOrNull()

                    if (parsedYear == null) {
                        error = "L’année doit être un nombre."
                        return@Button
                    }

                    if (parsedPrice == null) {
                        error = "Le prix doit être un nombre."
                        return@Button
                    }

                    if (parsedMileage == null) {
                        error = "Le kilométrage doit être un nombre."
                        return@Button
                    }

                    onSave(
                        mapOf(
                            "brandId" to selectedBrandId,
                            "brandName" to selectedBrandName,
                            "modelId" to selectedModelId,
                            "modelName" to selectedModelName,
                            "type" to selectedType,
                            "city" to selectedWilaya,
                            "fuel" to selectedFuel,
                            "gearbox" to selectedGearbox,
                            "year" to parsedYear,
                            "pricePerDay" to parsedPrice,
                            "mileage" to parsedMileage,
                            "imageUrl" to imageUrl,
                            "imageUrls" to listOf(imageUrl).filter { it.isNotBlank() }
                        )
                    )
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
fun EditAgentDialog(
    agent: MaintenanceAgent,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var firstName by remember { mutableStateOf(agent.firstName) }
    var lastName by remember { mutableStateOf(agent.lastName) }
    var phone by remember { mutableStateOf(agent.phone) }
    var location by remember { mutableStateOf(agent.location) }
    var imageUrl by remember { mutableStateOf(agent.profileImageUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Modifier agent")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Localisation") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Lien photo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        mapOf(
                            "firstName" to firstName.trim(),
                            "lastName" to lastName.trim(),
                            "phone" to phone.trim(),
                            "location" to location.trim(),
                            "profileImageUrl" to imageUrl.trim()
                        )
                    )
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
fun AgencyHeader(
    title: String,
    subtitle: String,
    imageUrl: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
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

        ProfileCircle(
            text = title.take(1),
            imageUrl = imageUrl
        )
    }
}

@Composable
fun ProfileCircle(
    text: String,
    imageUrl: String
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
                contentDescription = "Profil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = text.uppercase().ifBlank { "?" },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AgencyStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
        }
    }
}

@Composable
fun AgencyEmptyCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(18.dp),
            color = Color.Gray
        )
    }
}


@Composable
fun AgencyMessagesScreen(
    agencyId: String,
    agencyName: String,
    conversations: List<AgencyConversationUi>,
    selectedConversation: AgencyConversationUi?,
    onBack: () -> Unit,
    onSelectConversation: (AgencyConversationUi?) -> Unit,
    onRefresh: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var messages by remember(selectedConversation?.id) { mutableStateOf(listOf<AgencyChatMessageUi>()) }
    var text by remember(selectedConversation?.id) { mutableStateOf("") }
    var localMessage by remember { mutableStateOf("") }

    BackHandler {
        if (selectedConversation != null) {
            onSelectConversation(null)
        } else {
            onBack()
        }
    }

    fun loadMessages(conversation: AgencyConversationUi) {
        db.collection("conversations").document(conversation.id)
            .collection("messages")
            .get()
            .addOnSuccessListener { result ->
                messages = result.documents.map { doc ->
                    AgencyChatMessageUi(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderRole = doc.getString("senderRole") ?: "",
                        text = doc.getString("text") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.sortedBy { it.createdAt }
                db.collection("conversations").document(conversation.id).update("unreadForAgency", 0L)
            }
            .addOnFailureListener { localMessage = "Erreur chargement messages : ${it.message}" }
    }

    fun sendMessage(conversation: AgencyConversationUi) {
        val content = text.trim()
        if (content.isBlank()) return
        val now = System.currentTimeMillis()
        val convRef = db.collection("conversations").document(conversation.id)
        val msg = hashMapOf(
            "senderId" to agencyId,
            "senderRole" to "agency",
            "text" to content,
            "createdAt" to now
        )
        convRef.collection("messages").add(msg).addOnSuccessListener {
            convRef.update(
                mapOf(
                    "lastMessage" to content,
                    "updatedAt" to now,
                    "unreadForClient" to 1L,
                    "agencyName" to agencyName
                )
            )
            text = ""
            loadMessages(conversation)
            onRefresh()
        }.addOnFailureListener { localMessage = "Erreur envoi : ${it.message}" }
    }

    LaunchedEffect(selectedConversation?.id) {
        selectedConversation?.let { loadMessages(it) }
    }

    if (selectedConversation == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF4F6F8)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onBack, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))) { Text("← Retour") }
                    OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(16.dp)) { Text("Actualiser") }
                }
            }
            item { Text("Messagerie", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Discussions avec les clients", color = Color.Gray) }
            if (conversations.isEmpty()) {
                item { AgencyEmptyCard("Aucun message pour le moment.") }
            } else {
                items(conversations) { conversation ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectConversation(conversation) },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileCircle(text = conversation.clientName.take(1).ifBlank { "C" }, imageUrl = "")
                            Column(Modifier.weight(1f)) {
                                Text(conversation.clientName.ifBlank { "Client" }, fontWeight = FontWeight.Bold)
                                Text(conversation.carName.ifBlank { "Discussion générale" }, color = Color.Gray)
                                if (conversation.lastMessage.isNotBlank()) Text(conversation.lastMessage, color = Color.Gray)
                            }
                            if (conversation.unreadForAgency > 0) {
                                Box(Modifier.width(24.dp).height(24.dp).clip(CircleShape).background(Color(0xFFD13438)), contentAlignment = Alignment.Center) { Text(conversation.unreadForAgency.toString(), color = Color.White, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(Color(0xFFF4F6F8)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onSelectConversation(null) }, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))) { Text("← Retour") }
                Column { Text(selectedConversation.clientName, fontWeight = FontWeight.Bold); Text(selectedConversation.carName.ifBlank { "Discussion" }, color = Color.Gray) }
            }
            if (localMessage.isNotEmpty()) Text(localMessage, color = MaterialTheme.colorScheme.error)
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (messages.isEmpty()) item { AgencyEmptyCard("Aucun message dans cette discussion.") }
                items(messages) { msg ->
                    val mine = msg.senderRole == "agency"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (mine) Color(0xFF1DA1F2) else Color.White)) {
                            Text(msg.text, modifier = Modifier.padding(12.dp), color = if (mine) Color.White else Color.Black)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), label = { Text("Écrire un message") }, shape = RoundedCornerShape(18.dp))
                Button(onClick = { sendMessage(selectedConversation) }, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))) { Text("Envoyer") }
            }
        }
    }
}

@Composable
fun AgencyBottomBar(
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
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgencyBottomItem(
                key = "dashboard",
                label = "Accueil",
                icon = "⌂",
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            AgencyBottomItem(
                key = "add_car",
                label = "Ajouter",
                icon = "+",
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            AgencyBottomItem(
                key = "fleet",
                label = "Flotte",
                icon = "▣",
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            AgencyBottomItem(
                key = "agents",
                label = "Agents",
                icon = "●",
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            AgencyBottomItem(
                key = "profile",
                label = "Profil",
                icon = "☻",
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    }
}

@Composable
fun AgencyBottomItem(
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
            color = if (selected) Color(0xFF1DA1F2) else Color.Gray,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = label,
            color = if (selected) Color(0xFF1DA1F2) else Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}