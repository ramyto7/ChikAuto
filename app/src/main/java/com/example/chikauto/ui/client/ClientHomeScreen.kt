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

data class ClientConversationUi(
    val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val agencyId: String = "",
    val agencyName: String = "",
    val carId: String = "",
    val carName: String = "",
    val lastMessage: String = "",
    val updatedAt: Long = 0L,
    val unreadForClient: Long = 0L
)

data class ClientChatMessageUi(
    val id: String = "",
    val senderId: String = "",
    val senderRole: String = "",
    val text: String = "",
    val createdAt: Long = 0L
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
    var conversations by remember { mutableStateOf(listOf<ClientConversationUi>()) }
    var selectedConversation by remember { mutableStateOf<ClientConversationUi?>(null) }

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
    var showDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("fr") }
    var darkMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

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

        db.collection("conversations")
            .whereEqualTo("clientId", clientId)
            .get()
            .addOnSuccessListener { result ->
                conversations = result.documents.map { doc ->
                    ClientConversationUi(
                        id = doc.id,
                        clientId = doc.getString("clientId") ?: clientId,
                        clientName = doc.getString("clientName") ?: fullName,
                        agencyId = doc.getString("agencyId") ?: "",
                        agencyName = doc.getString("agencyName") ?: "Agence",
                        carId = doc.getString("carId") ?: "",
                        carName = doc.getString("carName") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        updatedAt = doc.getLong("updatedAt") ?: 0L,
                        unreadForClient = doc.getLong("unreadForClient") ?: 0L
                    )
                }.sortedByDescending { it.updatedAt }
            }
            .addOnFailureListener {
                message = "Erreur chargement messagerie : ${it.message}"
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

    fun openConversationWithAgency(agency: ClientAgencyUi, car: Car? = null) {
        val agencyKey = agency.ownerId.ifBlank { agency.id }
        db.collection("conversations")
            .whereEqualTo("clientId", clientId)
            .whereEqualTo("agencyId", agencyKey)
            .whereEqualTo("carId", car?.id ?: "")
            .get()
            .addOnSuccessListener { result ->
                val existing = result.documents.firstOrNull()
                if (existing != null) {
                    selectedConversation = ClientConversationUi(
                        id = existing.id,
                        clientId = clientId,
                        clientName = existing.getString("clientName") ?: fullName,
                        agencyId = existing.getString("agencyId") ?: agencyKey,
                        agencyName = existing.getString("agencyName") ?: agency.agencyName,
                        carId = existing.getString("carId") ?: (car?.id ?: ""),
                        carName = existing.getString("carName") ?: car?.let { "${it.brandName} ${it.modelName}" }.orEmpty(),
                        lastMessage = existing.getString("lastMessage") ?: "",
                        updatedAt = existing.getLong("updatedAt") ?: 0L,
                        unreadForClient = existing.getLong("unreadForClient") ?: 0L
                    )
                    selectedTab = "messages"
                } else {
                    val ref = db.collection("conversations").document()
                    val carName = car?.let { "${it.brandName} ${it.modelName}" } ?: ""
                    val data = hashMapOf(
                        "clientId" to clientId,
                        "clientName" to fullName,
                        "agencyId" to agencyKey,
                        "agencyName" to agency.agencyName,
                        "carId" to (car?.id ?: ""),
                        "carName" to carName,
                        "lastMessage" to "",
                        "updatedAt" to System.currentTimeMillis(),
                        "unreadForClient" to 0L,
                        "unreadForAgency" to 0L
                    )
                    ref.set(data).addOnSuccessListener {
                        selectedConversation = ClientConversationUi(
                            id = ref.id, clientId = clientId, clientName = fullName, agencyId = agencyKey,
                            agencyName = agency.agencyName, carId = car?.id ?: "", carName = carName
                        )
                        selectedTab = "messages"
                        loadData()
                    }
                }
            }
            .addOnFailureListener { message = "Erreur ouverture messagerie : ${it.message}" }
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

    val screenBg = if (darkMode) Color(0xFF101418) else Color(0xFFF4F6F8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
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
                    onAgencyClick = { selectedAgencyForDetails = it },
                    onMenuClick = { showDrawer = true },
                    language = language,
                    darkMode = darkMode
                )

                "favorites" -> ClientFavoritesTab(
                    language = language,
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
                    language = language,
                    reservations = reservations,
                    cars = cars,
                    pendingCount = pendingCount,
                    acceptedCount = acceptedCount,
                    refusedCount = refusedCount,
                    onCancelReservation = { cancelReservation(it) },
                    onReviewReservation = { reservationForReview = it }
                )

                "messages" -> ClientMessagesTab(
                    clientId = clientId,
                    fullName = fullName,
                    conversations = conversations,
                    selectedConversation = selectedConversation,
                    onBack = { selectedConversation = null; selectedTab = "home"; loadData() },
                    onSelectConversation = { selectedConversation = it },
                    onRefresh = { loadData() }
                )

                "profile" -> ClientProfileTab(
                    language = language,
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

        if (showDrawer) {
            ClientSideDrawer(
                fullName = fullName,
                email = email,
                profileImageUrl = profileImageUrl,
                onClose = { showDrawer = false },
                onProfile = { showDrawer = false; selectedTab = "profile" },
                onSettings = { showDrawer = false; showSettings = true },
                onAbout = { showDrawer = false; showAbout = true },
                onTerms = { showDrawer = false; showTerms = true },
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("client_home") { inclusive = true }
                    }
                }
            )
        }

        ClientBottomBar(
            selectedTab = selectedTab,
            unreadMessagesCount = conversations.sumOf { it.unreadForClient }.toInt(),
            language = language,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    selectedCarForReservation?.let { car ->
        ReservationDialog(
            car = car,
            acceptedReservations = acceptedReservationsForAllCars.filter { it.carId == car.id },
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
            },
            onMessageAgency = {
                selectedAgencyForDetails = null
                openConversationWithAgency(agency)
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


    if (showAbout) {
        ClientInfoDialog(
            title = if (language == "en") "About ChikAuto" else "À propos de ChikAuto",
            icon = "ⓘ",
            body = if (language == "en") {
                "ChikAuto is a car rental application that connects clients with rental agencies. Clients can search for cars, reserve, manage favorites, follow reservations and contact agencies by messages."
            } else {
                "ChikAuto est une application de location de voitures qui relie les clients aux agences. Le client peut chercher une voiture, réserver, gérer ses favoris, suivre ses réservations et contacter les agences par messagerie."
            },
            onDismiss = { showAbout = false }
        )
    }

    if (showTerms) {
        ClientInfoDialog(
            title = if (language == "en") "Terms of use" else "Conditions d'utilisation",
            icon = "§",
            body = if (language == "en") {
                "Use the application respectfully. Reservations are confirmed only after agency approval. False information, abusive messages and fake reservations can lead to account suspension."
            } else {
                "Utilisez l’application correctement. Une réservation est confirmée seulement après l’acceptation de l’agence. Les fausses informations, les messages abusifs et les fausses réservations peuvent entraîner la suspension du compte."
            },
            onDismiss = { showTerms = false }
        )
    }

    if (showSettings) {
        ClientSettingsDialog(
            language = language,
            darkMode = darkMode,
            notificationsEnabled = notificationsEnabled,
            onLanguageChange = { language = it },
            onDarkModeChange = { darkMode = it },
            onNotificationsChange = { notificationsEnabled = it },
            onDismiss = { showSettings = false }
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
    onAgencyClick: (ClientAgencyUi) -> Unit,
    onMenuClick: () -> Unit,
    language: String,
    darkMode: Boolean
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
                onCityClick = onCityClick,
                onMenuClick = onMenuClick
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
    onCityClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.clickable { onMenuClick() },
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
    onOpenCarDetails: (Car) -> Unit,
    onMessageAgency: () -> Unit
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
                    OutlinedButton(
                        onClick = onMessageAgency,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("✉ Contacter")
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
    acceptedReservations: List<ClientReservationUi>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Long, Int) -> Unit
) {
    var startDateText by remember { mutableStateOf("") }
    var endDateText by remember { mutableStateOf("") }
    var startMillis by remember { mutableStateOf(0L) }
    var endMillis by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf("") }
    var pickingMode by remember { mutableStateOf<String?>(null) }

    val reservedDays = remember(acceptedReservations) { buildReservedDays(acceptedReservations) }
    val totalDays = calculateDays(startMillis, endMillis)
    val totalPrice = totalDays * car.pricePerDay

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Réserver ${car.brandName} ${car.modelName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Prix : ${car.pricePerDay} DA / jour")
                Text("Les jours déjà loués sont bloqués automatiquement.", color = Color.Gray)

                OutlinedButton(
                    onClick = { pickingMode = "start" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(if (startDateText.isBlank()) "Date début 📅" else "Date début : $startDateText") }

                OutlinedButton(
                    onClick = { pickingMode = "end" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = startMillis > 0L
                ) { Text(if (endDateText.isBlank()) "Date fin 📅" else "Date fin : $endDateText") }

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
                if (rangeContainsReservedDay(startMillis, endMillis, reservedDays)) {
                    error = "Cette période contient déjà des jours loués. Choisissez une autre période."
                    return@Button
                }
                onConfirm(startDateText, endDateText, startMillis, endMillis, totalDays)
            }) { Text("Envoyer demande") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Annuler") } }
    )

    pickingMode?.let { mode ->
        AvailabilityCalendarDialog(
            title = if (mode == "start") "Choisir la date début" else "Choisir la date fin",
            reservedDays = reservedDays,
            startMillis = startMillis,
            pickingEndDate = mode == "end",
            onDismiss = { pickingMode = null },
            onDateSelected = { text, millis ->
                if (mode == "start") {
                    startDateText = text
                    startMillis = millis
                    endDateText = ""
                    endMillis = 0L
                } else {
                    endDateText = text
                    endMillis = millis
                }
                error = ""
                pickingMode = null
            }
        )
    }
}

@Composable
fun AvailabilityCalendarDialog(
    title: String,
    reservedDays: Set<Long>,
    startMillis: Long,
    pickingEndDate: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (String, Long) -> Unit
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val today = todayStartMillis()
    val base = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, monthOffset)
    }
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.FRANCE).format(base.time)
    val maxAllowedEnd = if (pickingEndDate && startMillis > 0L) nextBlockedDayAfter(startMillis, reservedDays) else Long.MAX_VALUE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { monthOffset-- }, enabled = monthOffset > 0) { Text("‹") }
                    Text(monthName.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { monthOffset++ }) { Text("›") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("L", "M", "M", "J", "V", "S", "D").forEach { Text(it, fontWeight = FontWeight.Bold, color = Color.Gray) }
                }
                val days = monthCells(base)
                days.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { dayMillis ->
                            if (dayMillis == 0L) {
                                Box(Modifier.weight(1f).height(42.dp))
                            } else {
                                val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
                                val isReserved = reservedDays.contains(dayMillis)
                                val beforeToday = dayMillis < today
                                val beforeStart = pickingEndDate && dayMillis < startMillis
                                val afterBlocked = pickingEndDate && dayMillis >= maxAllowedEnd
                                val disabled = isReserved || beforeToday || beforeStart || afterBlocked
                                val selected = dayMillis == startMillis
                                Card(
                                    modifier = Modifier.weight(1f).height(42.dp).clickable(enabled = !disabled) {
                                        onDateSelected(formatter.format(cal.time), dayMillis)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            disabled -> Color(0xFFE1E5EA)
                                            selected -> Color(0xFF1DA1F2)
                                            else -> Color.White
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(if (disabled) 0.dp else 2.dp)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                                            color = when {
                                                disabled -> Color.Gray
                                                selected -> Color.White
                                                else -> Color.Black
                                            },
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Gris = indisponible. Pour la date fin, les jours après une réservation bloquée sont aussi désactivés.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Fermer") } }
    )
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
    language: String,
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
                text = if (language == "en") "Favorite cars" else "Voitures favorites",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (cars.isEmpty()) {
            item {
                EmptyClientCard(if (language == "en") "You have no favorite cars." else "Vous n’avez aucune voiture favorite.")
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
    language: String,
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
                text = if (language == "en") "My reservations" else "Mes réservations",
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
    language: String,
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
            text = if (language == "en") "Profile" else "Profil",
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
            Text(if (language == "en") "Edit profile" else "Modifier le profil")
        }

        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(if (language == "en") "Refresh" else "Actualiser")
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
fun ProfileCircle(text: String, imageUrl: String) {
    Box(
        modifier = Modifier
            .height(50.dp)
            .width(50.dp)
            .clip(CircleShape)
            .background(Color(0xFF1DA1F2)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Photo profil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(text.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ClientSideDrawer(
    fullName: String,
    email: String,
    profileImageUrl: String,
    onClose: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onTerms: () -> Unit,
    onLogout: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable { onClose() }) {
        Card(
            modifier = Modifier.fillMaxHeight().width(310.dp).padding(12.dp).clickable(enabled = false) {},
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileCircle(text = fullName.take(1).ifBlank { "C" }, imageUrl = profileImageUrl)
                    Column {
                        Text(fullName.ifBlank { "Client" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(email.ifBlank { "Email non précisé" }, color = Color.Gray)
                    }
                }
                Divider()
                DrawerAction("👤", "Mon profil", onProfile)
                DrawerAction("⚙", "Paramètres", onSettings)
                DrawerAction("ⓘ", "À propos", onAbout)
                DrawerAction("§", "Conditions d'utilisation", onTerms)
                Spacer(Modifier.weight(1f))
                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD13438))) {
                    Text("Se déconnecter")
                }
            }
        }
    }
}

@Composable
fun DrawerAction(icon: String, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, style = MaterialTheme.typography.titleLarge)
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ClientInfoDialog(
    title: String,
    icon: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, style = MaterialTheme.typography.headlineSmall)
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp)) { Text("OK") }
        }
    )
}

@Composable
fun ClientSettingsDialog(
    language: String,
    darkMode: Boolean,
    notificationsEnabled: Boolean,
    onLanguageChange: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (language == "en") "Settings" else "Paramètres") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (language == "en") "Language" else "Langue", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { onLanguageChange("fr") }, label = { Text("Français") })
                    AssistChip(onClick = { onLanguageChange("en") }, label = { Text("English") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (language == "en") "Dark mode" else "Mode sombre")
                    Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (language == "en") "Notifications" else "Notifications")
                    Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChange)
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(if (language == "en") "OK" else "Valider") } }
    )
}



@Composable
fun SimpleClientCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Text(text = text, modifier = Modifier.padding(16.dp), color = Color.Gray)
    }
}

@Composable
fun ClientMessagesTab(
    clientId: String,
    fullName: String,
    conversations: List<ClientConversationUi>,
    selectedConversation: ClientConversationUi?,
    onBack: () -> Unit,
    onSelectConversation: (ClientConversationUi) -> Unit,
    onRefresh: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var messages by remember(selectedConversation?.id) { mutableStateOf(listOf<ClientChatMessageUi>()) }
    var text by remember(selectedConversation?.id) { mutableStateOf("") }
    var localMessage by remember { mutableStateOf("") }

    fun loadMessages(conversation: ClientConversationUi) {
        db.collection("conversations").document(conversation.id)
            .collection("messages")
            .get()
            .addOnSuccessListener { result ->
                messages = result.documents.map { doc ->
                    ClientChatMessageUi(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderRole = doc.getString("senderRole") ?: "",
                        text = doc.getString("text") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.sortedBy { it.createdAt }
                db.collection("conversations").document(conversation.id).update("unreadForClient", 0L)
            }
            .addOnFailureListener { localMessage = "Erreur chargement messages : ${it.message}" }
    }

    fun sendMessage(conversation: ClientConversationUi) {
        val content = text.trim()
        if (content.isBlank()) return
        val now = System.currentTimeMillis()
        val convRef = db.collection("conversations").document(conversation.id)
        val msg = hashMapOf(
            "senderId" to clientId,
            "senderRole" to "client",
            "text" to content,
            "createdAt" to now
        )
        convRef.collection("messages").add(msg).addOnSuccessListener {
            convRef.update(
                mapOf(
                    "lastMessage" to content,
                    "updatedAt" to now,
                    "unreadForAgency" to 1L,
                    "clientName" to fullName
                )
            )
            text = ""
            loadMessages(conversation)
            onRefresh()
        }.addOnFailureListener { localMessage = "Erreur envoi : ${it.message}" }
    }

    LaunchedEffect(selectedConversation?.id) {
        selectedConversation?.let { if (it.id.isNotBlank()) loadMessages(it) }
    }

    if (selectedConversation == null || selectedConversation.id.isBlank()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onBack, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))) { Text("← Retour") }
                    OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(16.dp)) { Text("Actualiser") }
                }
            }
            item { Text("Messages", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Vos discussions avec les agences", color = Color.Gray) }
            if (conversations.isEmpty()) {
                item { SimpleClientCard("Aucun message pour le moment.") }
            } else {
                items(conversations) { conversation ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectConversation(conversation) },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.width(46.dp).height(46.dp).clip(CircleShape).background(Color(0xFF1DA1F2)), contentAlignment = Alignment.Center) { Text(conversation.agencyName.take(1).ifBlank { "A" }, color = Color.White, fontWeight = FontWeight.Bold) }
                            Column(Modifier.weight(1f)) {
                                Text(conversation.agencyName.ifBlank { "Agence" }, fontWeight = FontWeight.Bold)
                                Text(conversation.carName.ifBlank { "Discussion générale" }, color = Color.Gray)
                                if (conversation.lastMessage.isNotBlank()) Text(conversation.lastMessage, color = Color.Gray)
                            }
                            if (conversation.unreadForClient > 0) {
                                Box(Modifier.width(24.dp).height(24.dp).clip(CircleShape).background(Color(0xFFD13438)), contentAlignment = Alignment.Center) { Text(conversation.unreadForClient.toString(), color = Color.White, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onSelectConversation(ClientConversationUi()) }, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))) { Text("← Retour") }
                Column { Text(selectedConversation.agencyName, fontWeight = FontWeight.Bold); Text(selectedConversation.carName.ifBlank { "Discussion" }, color = Color.Gray) }
            }
            if (localMessage.isNotEmpty()) Text(localMessage, color = MaterialTheme.colorScheme.error)
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (messages.isEmpty()) item { SimpleClientCard("Aucun message dans cette discussion.") }
                items(messages) { msg ->
                    val mine = msg.senderRole == "client"
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
fun ClientBottomBar(
    selectedTab: String,
    unreadMessagesCount: Int,
    language: String,
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
            BottomItem("home", if (language == "en") "Home" else "Accueil", "⌂", selectedTab, onTabSelected)
            BottomItem("favorites", if (language == "en") "Favorites" else "Favoris", "♡", selectedTab, onTabSelected)
            BottomItem("reservations", if (language == "en") "Bookings" else "Réserv.", "▣", selectedTab, onTabSelected)
            BottomItem("messages", if (unreadMessagesCount > 0) "Msg $unreadMessagesCount" else if (language == "en") "Messages" else "Messages", "✉", selectedTab, onTabSelected)
            BottomItem("profile", if (language == "en") "Profile" else "Profil", "●", selectedTab, onTabSelected)
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



fun normalizeDayMillis(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

fun buildReservedDays(reservations: List<ClientReservationUi>): Set<Long> {
    val days = mutableSetOf<Long>()
    val oneDay = 24 * 60 * 60 * 1000L
    reservations.filter { it.status == "accepted" }.forEach { reservation ->
        var current = normalizeDayMillis(reservation.startDateMillis)
        val end = normalizeDayMillis(reservation.endDateMillis)
        while (current <= end) {
            days.add(current)
            current += oneDay
        }
    }
    return days
}

fun rangeContainsReservedDay(startMillis: Long, endMillis: Long, reservedDays: Set<Long>): Boolean {
    val oneDay = 24 * 60 * 60 * 1000L
    var current = normalizeDayMillis(startMillis)
    val end = normalizeDayMillis(endMillis)
    while (current <= end) {
        if (reservedDays.contains(current)) return true
        current += oneDay
    }
    return false
}

fun nextBlockedDayAfter(startMillis: Long, reservedDays: Set<Long>): Long {
    return reservedDays.filter { it > normalizeDayMillis(startMillis) }.minOrNull() ?: Long.MAX_VALUE
}

fun monthCells(monthCalendar: Calendar): List<Long> {
    val cal = monthCalendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDay = cal.get(Calendar.DAY_OF_WEEK)
    val mondayBasedBlank = (firstDay + 5) % 7
    val result = mutableListOf<Long>()
    repeat(mondayBasedBlank) { result.add(0L) }
    val max = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (day in 1..max) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        result.add(normalizeDayMillis(cal.timeInMillis))
    }
    while (result.size % 7 != 0) result.add(0L)
    return result
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