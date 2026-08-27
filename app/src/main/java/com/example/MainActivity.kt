package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import com.example.ui.components.GpsPermissionDialog
import com.example.ui.components.PumpDetailDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.model.Pump
import com.example.data.repository.CngRepository
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Surface
import com.example.ui.components.LanguageSelectorDialog
import com.example.util.AppLocalization
import com.example.util.LocaleManager
import com.example.util.ProvideAppLocale
import com.example.ui.navigation.Screen
import com.example.ui.navigation.bottomNavScreens
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CarManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PumpFinderScreen
import com.example.ui.screens.RefillDiaryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CNGTrackTheme
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.CarViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.PumpViewModel
import com.example.ui.viewmodel.RefillViewModel
import com.example.ui.viewmodel.ReportViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.util.AppPreferences
import com.example.util.LocationHelper
import com.example.util.LocationService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val preferences = AppPreferences(applicationContext)
        val firestoreSyncRepo = com.example.data.repository.FirestoreSyncRepositoryImpl(
            carDao = database.carDao(),
            refillDao = database.refillDao(),
            appPreferences = preferences
        )
        val repository = CngRepository(
            carDao = database.carDao(),
            pumpDao = database.pumpDao(),
            refillDao = database.refillDao(),
            pumpRatingDao = database.pumpRatingDao(),
            priceHistoryDao = database.priceHistoryDao(),
            cachedSearchDao = database.cachedSearchDao(),
            firestoreSyncRepository = firestoreSyncRepo
        )
        val localeManager = LocaleManager.getInstance(applicationContext)

        // Initialize notification channel & WorkManager for favorited CNG pumps & mileage reminders
        com.example.util.NotificationHelper.createNotificationChannel(applicationContext)
        com.example.worker.FavoritePumpsWorker.schedulePeriodicCheck(applicationContext)
        if (preferences.mileageDiaryReminderEnabled) {
            com.example.worker.MileageDiaryReminderWorker.schedulePeriodicCheck(applicationContext)
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(repository, preferences, localeManager)
            )
            val settingsState by settingsViewModel.settingsState.collectAsState()
            val isDarkTheme = when (settingsState.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            CNGTrackTheme(darkTheme = isDarkTheme) {
                ProvideAppLocale(localeManager = localeManager) {
                    CngTrackMainApp(
                        repository = repository,
                        preferences = preferences,
                        localeManager = localeManager,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CngTrackMainApp(
    repository: CngRepository,
    preferences: AppPreferences,
    localeManager: LocaleManager = LocaleManager.getInstance(LocalContext.current),
    settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(repository, preferences, localeManager)
    )
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // ViewModels
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
    val pumpViewModel: PumpViewModel = viewModel(factory = PumpViewModel.Factory(repository, settingsViewModel.syncHelper))
    val refillViewModel: RefillViewModel = viewModel(factory = RefillViewModel.Factory(repository))
    val carViewModel: CarViewModel = viewModel(factory = CarViewModel.Factory(repository))
    val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModel.Factory(repository))
    val settingsState by settingsViewModel.settingsState.collectAsState()

    // Language Selector dialog state
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val strings = remember(settingsState.selectedLanguage) {
        AppLocalization.getStrings(settingsState.selectedLanguage)
    }

    // State for Pump Details modal
    var selectedPumpForDetail by remember { mutableStateOf<Pump?>(null) }
    var selectedPumpDistance by rememberSaveable { mutableStateOf(2.5) }

    // GPS Permission Rationale state - only shown if user explicitly initiates a location-based feature
    val isLocationAlreadyGranted: Boolean = remember(context) {
        LocationService.isPermissionGranted(context)
    }
    var isGpsPreviouslyDenied by rememberSaveable { mutableStateOf(false) }
    var showGpsRationaleDialog by rememberSaveable { mutableStateOf(false) }

    // Auto-fetch location on initial load or reload if permission is already granted
    LaunchedEffect(Unit) {
        if (LocationService.isPermissionGranted(context)) {
            LocationHelper.fetchCurrentLocation(
                context = context,
                onSuccess = { loc ->
                    pumpViewModel.setGpsLocation(loc.latitude, loc.longitude)
                },
                onFailure = {
                    // Fall back to saved or default city without crash
                }
            )
        }
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        preferences.isGpsPermissionPrompted = true
        showGpsRationaleDialog = false

        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            isGpsPreviouslyDenied = false
            LocationHelper.fetchCurrentLocation(
                context = context,
                onSuccess = { loc ->
                    pumpViewModel.setGpsLocation(loc.latitude, loc.longitude)
                },
                onFailure = {
                    pumpViewModel.setGpsDenied()
                }
            )
        } else {
            isGpsPreviouslyDenied = true
            pumpViewModel.setGpsDenied()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = "CNGमित्र Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.appTitle,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = strings.appTagline,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    // TOP OF UI TRANSLATE / LANGUAGE BUTTON
                    Surface(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { showLanguageDialog = true }
                            .testTag("top_bar_translate_button"),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x33FFFFFF),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Translate App",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    settingsState.selectedLanguage.startsWith("Hindi") || settingsState.selectedLanguage.contains("हिंदी") -> "हिंदी"
                                    settingsState.selectedLanguage.startsWith("Marathi") || settingsState.selectedLanguage.contains("मराठी") -> "मराठी"
                                    settingsState.selectedLanguage.startsWith("Gujarati") || settingsState.selectedLanguage.contains("ગુજરાતી") -> "ગુજરાતી"
                                    settingsState.selectedLanguage.startsWith("Punjabi") || settingsState.selectedLanguage.contains("ਪੰਜਾਬੀ") -> "ਪੰਜਾਬੀ"
                                    settingsState.selectedLanguage.startsWith("Tamil") || settingsState.selectedLanguage.contains("தமிழ்") -> "தமிழ்"
                                    settingsState.selectedLanguage.startsWith("Telugu") || settingsState.selectedLanguage.contains("తెలుగు") -> "తెలుగు"
                                    settingsState.selectedLanguage.startsWith("Bengali") || settingsState.selectedLanguage.contains("বাংলা") -> "বাংলা"
                                    settingsState.selectedLanguage.startsWith("Kannada") || settingsState.selectedLanguage.contains("ಕನ್ನಡ") -> "ಕನ್ನಡ"
                                    settingsState.selectedLanguage.startsWith("Spanish") || settingsState.selectedLanguage.contains("Español") -> "ESP"
                                    else -> "English"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Notification Icon with Active Tip Badge
                    IconButton(
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "🔔 CNG Alert: High pressure (210 BAR) reported at nearby stations today!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        },
                        modifier = Modifier.testTag("top_bar_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = EmeraldGreen,
                                    contentColor = Color.White
                                ) {
                                    Text("1", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }

                    // Profile / Driver Icon
                    IconButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.testTag("top_bar_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile & Settings",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkTeal)
            )
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(
                containerColor = DarkTeal,
                contentColor = Color.White
            ) {
                bottomNavScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    val screenTitle = when (screen) {
                        Screen.Home -> strings.navHome
                        Screen.Pumps -> strings.navPumps
                        Screen.Refills -> "History"
                        Screen.Cars -> "Vehicle"
                        Screen.Reports -> strings.navReports
                        Screen.Settings -> "Profile"
                        Screen.Auth -> "Sign In"
                    }

                    NavigationBarItem(
                        modifier = Modifier.testTag("nav_${screen.route}"),
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screenTitle,
                                tint = if (isSelected) EmeraldGreen else Color.White.copy(alpha = 0.7f)
                            )
                        },
                        label = {
                            Text(
                                text = screenTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = EmeraldGreen.copy(alpha = 0.25f),
                            selectedIconColor = EmeraldGreen,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    pumpViewModel = pumpViewModel,
                    refillViewModel = refillViewModel,
                    settingsViewModel = settingsViewModel,
                    onNavigateToPumps = {
                        navController.navigate(Screen.Pumps.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAddRefill = {
                        navController.navigate(Screen.Refills.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCars = {
                        navController.navigate(Screen.Cars.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSelectPump = { pump ->
                        selectedPumpForDetail = pump
                        selectedPumpDistance = 2.4
                    }
                )
            }

            composable(Screen.Pumps.route) {
                PumpFinderScreen(
                    pumpViewModel = pumpViewModel,
                    selectedLanguage = settingsState.selectedLanguage,
                    onRequestLocationPermission = { showGpsRationaleDialog = true },
                    onSelectPump = { pump ->
                        selectedPumpForDetail = pump
                        selectedPumpDistance = 3.1
                    }
                )
            }

            composable(Screen.Refills.route) {
                RefillDiaryScreen(
                    refillViewModel = refillViewModel,
                    selectedLanguage = settingsState.selectedLanguage
                )
            }

            composable(Screen.Cars.route) {
                CarManagerScreen(
                    carViewModel = carViewModel,
                    selectedLanguage = settingsState.selectedLanguage
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    reportViewModel = reportViewModel,
                    selectedLanguage = settingsState.selectedLanguage
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    snackbarHostState = snackbarHostState,
                    onRequestLocationPermission = { showGpsRationaleDialog = true },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            composable(Screen.Auth.route) {
                AuthScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAuthSuccess = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

    // Top Bar Language Selector Dialog
    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = settingsState.selectedLanguage,
            onLanguageSelected = { newLanguage ->
                settingsViewModel.setAppLanguage(newLanguage)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Location Permission Dedicated Rationale Dialog
    if (showGpsRationaleDialog) {
        GpsPermissionDialog(
            isPreviouslyDenied = isGpsPreviouslyDenied,
            selectedLanguage = settingsState.selectedLanguage,
            onAllowClicked = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDenyClicked = {
                preferences.isGpsPermissionPrompted = true
                showGpsRationaleDialog = false
                pumpViewModel.setGpsDenied()
            },
            onSelectCityClicked = { userLocation: LocationHelper.UserLocation ->
                preferences.isGpsPermissionPrompted = true
                showGpsRationaleDialog = false
                pumpViewModel.selectCity(userLocation)
            }
        )
    }

    // Pump Details Modal Sheet
    selectedPumpForDetail?.let { pump: Pump ->
        PumpDetailDialog(
            pump = pump,
            distanceKm = selectedPumpDistance,
            viewModel = pumpViewModel,
            onDismiss = { selectedPumpForDetail = null }
        )
    }
}
