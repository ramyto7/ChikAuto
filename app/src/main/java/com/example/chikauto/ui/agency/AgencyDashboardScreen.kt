package com.example.chikauto.ui.agency

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chikauto.ai.PredictionEngine
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

@Composable
fun AgencyDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val agencyId = auth.currentUser?.uid ?: ""

    var selectedTab by remember { mutableStateOf("dashboard") }

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

    var message by remember { mutableStateOf("") }

    var carToEdit by remember { mutableStateOf<Car?>(null) }
    var agentToEdit by remember { mutableStateOf<MaintenanceAgent?>(null) }
    var showEditProfile by remember { mutableStateOf(false) }

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

        db.collection("cars")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { result ->
                cars = result.documents.mapNotNull {
                    it.toObject(Car::class.java)?.copy(id = it.id)
                }
            }

        db.collection("maintenanceAgents")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { result ->
                agents = result.documents.mapNotNull {
                    it.toObject(MaintenanceAgent::class.java)?.copy(id = it.id)
                }
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
    }

    fun acceptReservation(reservation: AgencyReservationUi) {
        db.collection("reservations")
            .whereEqualTo("carId", reservation.carId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                val conflict = result.documents.any { doc ->
                    val s = doc.getLong("startDateMillis") ?: 0L
                    val e = doc.getLong("endDateMillis") ?: 0L
                    reservation.startDateMillis <= e && s <= reservation.endDateMillis
                }

                if (conflict) {
                    message = "Cette voiture est déjà réservée dans cette période."
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
                            message = "Réservation acceptée, mais erreur MAJ IA : ${it.message}"
                            loadData()
                        }
                    }
                    .addOnFailureListener {
                        message = "Erreur acceptation : ${it.message}"
                    }
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
                    onRefuse = { refuseReservation(it) }
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
    onRefuse: (AgencyReservationUi) -> Unit
) {
    val availableCars = cars.count { it.available && it.status == "available" }
    val maintenanceCars = cars.count { it.status == "maintenance" }
    val pendingRequests = reservations.count { it.status == "pending" }

    val sortedReservations = reservations.sortedByDescending {
        when (it.status) {
            "pending" -> 3
            "accepted" -> 2
            "refused" -> 1
            else -> 0
        }
    }

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
                    color = MaterialTheme.colorScheme.primary
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
                    value = reservations.count { it.status == "accepted" }.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Demandes de réservation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (sortedReservations.isEmpty()) {
            item {
                AgencyEmptyCard("Aucune demande de réservation pour le moment.")
            }
        } else {
            items(sortedReservations) { reservation ->
                val car = cars.firstOrNull { it.id == reservation.carId }

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

                                Text("Tél : ${reservation.clientPhone}")
                                Text("Email : ${reservation.clientEmail}")
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
                            text = when (reservation.status) {
                                "pending" -> "Statut : en attente"
                                "accepted" -> "Statut : acceptée"
                                "refused" -> "Statut : refusée"
                                "cancelled" -> "Statut : annulée"
                                else -> "Statut : ${reservation.status}"
                            },
                            color = when (reservation.status) {
                                "accepted" -> Color(0xFF13A10E)
                                "refused" -> Color(0xFFD13438)
                                else -> Color(0xFF1DA1F2)
                            },
                            fontWeight = FontWeight.Bold
                        )

                        if (reservation.status == "pending") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAccept(reservation) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Accepter")
                                }

                                OutlinedButton(
                                    onClick = { onRefuse(reservation) },
                                    modifier = Modifier.weight(1f)
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
            "totalReviews" to 0,
            "previousRentals" to 0
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
                        createCar("")
                    }
            }
            .addOnFailureListener {
                createCar("")
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
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
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
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Prix par jour en DA") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mileage,
                        onValueChange = { mileage = it },
                        label = { Text("Kilométrage") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
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
                subtitle = "Gestion des véhicules",
                imageUrl = ""
            )
        }

        if (cars.isEmpty()) {
            item {
                AgencyEmptyCard("Aucune voiture ajoutée.")
            }
        } else {
            items(cars) { car ->
                val prediction = PredictionEngine.predictCarDemand(
                    previousRentals = car.previousRentals,
                    pricePerDay = car.pricePerDay,
                    ratingAverage = car.ratingAverage,
                    availableDays = 25
                )

                val selectedAgentName = selectedAgents[car.id] ?: ""

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

                        Text("Wilaya : ${car.city}")
                        Text("Prix : ${car.pricePerDay} DA / jour")
                        Text("Kilométrage : ${car.mileage} km")
                        Text("Statut : ${car.status}")
                        Text("Disponible : ${if (car.available) "Oui" else "Non"}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text(car.type) }
                            )

                            AssistChip(
                                onClick = {},
                                label = { Text(car.fuel) }
                            )

                            AssistChip(
                                onClick = {},
                                label = { Text(car.gearbox) }
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Assigner entretien / lavage")
                            }
                        } else {
                            Text("Ajoutez un agent pour pouvoir assigner un entretien.")
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onEditCar(car) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Modifier")
                            }

                            OutlinedButton(
                                onClick = { onMakeAvailable(car) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disponible")
                            }
                        }

                        OutlinedButton(
                            onClick = { onDeleteCar(car) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Supprimer")
                        }

                        Divider()

                        Text(
                            text = "Prédiction IA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1DA1F2)
                        )

                        Text("Prévision : ${prediction.rentals} location(s) le mois prochain")
                        Text("Niveau de demande : ${prediction.demandLevel}")
                        Text("Conseil : ${prediction.advice}")
                    }
                }
            }
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

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }

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
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ajouter un agent",
                        style = MaterialTheme.typography.titleLarge
                    )

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Prénom") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Adresse / localisation") },
                        modifier = Modifier.fillMaxWidth()
                    )

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

                            val data = hashMapOf(
                                "agencyId" to agencyId,
                                "firstName" to firstName.trim(),
                                "lastName" to lastName.trim(),
                                "phone" to phone.trim(),
                                "location" to location.trim(),
                                "available" to true
                            )

                            db.collection("maintenanceAgents")
                                .add(data)
                                .addOnSuccessListener {
                                    firstName = ""
                                    lastName = ""
                                    phone = ""
                                    location = ""
                                    message = ""
                                    onAgentAdded()
                                }
                                .addOnFailureListener {
                                    message = "Erreur ajout agent : ${it.message}"
                                }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ajouter l’agent")
                    }
                }
            }
        }

        item {
            Text(
                text = "Liste des agents",
                style = MaterialTheme.typography.titleLarge
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
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${agent.firstName} ${agent.lastName}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text("Téléphone : ${agent.phone}")
                        Text("Localisation : ${agent.location}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onEditAgent(agent) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Modifier")
                            }

                            OutlinedButton(
                                onClick = { onDeleteAgent(agent) },
                                modifier = Modifier.weight(1f)
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
            colors = CardDefaults.cardColors(containerColor = Color.White)
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

                Text("Téléphone : $phone")
                Text("Wilaya : $city")
            }
        }

        Button(
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Modifier le profil")
        }

        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Actualiser")
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
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

    val filteredModels = models.filter { it.brandId == selectedBrandId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Modifier véhicule")
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Prix / jour") },
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
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Lien image") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
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
                            "year" to (year.toIntOrNull() ?: car.year),
                            "pricePerDay" to (price.toDoubleOrNull() ?: car.pricePerDay),
                            "mileage" to (mileage.toIntOrNull() ?: car.mileage),
                            "imageUrl" to imageUrl,
                            "imageUrls" to listOf(imageUrl).filter { it.isNotBlank() }
                        )
                    )
                }
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
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Localisation") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        mapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "phone" to phone,
                            "location" to location
                        )
                    )
                }
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
                text = text.uppercase(),
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(18.dp),
            color = Color.Gray
        )
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

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = { onTabSelected(key) }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = icon,
                    color = if (selected) Color(0xFF1DA1F2) else Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = label,
                    color = if (selected) Color(0xFF1DA1F2) else Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}