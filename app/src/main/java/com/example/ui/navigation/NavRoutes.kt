package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Pumps : Screen("pumps", "Pumps", Icons.Default.LocalGasStation)
    object Cars : Screen("cars", "Vehicle", Icons.Default.DirectionsCar)
    object Reports : Screen("reports", "Expenses", Icons.Default.Analytics)
    object Settings : Screen("settings", "Profile", Icons.Default.Person)
    object Refills : Screen("refills", "Refills", Icons.Default.Receipt)
    object Auth : Screen("auth", "Sign In", Icons.Default.Person)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Pumps,
    Screen.Cars,
    Screen.Refills,
    Screen.Settings
)
