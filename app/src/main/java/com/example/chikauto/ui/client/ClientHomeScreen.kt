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
import com.example.chikauto.data.model.AppOptions
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
    val status: String = "pending",
    val reviewed: Boolean = false
)

data class ClientFilterState(
    val maxPrice: String = "",
    val city: String = "",
    val fuel: String = "",
    val gearbox: String = ""
)

data class ClientReviewUi(
    val id: String = "",
    val clientName: String = "",
    val carRating: Int = 0,
    val agencyRating: Int = 0,
    val comment: String = "",
    val createdAt: Long = 0L
)

data class ClientAgencyUi(
    val id: String = "",
    val agencyName: String = "",
    val ownerId: String = "",
    val city: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val status: String = "",
    val ratingAverage: Double = 0.0,
    val totalReviews: Long = 0L
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
    var selectedCity by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }

    var cars by remember { mutableStateOf(listOf<Car>()) }
    var agencies by remember { mutableStateOf(listOf<ClientAgencyUi>()) }
    var reservations by remember { mutableStateOf(listOf<ClientReservationUi>()) }
    var acceptedReservationsForAllCars by remember { mutableStateOf(listOf<ClientReservationUi>()) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }

    var search by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var selectedCarForReservation by remember { mutableStateOf<Car?>(null) }
    var selectedCarForDetails by remember { mutableStateOf<Car?>(null) }
    var reviewsForSelectedCar by remember { mutableStateOf(listOf<ClientReviewUi>()) }
    var reviewsLoading by remember { mutableStateOf(false) }

    var selectedAgencyForDetails by remember { mutableStateOf<ClientAgencyUi?>(null) }
    var showAllAgencies by remember { mutableStateOf(false) }
    var showAllBrands by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }

    var reservationForReview by remember { mutableStateOf<ClientReservationUi?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }

    var filters by remember { mutableStateOf(ClientFilterState()) }

    fun loadData() {
        if (clientId.isBlank()) return

        db.collection("users")
            .document(clientId)
            .get()
            .addOnSuccessListener { doc ->
                fullName = doc.getString("fullName") ?: "Client"
                email = doc.getString("email") ?: auth.currentUser?.email.orEmpty()
                phone = doc.getString("phone") ?: ""
                city = doc.getString("city") ?: "Algérie"
                profileImageUrl = doc.getString("profileImageUrl") ?: ""

                if (selectedCity.isBlank()) {
                    selectedCity = doc.getString("city") ?: "Algérie"
                }
            }

        db.collection("cars")
            .get()
            .addOnSuccessListener { result ->
                cars = result.documents.mapNotNull { doc ->
                    doc.toObject(Car::class.java)?.copy(id = doc.id)
                }
            }
            .addOnFailureListener {
                message = "Erreur chargement voitures : ${it.message}"
            }

        db.collection("agencies")
            .get()
            .addOnSuccessListener { result ->
                agencies = result.documents.map { doc ->
                    ClientAgencyUi(
                        id = doc.id,
                        agencyName = doc.getString("agencyName") ?: "Agence",
                        ownerId = doc.getString("ownerId") ?: "",
                        city = doc.getString("city") ?: "",
                        address = doc.getString("address") ?: "",
                        phone = doc.getString("phone") ?: "",
                        email = doc.getString("email") ?: "",
                        profileImageUrl = doc.getString("profileImageUrl") ?: "",
                        status = doc.getString("status") ?: "",
                        ratingAverage = doc.getDouble("ratingAverage") ?: 0.0,
                        totalReviews = doc.getLong("totalReviews") ?: 0L
                    )
                }
            }
            .addOnFailureListener {
                message = "Erreur chargement agences : ${it.message}"
            }

        db.collection("favorites")
            .whereEqualTo("clientId", clientId)
            .get()
            .addOnSuccessListener { result ->
                favoriteIds = result.documents.mapNotNull {
                    it.getString("carId")
                }.toSet()
            }

        db.collection("reservations")
            .whereEqualTo("clientId", clientId)
            .get()
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
                        status = doc.getString("status") ?: "pending",
                        reviewed = doc.getBoolean("reviewed") ?: false
                    )
                }
            }
            .addOnFailureListener {
                message = "Erreur chargement réservations : ${it.message}"
            }

        db.collection("reservations")
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                acceptedReservationsForAllCars = result.documents.map { doc ->
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
                        status = doc.getString("status") ?: "accepted",
                        reviewed = doc.getBoolean("reviewed") ?: false
                    )
                }
            }
            .addOnFailureListener {
                message = "Erreur chargement disponibilités : ${it.message}"
            }
    }

    fun openCarDetails(car: Car) {
        selectedCarForDetails = car
        reviewsForSelectedCar = emptyList()
        reviewsLoading = true

        db.collection("reviews")
            .whereEqualTo("carId", car.id)
            .whereEqualTo("status", "visible")
            .get()
            .addOnSuccessListener { result ->
                reviewsForSelectedCar = result.documents.map { doc ->
                    ClientReviewUi(
                        id = doc.id,
                        clientName = doc.getString("clientName") ?: "Client",
                        carRating = (doc.getLong("carRating") ?: 0L).toInt(),
                        agencyRating = (doc.getLong("agencyRating") ?: 0L).toInt(),
                        comment = doc.getString("comment") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }

                reviewsLoading = false
            }
            .addOnFailureListener {
                reviewsLoading = false
                message = "Erreur chargement avis : ${it.message}"
            }
    }

    fun toggleFavorite(car: Car) {
        if (clientId.isBlank()) return

        if (favoriteIds.contains(car.id)) {
            db.collection("favorites")
                .whereEqualTo("clientId", clientId)
                .whereEqualTo("carId", car.id)
                .get()
                .addOnSuccessListener { result ->
                    result.documents.forEach { it.reference.delete() }
                    favoriteIds = favoriteIds - car.id
                }
        } else {
            val favorite = hashMapOf(
                "clientId" to clientId,
                "carId" to car.id,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("favorites")
                .add(favorite)
                .addOnSuccessListener {
                    favoriteIds = favoriteIds + car.id
                }
        }
    }

    fun createReservation(
        car: Car,
        startText: String,
        endText: String,
        startMillis: Long,
        endMillis: Long,
        days: Int
    ) {
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
                    "status" to "pending",
                    "reviewed" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("reservations")
                    .add(reservation)
                    .addOnSuccessListener {
                        message = "Demande de réservation envoyée à l’agence."
                        selectedCarForReservation = null
                        loadData()
                    }
                    .addOnFailureListener {
                        message = "Erreur réservation : ${it.message}"
                    }
            }
            .addOnFailureListener {
                message = "Erreur vérification disponibilité : ${it.message}"
            }
    }

    fun cancelReservation(reservation: ClientReservationUi) {
        db.collection("reservations")
            .document(reservation.id)
            .update("status", "cancelled")
            .addOnSuccessListener {
                message = "Réservation annulée."
                loadData()
            }
            .addOnFailureListener {
                message = "Erreur annulation : ${it.message}"
            }
    }

    fun addReview(
        reservation: ClientReservationUi,
        carRating: Int,
        agencyRating: Int,
        comment: String
    ) {
        if (clientId.isBlank()) {
            message = "Utilisateur non connecté."
            return
        }

        if (carRating !in 1..5 || agencyRating !in 1..5) {
            message = "Les notes doivent être entre 1 et 5."
            return
        }

        if (reservation.reviewed) {
            message = "Vous avez déjà noté cette réservation."
            reservationForReview = null
            return
        }

        val reservationRef = db.collection("reservations").document(reservation.id)
        val carRef = db.collection("cars").document(reservation.carId)
        val agencyRef = db.collection("agencies").document(reservation.agencyId)

        db.collection("reviews")
            .whereEqualTo("reservationId", reservation.id)
            .whereEqualTo("clientId", clientId)
            .get()
            .addOnSuccessListener { existingReviews ->
                if (!existingReviews.isEmpty) {
                    message = "Vous avez déjà noté cette réservation."
                    reservationForReview = null
                    return@addOnSuccessListener
                }

                val reviewRef = db.collection("reviews").document()

                db.runTransaction { transaction ->
                    val carSnapshot = transaction.get(carRef)
                    val agencySnapshot = transaction.get(agencyRef)

                    val oldCarAverage = carSnapshot.getDouble("ratingAverage") ?: 0.0
                    val oldCarTotal = carSnapshot.getLong("totalReviews") ?: 0L

                    val oldAgencyAverage = agencySnapshot.getDouble("ratingAverage") ?: 0.0
                    val oldAgencyTotal = agencySnapshot.getLong("totalReviews") ?: 0L

                    val newCarTotal = oldCarTotal + 1
                    val newAgencyTotal = oldAgencyTotal + 1

                    val newCarAverage =
                        ((oldCarAverage * oldCarTotal) + carRating) / newCarTotal

                    val newAgencyAverage =
                        ((oldAgencyAverage * oldAgencyTotal) + agencyRating) / newAgencyTotal

                    val review = hashMapOf(
                        "id" to reviewRef.id,
                        "clientId" to clientId,
                        "clientName" to fullName,
                        "agencyId" to reservation.agencyId,
                        "agencyName" to reservation.agencyName,
                        "carId" to reservation.carId,
                        "carName" to reservation.carName,
                        "reservationId" to reservation.id,
                        "carRating" to carRating,
                        "agencyRating" to agencyRating,
                        "comment" to comment.trim(),
                        "status" to "visible",
                        "createdAt" to System.currentTimeMillis()
                    )

                    transaction.set(reviewRef, review)

                    transaction.update(
                        carRef,
                        mapOf(
                            "ratingAverage" to newCarAverage,
                            "totalReviews" to newCarTotal
                        )
                    )

                    transaction.update(
                        agencyRef,
                        mapOf(
                            "ratingAverage" to newAgencyAverage,
                            "totalReviews" to newAgencyTotal
                        )
                    )

                    transaction.update(
                        reservationRef,
                        mapOf(
                            "reviewed" to true,
                            "carRating" to carRating,
                            "agencyRating" to agencyRating,
                            "reviewComment" to comment.trim()
                        )
                    )
                }.addOnSuccessListener {
                    message = "Avis ajouté avec succès."
                    reservationForReview = null
                    loadData()
                }.addOnFailureListener { exception ->
                    message = "Erreur avis : ${exception.message}"
                }
            }
            .addOnFailureListener { exception ->
                message = "Erreur vérification avis : ${exception.message}"
            }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val visibleCity = selectedCity.ifBlank { city }

    val cityFilteredCars = cars.filter { car ->
        visibleCity.isBlank() ||
                visibleCity == "Algérie" ||
                car.city.equals(visibleCity, ignoreCase = true)
    }

    val cityFilteredAgencies = agencies.filter { agency ->
        visibleCity.isBlank() ||
                visibleCity == "Algérie" ||
                agency.city.equals(visibleCity, ignoreCase = true)
    }

    val pendingCount = reservations.count { it.status == "pending" }
    val acceptedCount = reservations.count { it.status == "accepted" }
    val refusedCount = reservations.count { it.status == "refused" }

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
                "home" -> ClientHomeTab(
                    fullName = fullName,
                    selectedCity = visibleCity,
                    profileImageUrl = profileImageUrl,
                    cars = cityFilteredCars.filter { car ->
                        car.available &&
                                car.status != "maintenance" &&
                                car.status != "disabled" &&
                                !isCarReservedToday(
                                    carId = car.id,
                                    acceptedReservations = acceptedReservationsForAllCars
                                )
                    },
                    agencies = cityFilteredAgencies,
                    search = search,
                    filters = filters,
                    onSearchChange = { search = it },
                    favoriteIds = favoriteIds,
                    message = message,
                    onFilterClick = { showFilterDialog = true },
                    onToggleFavorite = { toggleFavorite(it) },
                    onReserveClick = { selectedCarForReservation = it },
                    onCarClick = { openCarDetails(it) },
                    onProfileClick = { selectedTab = "profile" },
                    onCityClick = { showCityDialog = true },
                    onAllBrandsClick = { showAllBrands = true },
                    onAllAgenciesClick = { showAllAgencies = true },
                    onAgencyClick = { selectedAgencyForDetails = it }
                )

                "favorites" -> ClientFavoritesTab(
                    cars = cars.filter { car ->
                        favoriteIds.contains(car.id) &&
                                car.available &&
                                car.status != "maintenance" &&
                                car.status != "disabled" &&
                                !isCarReservedToday(
                                    carId = car.id,
                                    acceptedReservations = acceptedReservationsForAllCars
                                )
                    },
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { toggleFavorite(it) },
                    onReserveClick = { selectedCarForReservation = it },
                    onCarClick = { openCarDetails(it) }
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
                            popUpTo("client_home") {
                                inclusive = true
                            }
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

    selectedCarForDetails?.let { car ->
        CarDetailsDialog(
            car = car,
            reviews = reviewsForSelectedCar,
            loading = reviewsLoading,
            onDismiss = {
                selectedCarForDetails = null
                reviewsForSelectedCar = emptyList()
            },
            onReserve = {
                selectedCarForDetails = null
                selectedCarForReservation = car
            }
        )
    }

    selectedAgencyForDetails?.let { agency ->
        val fleet = cars.filter { car ->
            (car.agencyId == agency.id || car.agencyId == agency.ownerId) &&
                    car.available &&
                    car.status != "maintenance" &&
                    car.status != "disabled" &&
                    !isCarReservedToday(
                        carId = car.id,
                        acceptedReservations = acceptedReservationsForAllCars
                    )
        }

        AgencyDetailsDialog(
            agency = agency,
            fleet = fleet,
            favoriteIds = favoriteIds,
            onDismiss = { selectedAgencyForDetails = null },
            onToggleFavorite = { toggleFavorite(it) },
            onReserveCar = {
                selectedAgencyForDetails = null
                selectedCarForReservation = it
            },
            onOpenCarDetails = {
                selectedAgencyForDetails = null
                openCarDetails(it)
            }
        )
    }

    if (showAllAgencies) {
        AllAgenciesDialog(
            agencies = cityFilteredAgencies,
            onDismiss = { showAllAgencies = false },
            onAgencyClick = {
                showAllAgencies = false
                selectedAgencyForDetails = it
            }
        )
    }

    if (showAllBrands) {
        val brands = cityFilteredCars
            .map { it.brandName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        AllBrandsDialog(
            brands = brands,
            onDismiss = { showAllBrands = false },
            onBrandClick = {
                search = it
                showAllBrands = false
            }
        )
    }

    if (showCityDialog) {
        CityDialog(
            currentCity = visibleCity,
            onDismiss = { showCityDialog = false },
            onSelectCity = {
                selectedCity = it
                filters = filters.copy(city = it)
                showCityDialog = false
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
                if (it.city.isNotBlank()) selectedCity = it.city
                showFilterDialog = false
            },
            onClear = {
                filters = ClientFilterState()
                selectedCity = city
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
    selectedCity: String,
    profileImageUrl: String,
    cars: List<Car>,
    agencies: List<ClientAgencyUi>,
    search: String,
    filters: ClientFilterState,
    onSearchChange: (String) -> Unit,
    favoriteIds: Set<String>,
    message: String,
    onFilterClick: () -> Unit,
    onToggleFavorite: (Car) -> Unit,
    onReserveClick: (Car) -> Unit,
    onCarClick: (Car) -> Unit,
    onProfileClick: () -> Unit,
    onCityClick: () -> Unit,
    onAllBrandsClick: () -> Unit,
    onAllAgenciesClick: () -> Unit,
    onAgencyClick: (ClientAgencyUi) -> Unit
) {
    val maxPrice = filters.maxPrice.toDoubleOrNull()

    val filteredCars = cars.filter { car ->
        val searchOk = search.isBlank()
                || car.brandName.contains(search, true)
                || car.modelName.contains(search, true)
                || car.agencyName.contains(search, true)

        val priceOk = maxPrice == null || car.pricePerDay <= maxPrice
        val cityOk = filters.city.isBlank() || car.city.equals(filters.city, true)
        val fuelOk = filters.fuel.isBlank() || car.fuel.contains(filters.fuel, true)
        val gearboxOk = filters.gearbox.isBlank() || car.gearbox.contains(filters.gearbox, true)

        searchOk && priceOk && cityOk && fuelOk && gearboxOk
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ClientTopHeader(
                city = selectedCity,
                fullName = fullName,
                profileImageUrl = profileImageUrl,
                onProfileClick = onProfileClick,
                onCityClick = onCityClick
            )
        }

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
                    label = { Text("Marque, modèle, agence...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )

                Button(
                    onClick = onFilterClick,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DA1F2)
                    )
                ) {
                    Text("Filtre")
                }
            }
        }

        item {
            SectionTitle(
                title = "Marques tendances",
                action = "Voir tout",
                onActionClick = onAllBrandsClick
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val brands = cars
                    .map { it.brandName }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(8)
                    .ifEmpty {
                        listOf("Audi", "BMW", "Mercedes", "Renault", "Peugeot")
                    }

                items(brands) { brand ->
                    BrandSmallCard(
                        brand = brand,
                        onClick = { onSearchChange(brand) }
                    )
                }
            }
        }

        item {
            SectionTitle(
                title = "Voitures disponibles",
                action = "${filteredCars.size} résultats"
            )
        }

        if (message.isNotEmpty() && !message.contains("favori", ignoreCase = true)) {
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (filteredCars.isEmpty()) {
            item {
                EmptyClientCard("Aucune voiture disponible pour cette recherche.")
            }
        } else {
            items(filteredCars) { car ->
                ClientModernCarCard(
                    car = car,
                    isFavorite = favoriteIds.contains(car.id),
                    onToggleFavorite = { onToggleFavorite(car) },
                    onReserve = { onReserveClick(car) },
                    onOpenDetails = { onCarClick(car) }
                )
            }
        }

        item {
            SectionTitle(
                title = "Agences populaires",
                action = "Voir tout",
                onActionClick = onAllAgenciesClick
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val popularAgencies = agencies
                    .sortedByDescending { it.ratingAverage }
                    .take(6)

                if (popularAgencies.isEmpty()) {
                    item {
                        AgencySmallCard(
                            agency = ClientAgencyUi(agencyName = "Aucune agence"),
                            onClick = {}
                        )
                    }
                } else {
                    items(popularAgencies) { agency ->
                        AgencySmallCard(
                            agency = agency,
                            onClick = { onAgencyClick(agency) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClientTopHeader(
    city: String,
    fullName: String,
    profileImageUrl: String,
    onProfileClick: () -> Unit,
    onCityClick: () -> Unit
) {
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

        Text(
            text = "📍 ${city.ifBlank { "Algérie" }}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onCityClick() }
        )

        Box(
            modifier = Modifier
                .height(42.dp)
                .width(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF1DA1F2))
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "Photo profil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = fullName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    action: String,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = action,
            color = Color(0xFF1DA1F2),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = if (onActionClick != null) {
                Modifier.clickable { onActionClick() }
            } else {
                Modifier
            }
        )
    }
}

@Composable
fun BrandSmallCard(
    brand: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Card(
            modifier = Modifier
                .width(76.dp)
                .height(66.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(5.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = brand.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1DA1F2)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = brand,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ClientModernCarCard(
    car: Car,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onReserve: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (car.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = car.imageUrl,
                    contentDescription = "Photo véhicule",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${car.brandName} ${car.modelName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = car.agencyName.ifBlank { "Agence" },
                        color = Color.Gray
                    )

                    Text(
                        text = "${car.pricePerDay} DA / jour",
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "⭐ ${String.format(Locale.US, "%.1f", car.ratingAverage)}",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${car.totalReviews} avis",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(min = 36.dp)
            ) {
                AssistChip(onClick = {}, label = { Text(car.city.ifBlank { "Wilaya" }) })
                AssistChip(onClick = {}, label = { Text(car.fuel.ifBlank { "Carburant" }) })
                AssistChip(onClick = {}, label = { Text(car.gearbox.ifBlank { "Boîte" }) })
            }

            Text("Kilométrage : ${car.mileage} km")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onReserve,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DA1F2)
                    )
                ) {
                    Text("Réserver")
                }

                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isFavorite) "Retirer" else "Favori")
                }
            }
        }
    }
}

@Composable
fun CarDetailsDialog(
    car: Car,
    reviews: List<ClientReviewUi>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onReserve: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${car.brandName} ${car.modelName}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    if (car.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = car.imageUrl,
                            contentDescription = "Photo véhicule",
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
                            Text("🚗", style = MaterialTheme.typography.displayMedium)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6F8))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${car.brandName} ${car.modelName}", fontWeight = FontWeight.Bold)
                            Text("Agence : ${car.agencyName.ifBlank { "Agence" }}")
                            Text("Wilaya : ${car.city.ifBlank { "Non précisée" }}")
                            Text("Carburant : ${car.fuel.ifBlank { "Non précisé" }}")
                            Text("Boîte : ${car.gearbox.ifBlank { "Non précisée" }}")
                            Text("Kilométrage : ${car.mileage} km")
                            Text("Prix : ${car.pricePerDay} DA / jour")
                            Text(
                                text = "⭐ ${String.format(Locale.US, "%.1f", car.ratingAverage)} / 5",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1DA1F2)
                            )
                            Text("${car.totalReviews} avis")
                        }
                    }
                }

                item {
                    Text(
                        text = "Commentaires des clients",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (loading) {
                    item { Text("Chargement des avis...") }
                } else if (reviews.isEmpty()) {
                    item {
                        Text("Aucun commentaire pour cette voiture.", color = Color.Gray)
                    }
                } else {
                    items(reviews) { review ->
                        ReviewSmallCard(review)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onReserve,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
            ) {
                Text("Réserver")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun ReviewSmallCard(review: ClientReviewUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = review.clientName.ifBlank { "Client" },
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Voiture : ${starsText(review.carRating)}  •  Agence : ${starsText(review.agencyRating)}",
                color = Color(0xFF1DA1F2),
                fontWeight = FontWeight.Bold
            )

            if (review.comment.isNotBlank()) {
                Text(review.comment)
            } else {
                Text("Aucun commentaire écrit.", color = Color.Gray)
            }
        }
    }
}

@Composable
fun AgencySmallCard(
    agency: ClientAgencyUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(155.dp)
            .height(105.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .width(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1DA1F2)),
                contentAlignment = Alignment.Center
            ) {
                if (agency.profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = agency.profileImageUrl,
                        contentDescription = "Agence",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = agency.agencyName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = agency.agencyName,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = "⭐ ${String.format(Locale.US, "%.1f", agency.ratingAverage)}",
                color = Color(0xFF1DA1F2),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun AllAgenciesDialog(
    agencies: List<ClientAgencyUi>,
    onDismiss: () -> Unit,
    onAgencyClick: (ClientAgencyUi) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Toutes les agences") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (agencies.isEmpty()) {
                    item {
                        Text("Aucune agence trouvée dans cette ville.", color = Color.Gray)
                    }
                } else {
                    items(agencies.sortedByDescending { it.ratingAverage }) { agency ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAgencyClick(agency) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .width(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1DA1F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (agency.profileImageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = agency.profileImageUrl,
                                            contentDescription = "Agence",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            agency.agencyName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column {
                                    Text(agency.agencyName, fontWeight = FontWeight.Bold)
                                    Text("📍 ${agency.city.ifBlank { "Ville non précisée" }}")
                                    Text("⭐ ${String.format(Locale.US, "%.1f", agency.ratingAverage)} • ${agency.totalReviews} avis")
                                }
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
}

@Composable
fun AgencyDetailsDialog(
    agency: ClientAgencyUi,
    fleet: List<Car>,
    favoriteIds: Set<String>,
    onDismiss: () -> Unit,
    onToggleFavorite: (Car) -> Unit,
    onReserveCar: (Car) -> Unit,
    onOpenCarDetails: (Car) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(agency.agencyName) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6F8))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(70.dp)
                                    .width(70.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1DA1F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (agency.profileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = agency.profileImageUrl,
                                        contentDescription = "Agence",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        agency.agencyName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(agency.agencyName, fontWeight = FontWeight.Bold)
                            Text("Ville : ${agency.city.ifBlank { "Non précisée" }}")
                            Text("Adresse : ${agency.address.ifBlank { "Non précisée" }}")
                            Text("Téléphone : ${agency.phone.ifBlank { "Non précisé" }}")
                            Text("Email : ${agency.email.ifBlank { "Non précisé" }}")
                            Text(
                                text = "⭐ ${String.format(Locale.US, "%.1f", agency.ratingAverage)} / 5 • ${agency.totalReviews} avis",
                                color = Color(0xFF1DA1F2),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Flotte de véhicules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (fleet.isEmpty()) {
                    item {
                        Text("Cette agence n’a aucun véhicule affiché.", color = Color.Gray)
                    }
                } else {
                    items(fleet) { car ->
                        ClientModernCarCard(
                            car = car,
                            isFavorite = favoriteIds.contains(car.id),
                            onToggleFavorite = { onToggleFavorite(car) },
                            onReserve = { onReserveCar(car) },
                            onOpenDetails = { onOpenCarDetails(car) }
                        )
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
}

@Composable
fun AllBrandsDialog(
    brands: List<String>,
    onDismiss: () -> Unit,
    onBrandClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Toutes les marques") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (brands.isEmpty()) {
                    item {
                        Text("Aucune marque trouvée.", color = Color.Gray)
                    }
                } else {
                    items(brands) { brand ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBrandClick(brand) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(38.dp)
                                        .width(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1DA1F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        brand.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(brand, fontWeight = FontWeight.Bold)
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
}

@Composable
fun CityDialog(
    currentCity: String,
    onDismiss: () -> Unit,
    onSelectCity: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une ville") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CityItem(
                        city = "Algérie",
                        selected = currentCity == "Algérie",
                        onClick = { onSelectCity("Algérie") }
                    )
                }

                items(AppOptions.wilayas) { wilaya ->
                    CityItem(
                        city = wilaya,
                        selected = currentCity == wilaya,
                        onClick = { onSelectCity(wilaya) }
                    )
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
}

@Composable
fun CityItem(
    city: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE3F2FD) else Color.White
        )
    ) {
        Text(
            text = if (selected) "✓ $city" else city,
            modifier = Modifier.padding(12.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFF1DA1F2) else Color.Black
        )
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
                        error = ""
                    }
                )

                AppDateButton(
                    label = "Date fin",
                    value = endDateText,
                    onDateSelected = { text, millis ->
                        endDateText = text
                        endMillis = millis
                        error = ""
                    }
                )

                if (totalDays > 0) {
                    Text("Durée : $totalDays jour(s)")
                    Text("Total : $totalPrice DA")
                }

                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (startMillis <= 0L || endMillis <= 0L) {
                        error = "Veuillez sélectionner les deux dates."
                        return@Button
                    }

                    if (endMillis < startMillis) {
                        error = "La date fin doit être après la date début."
                        return@Button
                    }

                    onConfirm(startDateText, endDateText, startMillis, endMillis, totalDays)
                }
            ) {
                Text("Envoyer demande")
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
fun AppDateButton(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrer la recherche") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { maxPrice = it },
                    label = { Text("Prix maximum") },
                    modifier = Modifier.fillMaxWidth()
                )

                AppDropdownLike(
                    label = "Ville",
                    value = city,
                    items = listOf("Algérie") + AppOptions.wilayas,
                    onItemSelected = { city = it }
                )

                OutlinedTextField(
                    value = fuel,
                    onValueChange = { fuel = it },
                    label = { Text("Carburant : Diesel, Essence...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gearbox,
                    onValueChange = { gearbox = it },
                    label = { Text("Boîte : Automatique, Manuelle") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        ClientFilterState(
                            maxPrice = maxPrice,
                            city = city,
                            fuel = fuel,
                            gearbox = gearbox
                        )
                    )
                }
            ) {
                Text("Appliquer")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClear) {
                    Text("Effacer")
                }

                OutlinedButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )
}

@Composable
fun AppDropdownLike(
    label: String,
    value: String,
    items: List<String>,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (value.isBlank()) label else "$label : $value")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ClientFavoritesTab(
    cars: List<Car>,
    favoriteIds: Set<String>,
    onToggleFavorite: (Car) -> Unit,
    onReserveClick: (Car) -> Unit,
    onCarClick: (Car) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Voitures favorites",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (cars.isEmpty()) {
            item {
                EmptyClientCard("Vous n’avez aucune voiture favorite.")
            }
        } else {
            items(cars) { car ->
                ClientModernCarCard(
                    car = car,
                    isFavorite = favoriteIds.contains(car.id),
                    onToggleFavorite = { onToggleFavorite(car) },
                    onReserve = { onReserveClick(car) },
                    onOpenDetails = { onCarClick(car) }
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
    var showHistory by remember { mutableStateOf(false) }

    val today = todayStartMillis()

    val currentAndFutureReservations = reservations
        .filter { reservation ->
            reservation.endDateMillis >= today
        }
        .sortedWith(
            compareBy<ClientReservationUi> { it.startDateMillis }
                .thenBy { it.endDateMillis }
        )

    val oldReservations = reservations
        .filter { reservation ->
            reservation.endDateMillis > 0L && reservation.endDateMillis < today
        }
        .sortedByDescending { it.endDateMillis }

    val currentPendingCount = currentAndFutureReservations.count { it.status == "pending" }
    val currentAcceptedCount = currentAndFutureReservations.count { it.status == "accepted" }
    val currentRefusedCount = currentAndFutureReservations.count { it.status == "refused" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Mes réservations",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Réservations en cours et prochaines",
                color = Color.Gray
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReservationMiniCard("En attente", currentPendingCount.toString(), Modifier.weight(1f))
                ReservationMiniCard("Validées", currentAcceptedCount.toString(), Modifier.weight(1f))
                ReservationMiniCard("Refusées", currentRefusedCount.toString(), Modifier.weight(1f))
            }
        }

        item {
            OutlinedButton(
                onClick = { showHistory = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Historique des réservations (${oldReservations.size})")
            }
        }

        if (currentAndFutureReservations.isEmpty()) {
            item {
                EmptyClientCard("Aucune réservation en cours ou prochaine.")
            }
        } else {
            items(currentAndFutureReservations) { reservation ->
                val car = cars.firstOrNull { it.id == reservation.carId }

                ClientReservationCard(
                    reservation = reservation,
                    car = car,
                    isHistory = false,
                    onCancelReservation = { onCancelReservation(reservation) },
                    onReviewReservation = { onReviewReservation(reservation) }
                )
            }
        }
    }

    if (showHistory) {
        ReservationHistoryDialog(
            reservations = oldReservations,
            cars = cars,
            onDismiss = { showHistory = false },
            onReviewReservation = onReviewReservation
        )
    }
}

@Composable
fun ReservationHistoryDialog(
    reservations: List<ClientReservationUi>,
    cars: List<Car>,
    onDismiss: () -> Unit,
    onReviewReservation: (ClientReservationUi) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historique des réservations") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (reservations.isEmpty()) {
                    item {
                        Text(
                            text = "Aucune ancienne réservation.",
                            color = Color.Gray
                        )
                    }
                } else {
                    items(reservations) { reservation ->
                        val car = cars.firstOrNull { it.id == reservation.carId }

                        ClientReservationCard(
                            reservation = reservation,
                            car = car,
                            isHistory = true,
                            onCancelReservation = {},
                            onReviewReservation = { onReviewReservation(reservation) }
                        )
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
}

@Composable
fun ClientReservationCard(
    reservation: ClientReservationUi,
    car: Car?,
    isHistory: Boolean,
    onCancelReservation: () -> Unit,
    onReviewReservation: () -> Unit
) {
    val today = todayStartMillis()

    val reservationStateText = when {
        reservation.endDateMillis < today -> "Période : passée"
        reservation.startDateMillis <= today && today <= reservation.endDateMillis -> "Période : en cours"
        else -> "Période : prochaine"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = reservation.carName.ifBlank {
                    car?.let { "${it.brandName} ${it.modelName}" } ?: "Voiture"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text("Agence : ${reservation.agencyName.ifBlank { "Agence" }}")
            Text("Du : ${reservation.startDateText}")
            Text("Au : ${reservation.endDateText}")
            Text("Durée : ${reservation.totalDays} jour(s)")
            Text("Total : ${reservation.totalPrice} DA")

            Text(
                text = reservationStateText,
                color = when {
                    reservation.endDateMillis < today -> Color.Gray
                    reservation.startDateMillis <= today && today <= reservation.endDateMillis -> Color(0xFFD13438)
                    else -> Color(0xFF1DA1F2)
                },
                fontWeight = FontWeight.Bold
            )

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

            if (!isHistory && reservation.status == "pending") {
                OutlinedButton(
                    onClick = onCancelReservation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Annuler la demande")
                }
            }

            if (
                (reservation.status == "accepted" || reservation.status == "finished")
                && !reservation.reviewed
            ) {
                Button(
                    onClick = onReviewReservation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) {
                    Text("Noter la voiture et l’agence")
                }
            }

            if (reservation.reviewed) {
                Text(
                    text = "Avis déjà envoyé.",
                    color = Color(0xFF13A10E),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ReservationMiniCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall)

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1DA1F2)
            )
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
                Text(
                    text = reservation.carName.ifBlank { "Voiture" },
                    fontWeight = FontWeight.Bold
                )

                Text("Donnez une note entre 1 et 5.")

                OutlinedTextField(
                    value = carRating,
                    onValueChange = {
                        carRating = it
                        error = ""
                    },
                    label = { Text("Note voiture /5") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = agencyRating,
                    onValueChange = {
                        agencyRating = it
                        error = ""
                    },
                    label = { Text("Note agence /5") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        comment = it
                        error = ""
                    },
                    label = { Text("Commentaire facultatif") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val carNote = carRating.toIntOrNull() ?: 0
                    val agencyNote = agencyRating.toIntOrNull() ?: 0

                    if (carNote !in 1..5 || agencyNote !in 1..5) {
                        error = "Les notes doivent être entre 1 et 5."
                        return@Button
                    }

                    onConfirm(carNote, agencyNote, comment)
                }
            ) {
                Text("Envoyer")
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Profil",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(5.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(76.dp)
                        .width(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1DA1F2)),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl.isBlank()) {
                        Text(
                            text = fullName.take(1).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Photo profil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(email, color = Color.Gray)
                Text("Téléphone : $phone")
                Text("Wilaya : $city")
            }
        }

        Button(
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Modifier le profil")
        }

        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Actualiser")
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Se déconnecter")
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
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            color = Color.Gray
        )
    }
}

@Composable
fun ClientBottomBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .height(62.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
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
fun BottomItem(
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
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = label,
            color = if (selected) Color(0xFF1DA1F2) else Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

fun calculateDays(
    startMillis: Long,
    endMillis: Long
): Int {
    if (startMillis <= 0L || endMillis <= 0L) return 0
    if (endMillis < startMillis) return 0

    val oneDay = 24 * 60 * 60 * 1000L

    return (((endMillis - startMillis) / oneDay) + 1).toInt()
}

fun dateRangesOverlap(
    start1: Long,
    end1: Long,
    start2: Long,
    end2: Long
): Boolean {
    return start1 <= end2 && start2 <= end1
}


fun todayStartMillis(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

fun isCarReservedToday(
    carId: String,
    acceptedReservations: List<ClientReservationUi>
): Boolean {
    val today = todayStartMillis()

    return acceptedReservations.any { reservation ->
        reservation.carId == carId &&
                reservation.status == "accepted" &&
                today >= reservation.startDateMillis &&
                today <= reservation.endDateMillis
    }
}

fun starsText(note: Int): String {
    val validNote = note.coerceIn(0, 5)
    val fullStars = "★".repeat(validNote)
    val emptyStars = "☆".repeat(5 - validNote)
    return fullStars + emptyStars
}