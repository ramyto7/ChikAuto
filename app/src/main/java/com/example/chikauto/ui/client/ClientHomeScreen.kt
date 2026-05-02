package com.example.chikauto.ui.client

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chikauto.data.model.Car
import com.example.chikauto.ui.components.EditProfileDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ClientReservationUi(
    val id: String = "",
    val carId: String = "",
    val agencyId: String = "",
    val carName: String = "",
    val agencyName: String = "",
    val startDateText: String = "",
    val endDateText: String = "",
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    val totalDays: Int = 0,
    val totalPrice: Double = 0.0,
    val status: String = "pending"
)

data class ClientFilterState(
    val maxPrice: String = "",
    val city: String = "",
    val fuel: String = "",
    val gearbox: String = "",
    val type: String = ""
)

@Composable
fun ClientHomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val clientId = auth.currentUser?.uid ?: ""

    var selectedTab by remember { mutableStateOf("home") }

    var fullName by remember { mutableStateOf("Client") }
    var email by remember { mutableStateOf(auth.currentUser?.email ?: "") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Algérie") }
    var profileImageUrl by remember { mutableStateOf("") }

    var cars by remember { mutableStateOf(listOf<Car>()) }
    var reservations by remember { mutableStateOf(listOf<ClientReservationUi>()) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }

    var search by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var selectedCarForReservation by remember { mutableStateOf<Car?>(null) }
    var reservationForReview by remember { mutableStateOf<ClientReservationUi?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }

    var filters by remember { mutableStateOf(ClientFilterState()) }

    fun loadData() {
        if (clientId.isBlank()) return

        db.collection("users").document(clientId).get()
            .addOnSuccessListener { doc ->
                fullName = doc.getString("fullName") ?: "Client"
                email = doc.getString("email") ?: auth.currentUser?.email.orEmpty()
                phone = doc.getString("phone") ?: ""
                city = doc.getString("city") ?: "Algérie"
                profileImageUrl = doc.getString("profileImageUrl") ?: ""
            }

        db.collection("cars").get()
            .addOnSuccessListener { result ->
                cars = result.documents.mapNotNull { doc ->
                    doc.toObject(Car::class.java)?.copy(id = doc.id)
                }
            }
            .addOnFailureListener { message = "Erreur chargement voitures : ${it.message}" }

        db.collection("favorites").whereEqualTo("clientId", clientId).get()
            .addOnSuccessListener { result ->
                favoriteIds = result.documents.mapNotNull { it.getString("carId") }.toSet()
            }

        db.collection("reservations").whereEqualTo("clientId", clientId).get()
            .addOnSuccessListener { result ->
                reservations = result.documents.map { doc ->
                    ClientReservationUi(
                        id = doc.id,
                        carId = doc.getString("carId") ?: "",
                        agencyId = doc.getString("agencyId") ?: "",
                        carName = doc.getString("carName") ?: "",
                        agencyName = doc.getString("agencyName") ?: "",
                        startDateText = doc.getString("startDateText") ?: "",
                        endDateText = doc.getString("endDateText") ?: "",
                        startDateMillis = doc.getLong("startDateMillis") ?: 0L,
                        endDateMillis = doc.getLong("endDateMillis") ?: 0L,
                        totalDays = (doc.getLong("totalDays") ?: 0L).toInt(),
                        totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                        status = doc.getString("status") ?: "pending"
                    )
                }
            }
    }

    fun toggleFavorite(car: Car) {
        if (favoriteIds.contains(car.id)) {
            db.collection("favorites")
                .whereEqualTo("clientId", clientId)
                .whereEqualTo("carId", car.id)
                .get()
                .addOnSuccessListener { result ->
                    result.documents.forEach { it.reference.delete() }
                    favoriteIds = favoriteIds - car.id
                    message = "Voiture retirée des favoris."
                }
        } else {
            db.collection("favorites").add(hashMapOf("clientId" to clientId, "carId" to car.id))
                .addOnSuccessListener {
                    favoriteIds = favoriteIds + car.id
                    message = "Voiture ajoutée aux favoris."
                }
        }
    }

    fun createReservation(car: Car, startText: String, endText: String, startMillis: Long, endMillis: Long, days: Int) {
        if (clientId.isBlank()) {
            message = "Utilisateur non connecté."
            return
        }
        if (startMillis <= 0L || endMillis <= 0L || days <= 0) {
            message = "Dates invalides."
            return
        }

        db.collection("reservations")
            .whereEqualTo("carId", car.id)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                val conflict = result.documents.any { doc ->
                    val s = doc.getLong("startDateMillis") ?: 0L
                    val e = doc.getLong("endDateMillis") ?: 0L
                    dateRangesOverlap(startMillis, endMillis, s, e)
                }

                if (conflict) {
                    message = "Cette voiture est déjà réservée pendant cette période."
                    return@addOnSuccessListener
                }

                val reservation = hashMapOf(
                    "clientId" to clientId,
                    "clientName" to fullName,
                    "clientPhone" to phone,
                    "clientEmail" to email,
                    "clientProfileImageUrl" to profileImageUrl,
                    "agencyId" to car.agencyId,
                    "agencyName" to car.agencyName,
                    "carId" to car.id,
                    "carName" to "${car.brandName} ${car.modelName}",
                    "carImageUrl" to car.imageUrl,
                    "startDateMillis" to startMillis,
                    "endDateMillis" to endMillis,
                    "startDateText" to startText,
                    "endDateText" to endText,
                    "totalDays" to days,
                    "totalPrice" to car.pricePerDay * days,
                    "status" to "pending"
                )

                db.collection("reservations").add(reservation)
                    .addOnSuccessListener {
                        message = "Demande de réservation envoyée à l’agence."
                        selectedCarForReservation = null
                        loadData()
                    }
                    .addOnFailureListener { message = "Erreur réservation : ${it.message}" }
            }
    }

    fun cancelReservation(reservation: ClientReservationUi) {
        db.collection("reservations").document(reservation.id)
            .update("status", "cancelled")
            .addOnSuccessListener {
                message = "Réservation annulée."
                loadData()
            }
            .addOnFailureListener { message = "Erreur annulation : ${it.message}" }
    }

    fun addReview(reservation: ClientReservationUi, carRating: Int, agencyRating: Int, comment: String) {
        if (carRating !in 1..5 || agencyRating !in 1..5) {
            message = "Les notes doivent être entre 1 et 5."
            return
        }

        val review = hashMapOf(
            "clientId" to clientId,
            "clientName" to fullName,
            "agencyId" to reservation.agencyId,
            "carId" to reservation.carId,
            "reservationId" to reservation.id,
            "carRating" to carRating,
            "agencyRating" to agencyRating,
            "comment" to comment,
            "status" to "visible"
        )

        db.collection("reviews").add(review)
            .addOnSuccessListener {
                message = "Avis ajouté avec succès."
                reservationForReview = null
            }
            .addOnFailureListener { message = "Erreur avis : ${it.message}" }
    }

    LaunchedEffect(Unit) { loadData() }

    val pendingCount = reservations.count { it.status == "pending" }
    val acceptedCount = reservations.count { it.status == "accepted" }
    val refusedCount = reservations.count { it.status == "refused" }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F6F8))) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 78.dp)) {
            when (selectedTab) {
                "home" -> ClientHomeTab(
                    fullName = fullName,
                    city = city,
                    cars = cars.filter { it.available && it.status == "available" },
                    search = search,
                    filters = filters,
                    onSearchChange = { search = it },
                    favoriteIds = favoriteIds,
                    message = message,
                    onFilterClick = { showFilterDialog = true },
                    onToggleFavorite = { toggleFavorite(it) },
                    onReserveClick = { selectedCarForReservation = it }
                )
                "favorites" -> ClientFavoritesTab(
                    cars = cars.filter { favoriteIds.contains(it.id) },
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { toggleFavorite(it) },
                    onReserveClick = { selectedCarForReservation = it }
                )
                "reservations" -> ClientReservationsTab(
                    reservations = reservations,
                    cars = cars,
                    pendingCount = pendingCount,
                    acceptedCount = acceptedCount,
                    refusedCount = refusedCount,
                    onCancelReservation = { cancelReservation(it) },
                    onReviewReservation = { reservationForReview = it }
                )
                "profile" -> ClientProfileTab(
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    city = city,
                    profileImageUrl = profileImageUrl,
                    onEditProfile = { showEditProfile = true },
                    onRefresh = { loadData() },
                    onLogout = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("client_home") { inclusive = true }
                        }
                    }
                )
            }
        }

        ClientBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    selectedCarForReservation?.let { car ->
        ReservationDialog(
            car = car,
            onDismiss = { selectedCarForReservation = null },
            onConfirm = { startText, endText, startMillis, endMillis, days ->
                createReservation(car, startText, endText, startMillis, endMillis, days)
            }
        )
    }

    reservationForReview?.let { reservation ->
        ReviewDialog(
            reservation = reservation,
            onDismiss = { reservationForReview = null },
            onConfirm = { carRating, agencyRating, comment ->
                addReview(reservation, carRating, agencyRating, comment)
            }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            currentFilters = filters,
            onDismiss = { showFilterDialog = false },
            onApply = {
                filters = it
                showFilterDialog = false
            },
            onClear = {
                filters = ClientFilterState()
                showFilterDialog = false
            }
        )
    }

    if (showEditProfile) {
        EditProfileDialog(
            role = "client",
            currentName = fullName,
            currentEmail = email,
            currentPhone = phone,
            currentCity = city,
            currentImageUrl = profileImageUrl,
            onDismiss = { showEditProfile = false },
            onUpdated = {
                showEditProfile = false
                message = "Profil modifié avec succès."
                loadData()
            }
        )
    }
}

@Composable
fun ClientHomeTab(
    fullName: String,
    city: String,
    cars: List<Car>,
    search: String,
    filters: ClientFilterState,
    onSearchChange: (String) -> Unit,
    favoriteIds: Set<String>,
    message: String,
    onFilterClick: () -> Unit,
    onToggleFavorite: (Car) -> Unit,
    onReserveClick: (Car) -> Unit
) {
    val maxPrice = filters.maxPrice.toDoubleOrNull()

    val filteredCars = cars.filter { car ->
        val searchOk = search.isBlank()
                || car.brandName.contains(search, true)
                || car.modelName.contains(search, true)
                || car.city.contains(search, true)
                || car.type.contains(search, true)
                || car.fuel.contains(search, true)
                || car.gearbox.contains(search, true)
                || car.agencyName.contains(search, true)

        val priceOk = maxPrice == null || car.pricePerDay <= maxPrice
        val cityOk = filters.city.isBlank() || car.city.contains(filters.city, true)
        val fuelOk = filters.fuel.isBlank() || car.fuel.contains(filters.fuel, true)
        val gearboxOk = filters.gearbox.isBlank() || car.gearbox.contains(filters.gearbox, true)
        val typeOk = filters.type.isBlank() || car.type.contains(filters.type, true)

        searchOk && priceOk && cityOk && fuelOk && gearboxOk && typeOk
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { ClientTopHeader(city = city, fullName = fullName) }

        item {
            Text(
                text = "Trouvez la voiture\nqu’il vous faut ! 🚘",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    label = { Text("Marque, modèle, agence, wilaya...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )

                Button(
                    onClick = onFilterClick,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) { Text("Filtre") }
            }
        }

        item { SectionTitle(title = "Marques tendances", action = "Voir tout") }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val brands = cars.map { it.brandName }.filter { it.isNotBlank() }.distinct()
                    .ifEmpty { listOf("Audi", "BMW", "Mercedes", "Renault", "Peugeot") }

                items(brands) { brand ->
                    BrandSmallCard(brand = brand, onClick = { onSearchChange(brand) })
                }
            }
        }

        item { SectionTitle(title = "Voitures disponibles", action = "${filteredCars.size} résultats") }

        if (message.isNotEmpty()) {
            item { Text(text = message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }

        if (filteredCars.isEmpty()) {
            item { EmptyClientCard("Aucune voiture disponible pour cette recherche.") }
        } else {
            items(filteredCars) { car ->
                ClientModernCarCard(
                    car = car,
                    isFavorite = favoriteIds.contains(car.id),
                    onToggleFavorite = { onToggleFavorite(car) },
                    onReserve = { onReserveClick(car) }
                )
            }
        }

        item { SectionTitle(title = "Agences populaires", action = "Voir tout") }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val agencies = cars.map { it.agencyName.ifBlank { it.agencyId } }
                    .filter { it.isNotBlank() }.distinct().take(5).ifEmpty { listOf("Aucune agence") }

                items(agencies) { agency -> AgencySmallCard(agency) }
            }
        }
    }
}

@Composable
fun ClientTopHeader(city: String, fullName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Text(
                text = "☰",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Text(text = "📍 $city", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier.height(42.dp).width(42.dp).clip(CircleShape).background(Color(0xFF1DA1F2)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = fullName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionTitle(title: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = action, color = Color(0xFF1DA1F2), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BrandSmallCard(brand: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Card(
            modifier = Modifier.width(76.dp).height(66.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(5.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = brand.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1DA1F2)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(text = brand, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ClientModernCarCard(car: Car, isFavorite: Boolean, onToggleFavorite: () -> Unit, onReserve: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (car.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = car.imageUrl,
                    contentDescription = "Photo véhicule",
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(135.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFE9EEF3)),
                    contentAlignment = Alignment.Center
                ) { Text(text = "🚗", style = MaterialTheme.typography.displayMedium) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "${car.brandName} ${car.modelName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = car.agencyName.ifBlank { "Agence" }, color = Color.Gray)
                    Text(text = "${car.pricePerDay} DA / jour", color = Color.Gray)
                }
                Text(text = "⭐ ${car.ratingAverage}", fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(min = 36.dp)) {
                AssistChip(onClick = {}, label = { Text(car.city.ifBlank { "Wilaya" }) })
                AssistChip(onClick = {}, label = { Text(car.type.ifBlank { "Type" }) })
                AssistChip(onClick = {}, label = { Text(car.fuel.ifBlank { "Carburant" }) })
            }

            Text("Boîte : ${car.gearbox.ifBlank { "Non précisée" }}")
            Text("Kilométrage : ${car.mileage} km")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onReserve,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) { Text("Réserver") }

                OutlinedButton(onClick = onToggleFavorite, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text(if (isFavorite) "Retirer" else "Favori")
                }
            }
        }
    }
}

@Composable
fun ReservationDialog(
    car: Car,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Long, Int) -> Unit
) {
    var startDateText by remember { mutableStateOf("") }
    var endDateText by remember { mutableStateOf("") }
    var startMillis by remember { mutableStateOf(0L) }
    var endMillis by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf("") }

    val totalDays = calculateDays(startMillis, endMillis)
    val totalPrice = totalDays * car.pricePerDay

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Réserver ${car.brandName} ${car.modelName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Prix : ${car.pricePerDay} DA / jour")

                AppDateButton(
                    label = "Date début",
                    value = startDateText,
                    onDateSelected = { text, millis ->
                        startDateText = text
                        startMillis = millis
                    }
                )

                AppDateButton(
                    label = "Date fin",
                    value = endDateText,
                    onDateSelected = { text, millis ->
                        endDateText = text
                        endMillis = millis
                    }
                )

                if (totalDays > 0) {
                    Text("Durée : $totalDays jour(s)")
                    Text("Total : $totalPrice DA")
                }

                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (startMillis <= 0L || endMillis <= 0L) {
                    error = "Veuillez sélectionner les deux dates."
                    return@Button
                }
                if (endMillis < startMillis) {
                    error = "La date fin doit être après la date début."
                    return@Button
                }
                onConfirm(startDateText, endDateText, startMillis, endMillis, totalDays)
            }) { Text("Envoyer demande") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun AppDateButton(label: String, value: String, onDateSelected: (String, Long) -> Unit) {
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
                    onDateSelected(formatter.format(selectedCalendar.time), selectedCalendar.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) { Text(if (value.isBlank()) "$label 📅" else "$label : $value") }
}

@Composable
fun FilterDialog(
    currentFilters: ClientFilterState,
    onDismiss: () -> Unit,
    onApply: (ClientFilterState) -> Unit,
    onClear: () -> Unit
) {
    var maxPrice by remember { mutableStateOf(currentFilters.maxPrice) }
    var city by remember { mutableStateOf(currentFilters.city) }
    var fuel by remember { mutableStateOf(currentFilters.fuel) }
    var gearbox by remember { mutableStateOf(currentFilters.gearbox) }
    var type by remember { mutableStateOf(currentFilters.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrer la recherche") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = maxPrice, onValueChange = { maxPrice = it }, label = { Text("Prix maximum") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Wilaya") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fuel, onValueChange = { fuel = it }, label = { Text("Carburant : Diesel, Essence...") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = gearbox, onValueChange = { gearbox = it }, label = { Text("Boîte : Automatique, Manuelle") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type : SUV, Berline...") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onApply(ClientFilterState(maxPrice, city, fuel, gearbox, type)) }) {
                Text("Appliquer")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClear) { Text("Effacer") }
                OutlinedButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
}

@Composable
fun ClientFavoritesTab(
    cars: List<Car>,
    favoriteIds: Set<String>,
    onToggleFavorite: (Car) -> Unit,
    onReserveClick: (Car) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(text = "Voitures favorites", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

        if (cars.isEmpty()) {
            item { EmptyClientCard("Vous n’avez aucune voiture favorite.") }
        } else {
            items(cars) { car ->
                ClientModernCarCard(
                    car = car,
                    isFavorite = favoriteIds.contains(car.id),
                    onToggleFavorite = { onToggleFavorite(car) },
                    onReserve = { onReserveClick(car) }
                )
            }
        }
    }
}

@Composable
fun ClientReservationsTab(
    reservations: List<ClientReservationUi>,
    cars: List<Car>,
    pendingCount: Int,
    acceptedCount: Int,
    refusedCount: Int,
    onCancelReservation: (ClientReservationUi) -> Unit,
    onReviewReservation: (ClientReservationUi) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(text = "Mes réservations", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReservationMiniCard("En attente", pendingCount.toString(), Modifier.weight(1f))
                ReservationMiniCard("Validées", acceptedCount.toString(), Modifier.weight(1f))
                ReservationMiniCard("Refusées", refusedCount.toString(), Modifier.weight(1f))
            }
        }

        if (reservations.isEmpty()) {
            item { EmptyClientCard("Aucune réservation pour le moment.") }
        } else {
            items(reservations) { reservation ->
                val car = cars.firstOrNull { it.id == reservation.carId }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(
                            text = reservation.carName.ifBlank { car?.let { "${it.brandName} ${it.modelName}" } ?: "Voiture" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text("Agence : ${reservation.agencyName.ifBlank { "Agence" }}")
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
                                "finished" -> "Statut : terminée"
                                else -> "Statut : ${reservation.status}"
                            },
                            fontWeight = FontWeight.Bold,
                            color = when (reservation.status) {
                                "accepted" -> Color(0xFF13A10E)
                                "refused" -> Color(0xFFD13438)
                                "cancelled" -> Color.Gray
                                else -> Color(0xFF1DA1F2)
                            }
                        )

                        if (reservation.status == "pending") {
                            OutlinedButton(onClick = { onCancelReservation(reservation) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Annuler la demande")
                            }
                        }

                        if (reservation.status == "accepted" || reservation.status == "finished") {
                            Button(onClick = { onReviewReservation(reservation) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Noter la voiture et l’agence")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationMiniCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1DA1F2))
        }
    }
}

@Composable
fun ReviewDialog(
    reservation: ClientReservationUi,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, String) -> Unit
) {
    var carRating by remember { mutableStateOf("") }
    var agencyRating by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un avis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(reservation.carName.ifBlank { "Voiture" })
                OutlinedTextField(value = carRating, onValueChange = { carRating = it }, label = { Text("Note voiture /5") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = agencyRating, onValueChange = { agencyRating = it }, label = { Text("Note agence /5") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Commentaire") }, modifier = Modifier.fillMaxWidth())
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                val carNote = carRating.toIntOrNull() ?: 0
                val agencyNote = agencyRating.toIntOrNull() ?: 0
                if (carNote !in 1..5 || agencyNote !in 1..5) {
                    error = "Les notes doivent être entre 1 et 5."
                    return@Button
                }
                onConfirm(carNote, agencyNote, comment)
            }) { Text("Envoyer") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun ClientProfileTab(
    fullName: String,
    email: String,
    phone: String,
    city: String,
    profileImageUrl: String,
    onEditProfile: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(5.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.height(76.dp).width(76.dp).clip(CircleShape).background(Color(0xFF1DA1F2)),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl.isBlank()) {
                        Text(text = fullName.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    } else {
                        AsyncImage(model = profileImageUrl, contentDescription = "Photo profil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }

                Text(text = fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(email, color = Color.Gray)
                Text("Téléphone : $phone")
                Text("Wilaya : $city")
            }
        }

        Button(onClick = onEditProfile, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text("Modifier le profil")
        }

        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text("Actualiser")
        }

        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text("Se déconnecter")
        }
    }
}

@Composable
fun AgencySmallCard(name: String) {
    Card(
        modifier = Modifier.width(145.dp).height(90.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = name, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyClientCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(text = text, modifier = Modifier.padding(20.dp), color = Color.Gray)
    }
}

@Composable
fun ClientBottomBar(selectedTab: String, onTabSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(12.dp).height(62.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem("home", "Accueil", "⌂", selectedTab, onTabSelected)
            BottomItem("favorites", "Favoris", "♡", selectedTab, onTabSelected)
            BottomItem("reservations", "Réserv.", "▣", selectedTab, onTabSelected)
            BottomItem("profile", "Profil", "●", selectedTab, onTabSelected)
        }
    }
}

@Composable
fun BottomItem(key: String, label: String, icon: String, selectedTab: String, onTabSelected: (String) -> Unit) {
    val selected = selectedTab == key

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onTabSelected(key) }) {
        Text(text = icon, color = if (selected) Color(0xFF1DA1F2) else Color.LightGray, style = MaterialTheme.typography.titleLarge)
        Text(
            text = label,
            color = if (selected) Color(0xFF1DA1F2) else Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

fun calculateDays(startMillis: Long, endMillis: Long): Int {
    if (startMillis <= 0L || endMillis <= 0L) return 0
    if (endMillis < startMillis) return 0
    val oneDay = 24 * 60 * 60 * 1000L
    return (((endMillis - startMillis) / oneDay) + 1).toInt()
}

fun dateRangesOverlap(start1: Long, end1: Long, start2: Long, end2: Long): Boolean {
    return start1 <= end2 && start2 <= end1
}
