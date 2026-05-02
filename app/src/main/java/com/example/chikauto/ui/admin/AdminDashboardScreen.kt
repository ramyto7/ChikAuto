package com.example.chikauto.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chikauto.data.model.Agency
import com.example.chikauto.data.model.AppOptions
import com.example.chikauto.data.model.Brand
import com.example.chikauto.data.model.CarModel
import com.example.chikauto.ui.components.AppDropdown
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminDashboardScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()

    var screen by remember { mutableStateOf("home") }

    var usersCount by remember { mutableIntStateOf(0) }
    var clientsCount by remember { mutableIntStateOf(0) }
    var agenciesCount by remember { mutableIntStateOf(0) }
    var carsCount by remember { mutableIntStateOf(0) }

    var pendingAgencies by remember { mutableStateOf(listOf<Agency>()) }
    var brands by remember { mutableStateOf(listOf<Brand>()) }
    var models by remember { mutableStateOf(listOf<CarModel>()) }

    var message by remember { mutableStateOf("") }

    fun loadData() {
        db.collection("users").get().addOnSuccessListener { result ->
            usersCount = result.size()
            clientsCount = result.documents.count { it.getString("role") == "client" }
            agenciesCount = result.documents.count { it.getString("role") == "agency" }
        }

        db.collection("cars").get().addOnSuccessListener { result ->
            carsCount = result.size()
        }

        db.collection("agencies")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                pendingAgencies = result.documents.mapNotNull {
                    it.toObject(Agency::class.java)?.copy(id = it.id)
                }
            }

        db.collection("carBrands").get().addOnSuccessListener { result ->
            brands = result.documents.mapNotNull {
                it.toObject(Brand::class.java)?.copy(id = it.id)
            }
        }

        db.collection("carModels").get().addOnSuccessListener { result ->
            models = result.documents.mapNotNull {
                it.toObject(CarModel::class.java)?.copy(id = it.id)
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (screen == "home") {
        AdminHomeScreen(
            navController = navController,
            usersCount = usersCount,
            clientsCount = clientsCount,
            agenciesCount = agenciesCount,
            carsCount = carsCount,
            pendingAgencies = pendingAgencies,
            message = message,
            onOpenCatalog = { screen = "catalog" },
            onValidateAgency = { agency ->
                db.collection("agencies").document(agency.id)
                    .update("status", "active")
                    .addOnSuccessListener {
                        db.collection("users").document(agency.ownerId)
                            .update("status", "active")
                            .addOnSuccessListener {
                                message = "Agence validée."
                                loadData()
                            }
                    }
            },
            onRefuseAgency = { agency ->
                db.collection("agencies").document(agency.id)
                    .update("status", "refused")
                    .addOnSuccessListener {
                        db.collection("users").document(agency.ownerId)
                            .update("status", "refused")
                            .addOnSuccessListener {
                                message = "Agence refusée."
                                loadData()
                            }
                    }
            }
        )
    } else {
        AdminCatalogScreen(
            brands = brands,
            models = models,
            onBack = { screen = "home" },
            onReload = { loadData() }
        )
    }
}

@Composable
fun AdminHomeScreen(
    navController: NavController,
    usersCount: Int,
    clientsCount: Int,
    agenciesCount: Int,
    carsCount: Int,
    pendingAgencies: List<Agency>,
    message: String,
    onOpenCatalog: () -> Unit,
    onValidateAgency: (Agency) -> Unit,
    onRefuseAgency: (Agency) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Administration ChickAUTO", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatCard("Utilisateurs", usersCount.toString(), Modifier.weight(1f))
                AdminStatCard("Clients", clientsCount.toString(), Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatCard("Agences", agenciesCount.toString(), Modifier.weight(1f))
                AdminStatCard("Voitures", carsCount.toString(), Modifier.weight(1f))
            }
        }

        item {
            Button(
                onClick = onOpenCatalog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gérer les marques et modèles")
            }
        }

        if (message.isNotEmpty()) {
            item {
                Text(message, color = MaterialTheme.colorScheme.primary)
            }
        }

        item {
            Text("Demandes agences", style = MaterialTheme.typography.titleLarge)
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
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(agency.agencyName, style = MaterialTheme.typography.titleLarge)
                        Text("Wilaya : ${agency.city}")
                        Text("Adresse : ${agency.address}")
                        Text("Téléphone : ${agency.phone}")
                        Text("Email : ${agency.email}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onValidateAgency(agency) }) {
                                Text("Valider")
                            }

                            OutlinedButton(onClick = { onRefuseAgency(agency) }) {
                                Text("Refuser")
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    navController.navigate("login") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Se déconnecter")
            }
        }
    }
}
@Composable
fun AdminCatalogScreen(
    brands: List<Brand>,
    models: List<CarModel>,
    onBack: () -> Unit,
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
            .background(Color(0xFFF4F6F8))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Marques et modèles", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retour accueil admin")
            }
        }

        if (message.isNotEmpty()) {
            item {
                Text(message, color = MaterialTheme.colorScheme.primary)
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
                    Text("Ajouter une marque", style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = newBrand,
                        onValueChange = { newBrand = it },
                        label = { Text("Exemple : Audi") },
                        modifier = Modifier.fillMaxWidth()
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
                        },
                        modifier = Modifier.fillMaxWidth()
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
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Ajouter un modèle", style = MaterialTheme.typography.titleLarge)

                    AppDropdown(
                        label = "Marque",
                        value = selectedBrandName,
                        items = brands.map { it.name },
                        onItemSelected = { value ->
                            selectedBrandName = value
                            val brand = brands.firstOrNull { it.name == value }
                            selectedBrandId = brand?.id ?: ""
                        }
                    )

                    OutlinedTextField(
                        value = newModelName,
                        onValueChange = { newModelName = it },
                        label = { Text("Exemple : A3") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    AppDropdown(
                        label = "Type",
                        value = selectedType,
                        items = AppOptions.carTypes,
                        onItemSelected = { selectedType = it }
                    )

                    Button(
                        onClick = {
                            if (!brands.map { it.name }.contains(selectedBrandName)) {
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
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ajouter le modèle")
                    }
                }
            }
        }

        item {
            Text("Marques existantes", style = MaterialTheme.typography.titleLarge)
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
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(brand.name, style = MaterialTheme.typography.titleLarge)

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
fun AdminStatCard(
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
            modifier = Modifier.padding(14.dp)
        ) {
            Text(title)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun SimpleCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp)
        )
    }
}