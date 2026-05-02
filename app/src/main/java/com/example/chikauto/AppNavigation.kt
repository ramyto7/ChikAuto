package com.example.chikauto.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chikauto.ui.admin.AdminDashboardScreen
import com.example.chikauto.ui.agency.AgencyDashboardScreen
import com.example.chikauto.ui.auth.LoginScreen
import com.example.chikauto.ui.auth.RegisterScreen
import com.example.chikauto.ui.client.ClientHomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController)
        }

        composable("register") {
            RegisterScreen(navController)
        }

        composable("client_home") {
            ClientHomeScreen(navController)
        }

        composable("agency_dashboard") {
            AgencyDashboardScreen(navController)
        }

        composable("admin_dashboard") {
            AdminDashboardScreen(navController)
        }
    }
}