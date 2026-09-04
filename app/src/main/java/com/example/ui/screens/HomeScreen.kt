package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.data.model.Pump
import com.example.ui.components.AppTourDialog
import com.example.ui.components.AutomotiveClusterCard
import com.example.ui.components.AutomotiveSavingsCard
import com.example.ui.components.CircularMileageGauge
import com.example.ui.components.CngStatusCard
import com.example.ui.components.DriverQuickActionsGrid
import com.example.ui.components.DriverRefillBottomSheet
import com.example.ui.components.GeminiCngAdvisorSheet
import com.example.ui.components.HomeRecentRefillsSection
import com.example.ui.components.HomeStatisticsGrid
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.NearestStationDistanceCard
import com.example.ui.components.WeeklyExpensesBarChart
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.PumpViewModel
import com.example.ui.viewmodel.RefillViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.util.AppLocalization
import com.example.util.CsvExportUtil
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    pumpViewModel: PumpViewModel,
    refillViewModel: RefillViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToPumps: () -> Unit,
    onNavigateToAddRefill: () -> Unit,
    onNavigateToCars: () -> Unit,
    onNavigateToReports: () -> Unit,
    onSelectPump: (Pump) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    val state by homeViewModel.uiState.collectAsState()
    val pumpItems by pumpViewModel.pumpsList.collectAsState()
    val carsList by refillViewModel.carsList.collectAsState()
    val refillsList by refillViewModel.refillsList.collectAsState()
    val pumpsList by refillViewModel.pumpsList.collectAsState()
    val settingsState by settingsViewModel.settingsState.collectAsState()

    val strings = remember(settingsState.selectedLanguage) {
        AppLocalization.getStrings(settingsState.selectedLanguage)
    }

    val appPreferences = remember { com.example.util.AppPreferences(context) }
    var showQuickRefillBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showGeminiAdvisorSheet by rememberSaveable { mutableStateOf(false) }
    var showAppTourDialog by rememberSaveable { mutableStateOf(!appPreferences.isAppTourCompleted) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showVehicleSelectDialog by rememberSaveable { mutableStateOf(false) }

    // FAB expands when at top, shrinks to icon on scroll
    val isFabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }

    // Calculate today's fuel expense
    val startOfToday = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val todayRefills = remember(refillsList, startOfToday) {
        refillsList.filter { it.date >= startOfToday }
    }
    val todayExpense = todayRefills.sumOf { it.totalAmount }
    val todayKg = todayRefills.sumOf { it.quantityKg }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Driver App Tour & AI Advisor Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showGeminiAdvisorSheet = true }
                        .testTag("home_ai_advisor_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Smart CNG Assistant & AI Tips",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Gemini AI mileage tuning, diagnostics & health",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showGeminiAdvisorSheet = true }
                            ) {
                                Text(
                                    text = "AI Tips",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { showAppTourDialog = true }
                            ) {
                                Text(
                                    text = "Tour",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 1. Main Status Card (AVAILABLE / OFFLINE, Live CNG Price, Gas Pressure 210 BAR, Last Updated)
            item {
                CngStatusCard(
                    currentPrice = state.currentCngPrice,
                    gasPressureBar = state.averageGasPressure,
                    isOpen = state.openPumpsCount > 0,
                    lastUpdatedText = "Live • Updated just now",
                    nearestPump = state.nearestPump,
                    nearestDistanceKm = state.nearestPumpDistanceKm,
                    onNavigateClick = {
                        state.nearestPump?.let { pump ->
                            com.example.util.NavigationUtil.navigateToPump(
                                context = context,
                                latitude = pump.latitude,
                                longitude = pump.longitude,
                                pumpName = pump.name
                            )
                        } ?: onNavigateToPumps()
                    }
                )
            }

            // 2. Nearest CNG Station Navigation Card with Google Maps Driving & Walking Distance
            item {
                NearestStationDistanceCard(
                    nearestPump = state.nearestPump ?: pumpItems.firstOrNull()?.pump,
                    userLatitude = LocationHelper.DEFAULT_LAT,
                    userLongitude = LocationHelper.DEFAULT_LNG,
                    onViewAllPumpsClick = onNavigateToPumps
                )
            }

            // 3. Material 3 Visual Dashboard - Circular Gauge Chart for "Current Mileage"
            item {
                CircularMileageGauge(
                    currentMileage = state.activeCarMileage,
                    vehicleName = state.activeCar?.name ?: "Active Vehicle"
                )
            }

            // 3b. Gemini AI Mileage & Health Advisor Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showGeminiAdvisorSheet = true }
                        .testTag("home_gemini_advisor_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.2.dp, EmeraldGreen.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(DarkTeal, EmeraldGreen)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Gemini AI CNG Advisor",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Active",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = EmeraldGreen,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Smart mileage tuning & engine health audit",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkTeal
                            ) {
                                Text(
                                    text = "Open Tips",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 Quick Tip: Fill early in the morning at 200+ Bar for +10% denser gas, and keep spark plug gaps calibrated to 0.75 mm.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Material 3 Visual Dashboard - Bar Chart for "Weekly Expenses"
            item {
                WeeklyExpensesBarChart(
                    refills = refillsList
                )
            }

            // 5. Quick Actions (🚗 My Vehicle, ⛽ Find Pump, 💰 Add Expense, 🧾 Add Refill)
            item {
                DriverQuickActionsGrid(
                    onFindPumpClick = onNavigateToPumps,
                    onNavigateClick = {
                        state.nearestPump?.let { pump ->
                            com.example.util.NavigationUtil.navigateToPump(
                                context = context,
                                latitude = pump.latitude,
                                longitude = pump.longitude,
                                pumpName = pump.name
                            )
                        } ?: onNavigateToPumps()
                    },
                    onAddRefillClick = { showQuickRefillBottomSheet = true },
                    onAddVehicleClick = onNavigateToCars,
                    onHistoryClick = onNavigateToAddRefill,
                    onExpensesClick = onNavigateToReports
                )
            }

            // 6. Key Statistics Cards (Today's Expense, This Month, Total Refills, Average Mileage)
            item {
                HomeStatisticsGrid(
                    todayExpense = todayExpense,
                    todayKg = todayKg,
                    thisMonthExpense = state.currentMonthExpense,
                    petrolSavings = state.monthlyPetrolSavings,
                    totalRefillsCount = refillsList.size,
                    averageMileage = state.activeCarMileage,
                    onViewExpensesClick = onNavigateToReports
                )
            }

            // 7. Digital Cockpit / Instrument Cluster Card
            item {
                AutomotiveClusterCard(
                    activeCar = state.activeCar,
                    activeCarMileage = state.activeCarMileage,
                    estimatedRangeKm = state.estimatedRangeKm,
                    tankFillPercent = state.tankFillPercent,
                    gasPressureBar = state.averageGasPressure,
                    driverName = settingsState.userName,
                    isDriverLoggedIn = settingsState.isLoggedIn,
                    onSwitchVehicleClick = {
                        if (carsList.isNotEmpty()) {
                            showVehicleSelectDialog = true
                        } else {
                            onNavigateToCars()
                        }
                    }
                )
            }

            // 8. Recent Refills (Latest 3–5 refills with View All button)
            item {
                HomeRecentRefillsSection(
                    recentRefills = refillsList,
                    onViewAllClick = onNavigateToAddRefill,
                    onRefillClick = { refill ->
                        onNavigateToAddRefill()
                    }
                )
            }

            // 9. Monthly Fuel Spending & Petrol Savings Breakdown
            item {
                AutomotiveSavingsCard(
                    currentMonthExpense = state.currentMonthExpense,
                    petrolSavings = state.monthlyPetrolSavings,
                    currentMonthRefillsCount = state.currentMonthRefillsCount,
                    currentMonthCngKg = state.lastRefill?.quantityKg?.times(state.currentMonthRefillsCount) ?: 0.0,
                    onViewDiaryClick = onNavigateToReports,
                    onExportCsvClick = {
                        if (refillsList.isEmpty()) {
                            Toast.makeText(context, "No fueling records available to export", Toast.LENGTH_SHORT).show()
                        } else {
                            val csvContent = CsvExportUtil.buildFuelExpenseCsv(
                                refills = refillsList,
                                cars = carsList,
                                reportTitle = "Monthly CNG Expense Tracker & Fueling Backup"
                            )
                            val fileName = CsvExportUtil.getSuggestedFileName("Monthly_CNG_Expenses")
                            val file = CsvExportUtil.saveCsvToCache(context, csvContent, fileName)
                            if (file != null) {
                                CsvExportUtil.shareCsvFile(context, file, chooserTitle = "Share Monthly CNG Expense Backup")
                            } else {
                                Toast.makeText(context, "Failed to create CSV export", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // 10. Eco Green Impact & Carbon Savings Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_eco_impact_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.ecoImpactTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "100% Clean Green Mobility",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldGreen.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = strings.co2Saved, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", state.monthlyPetrolSavings * 0.12)} kg",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = EmeraldGreen
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DarkTeal.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, DarkTeal.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = strings.treesEquivalent, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${(state.monthlyPetrolSavings * 0.006).toInt() + 3} Trees 🌳",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = DarkTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 11. Regional Language Selector Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_language_module_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language Module",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.languageModuleTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = settingsState.selectedLanguage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.languageModuleDesc,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val quickLanguages = listOf(
                            "English",
                            "Hindi (हिंदी)",
                            "Marathi (मराठी)",
                            "Gujarati (ગુજરાતી)",
                            "Punjabi (ਪੰਜਾਬੀ)",
                            "Tamil (தமிழ்)",
                            "Telugu (తెలుగు)",
                            "Bengali (বাংলা)",
                            "Kannada (ಕನ್ನಡ)",
                            "Spanish (Español)"
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (lang in quickLanguages) {
                                val selected = settingsState.selectedLanguage == lang || (lang.startsWith("English") && settingsState.selectedLanguage == "English")
                                FilterChip(
                                    selected = selected,
                                    onClick = { settingsViewModel.setAppLanguage(lang) },
                                    label = { Text(lang, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Extended Floating Action Button (FAB) that says "Log CNG" and shrinks to an icon on scroll
        ExtendedFloatingActionButton(
            expanded = isFabExpanded,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showQuickRefillBottomSheet = true
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = "Log CNG",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = "Log CNG",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("quick_refill_fab")
        )

        // Swipe-Up ModalBottomSheet for Quick Refill with Custom NumPad & Haptic Feedback
        if (showQuickRefillBottomSheet) {
            DriverRefillBottomSheet(
                cars = carsList,
                pumps = pumpsList,
                existingRefill = null,
                lastOdometerReading = state.activeCar?.currentOdometer ?: 0.0,
                onDismiss = { showQuickRefillBottomSheet = false },
                onSave = { id, carId, pumpId, pumpName, odo, qty, price, total, isFull, notes ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    refillViewModel.saveRefill(id, carId, pumpId, pumpName, odo, qty, price, total, isFull, notes)
                    showQuickRefillBottomSheet = false
                }
            )
        }

        // Gemini AI CNG Advisor & Mileage Optimization Sheet
        if (showGeminiAdvisorSheet) {
            GeminiCngAdvisorSheet(
                activeCar = state.activeCar,
                refills = refillsList,
                onDismiss = { showGeminiAdvisorSheet = false }
            )
        }

        // App Tour Dialog
        if (showAppTourDialog) {
            AppTourDialog(
                onDismiss = {
                    showAppTourDialog = false
                    appPreferences.isAppTourCompleted = true
                }
            )
        }

        // Language Dialog
        if (showLanguageDialog) {
            LanguageSelectorDialog(
                currentLanguage = settingsState.selectedLanguage,
                onLanguageSelected = { lang ->
                    settingsViewModel.setAppLanguage(lang)
                },
                onDismiss = { showLanguageDialog = false }
            )
        }

        // Vehicle Switch Dialog
        if (showVehicleSelectDialog) {
            AlertDialog(
                onDismissRequest = { showVehicleSelectDialog = false },
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select Active Vehicle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        carsList.forEach { car ->
                            val isSelected = state.activeCar?.id == car.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        homeViewModel.switchActiveCar(car.id)
                                        showVehicleSelectDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            homeViewModel.switchActiveCar(car.id)
                                            showVehicleSelectDialog = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = car.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${car.regNumber} • ${car.tankCapacityKg} kg Tank",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showVehicleSelectDialog = false
                            onNavigateToCars()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Manage Garage", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVehicleSelectDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
