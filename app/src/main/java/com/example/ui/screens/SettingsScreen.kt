package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Palette
import com.example.ui.theme.DarkTeal
import com.example.util.LocationService
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.SettingsViewModel

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import com.example.util.LocationManager

import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.LocaleSettingsComponent
import com.example.ui.components.UserProfileSection
import com.example.util.AppLocalization

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    onRequestLocationPermission: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by settingsViewModel.settingsState.collectAsState()
    val strings = remember(state.selectedLanguage) { AppLocalization.getStrings(state.selectedLanguage) }
    val hasLocationPermission = remember { LocationService.getInstance(context).hasPermission() }

    // Export CSV file launcher
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { settingsViewModel.exportDataToCsv(context, it) }
    }

    // Import CSV file launcher
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { settingsViewModel.importDataFromCsv(context, it) }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            settingsViewModel.clearStatusMessage()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Account & Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 1. User Profile & Login / Account Section
        item {
            UserProfileSection(
                state = state,
                onSignInWithEmail = { email, password, onResult ->
                    settingsViewModel.signInWithEmail(email, password, onResult)
                },
                onSignUpWithEmail = { name, email, password, phone, onResult ->
                    settingsViewModel.signUpWithEmail(name, email, password, phone, onResult)
                },
                onSignInWithGoogle = {
                    settingsViewModel.signInWithGoogle(context)
                },
                onSendPasswordReset = { email ->
                    settingsViewModel.sendPasswordReset(email)
                },
                onUpdateProfile = { name, email, phone ->
                    settingsViewModel.updateProfile(name, email, phone)
                },
                onSyncCloud = {
                    settingsViewModel.syncToCloud()
                },
                onRestoreCloud = {
                    settingsViewModel.restoreFromCloud()
                },
                onTestConnection = {
                    settingsViewModel.testFirestoreConnection()
                },
                onToggleAutoSync = { enabled ->
                    settingsViewModel.toggleFirestoreAutoSync(enabled)
                },
                onToggleCommunitySync = { enabled ->
                    settingsViewModel.toggleFirestoreCommunitySync(enabled)
                },
                onLogout = {
                    settingsViewModel.logout(context)
                },
                onNavigateToAuth = onNavigateToAuth
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Import / Export Backup Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Backup & Export",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Export your refill diary, vehicle logs, and expense reports to Excel-compatible CSV format.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { exportCsvLauncher.launch("CNG_Expense_Backup.csv") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontSize = 12.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedButton(
                            onClick = { importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv")) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_csv_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import CSV", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Language & Locale Preferences Component
        item {
            LocaleSettingsComponent(
                onLanguageChanged = { lang ->
                    settingsViewModel.setAppLanguage(lang)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Theme & Display Mode Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = DarkTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Theme & Appearance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Customize application appearance with seamless dark and light modes.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val themes = listOf(
                        Triple("System", "System Default", Icons.Default.BrightnessAuto),
                        Triple("Light", "Light", Icons.Default.Brightness7),
                        Triple("Dark", "Dark", Icons.Default.Brightness4)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themes.forEach { (mode, label, icon) ->
                            val isSelected = state.themeMode.equals(mode, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { settingsViewModel.setThemeMode(mode) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("theme_mode_${mode.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Notification Preferences Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notification Reminders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        title = "Favorite Pump Price Alert",
                        subtitle = "WorkManager background check for price & status updates",
                        checked = state.priceChangeNotify,
                        onCheckedChange = { settingsViewModel.togglePriceChangeNotify(context, it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { settingsViewModel.triggerWorkManagerCheck(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trigger_work_manager_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger WorkManager Status Check Now", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mileage Diary Inactivity Push Notification Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_mileage_diary_reminder"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            SettingToggleRow(
                                title = "Mileage Diary Inactivity Reminder",
                                subtitle = "Push notification when no refills or odometer updates are logged for a set period",
                                checked = state.mileageDiaryReminderEnabled,
                                onCheckedChange = { settingsViewModel.toggleMileageDiaryReminder(context, it) }
                            )

                            if (state.mileageDiaryReminderEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Remind after inactivity period:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val inactivityOptions = listOf(
                                    1 to "1 Day",
                                    2 to "2 Days",
                                    3 to "3 Days (Recommended)",
                                    5 to "5 Days",
                                    7 to "7 Days"
                                )

                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    inactivityOptions.forEach { (days, label) ->
                                        val isSelected = state.mileageInactivityDays == days
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { settingsViewModel.setMileageInactivityDays(context, days) },
                                            label = {
                                                Text(label, fontSize = 11.sp)
                                            },
                                            modifier = Modifier.testTag("chip_inactivity_${days}_days"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EmeraldGreen,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { settingsViewModel.triggerMileageDiaryReminderCheck(context) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_test_mileage_diary_reminder"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = EmeraldGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Send Test Mileage Diary Notification",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingToggleRow(
                        title = "Pump Status Change",
                        subtitle = "Notify when favorite station opens or closes",
                        checked = state.openStatusNotify,
                        onCheckedChange = { settingsViewModel.toggleOpenStatusNotify(it) }
                    )

                    SettingToggleRow(
                        title = "Monthly Expense Reminder",
                        subtitle = "Monthly summary of your CNG budget",
                        checked = state.monthlyReminderNotify,
                        onCheckedChange = { settingsViewModel.toggleMonthlyReminderNotify(it) }
                    )

                    SettingToggleRow(
                        title = "Refill Log Reminder",
                        subtitle = "Remind to record odometer reading after refueling",
                        checked = state.refillReminderNotify,
                        onCheckedChange = { settingsViewModel.toggleRefillReminderNotify(it) }
                    )

                    SettingToggleRow(
                        title = "Weekly Data Backup",
                        subtitle = "Reminder to export CSV backup copy",
                        checked = state.backupReminderNotify,
                        onCheckedChange = { settingsViewModel.toggleBackupReminderNotify(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Privacy & Architecture Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = DarkTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy & Live Data Source", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Local-First Privacy: Your vehicles, refills, and expense data are safely stored on your device using Room SQLite.\n• GPS Protection: Location is used exclusively for distance and nearby station navigation.\n• Live API Architecture: Stations connected to IoT feeds display live pressure. Others are clearly marked as 'Last updated manually'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
