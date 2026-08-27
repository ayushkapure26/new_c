package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Car
import com.example.data.model.FuelRefill
import com.example.data.repository.CngRepository
import com.example.util.AppPreferences
import com.example.util.AuthResult
import com.example.util.CloudSyncState
import com.example.util.FirebaseAuthManager
import com.example.util.FirestoreSyncHelper
import com.example.util.ImportExportManager
import com.example.util.LocaleManager
import com.example.util.SyncSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val priceChangeNotify: Boolean = true,
    val openStatusNotify: Boolean = true,
    val monthlyReminderNotify: Boolean = true,
    val refillReminderNotify: Boolean = true,
    val backupReminderNotify: Boolean = false,
    val mileageDiaryReminderEnabled: Boolean = true,
    val mileageInactivityDays: Int = 3,
    val isFirestoreAutoSyncEnabled: Boolean = true,
    val isFirestoreCommunitySyncEnabled: Boolean = true,
    val selectedCity: String = "Delhi NCR",
    val selectedLanguage: String = "English",
    val themeMode: String = "System",
    val isLoggedIn: Boolean = false,
    val userId: String = "",
    val userName: String = "Driver",
    val userEmail: String = "driver@cngtrack.com",
    val userPhone: String = "+91 98765 43210",
    val photoUrl: String = "",
    val authProvider: String = "password", // "google" or "password"
    val memberSince: String = "August 2026",
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    // Personalized User Mileage & Expense Telemetry
    val personalizedTotalSpend: Double = 0.0,
    val personalizedTotalKm: Double = 0.0,
    val personalizedAverageMileage: Double = 0.0,
    val personalizedRefillCount: Int = 0,
    val monthlyBudgetRs: Double = 5000.0,
    // Cloud Firestore Sync State
    val isCloudSyncing: Boolean = false,
    val lastCloudSyncMessage: String? = null,
    val isCloudSyncSuccess: Boolean = true
)

class SettingsViewModel(
    private val repository: CngRepository,
    private val appPreferences: AppPreferences,
    private val localeManager: LocaleManager
) : ViewModel() {

    private val authManager = FirebaseAuthManager()
    val syncHelper = FirestoreSyncHelper(repository, appPreferences)

    val settingsState = MutableStateFlow(
        SettingsUiState(
            priceChangeNotify = appPreferences.priceChangeNotify,
            openStatusNotify = appPreferences.openStatusNotify,
            monthlyReminderNotify = appPreferences.monthlyReminderNotify,
            refillReminderNotify = appPreferences.refillReminderNotify,
            backupReminderNotify = appPreferences.backupReminderNotify,
            mileageDiaryReminderEnabled = appPreferences.mileageDiaryReminderEnabled,
            mileageInactivityDays = appPreferences.mileageInactivityDays,
            isFirestoreAutoSyncEnabled = appPreferences.isFirestoreAutoSyncEnabled,
            isFirestoreCommunitySyncEnabled = appPreferences.isFirestoreCommunitySyncEnabled,
            selectedCity = appPreferences.selectedCity,
            selectedLanguage = localeManager.selectedLanguageState.value,
            themeMode = appPreferences.themeMode,
            isLoggedIn = appPreferences.isLoggedIn || authManager.isUserLoggedIn,
            userId = appPreferences.userId.ifBlank { authManager.currentUser?.uid ?: "" },
            userName = authManager.currentUser?.displayName ?: appPreferences.userName,
            userEmail = authManager.currentUser?.email ?: appPreferences.userEmail,
            userPhone = authManager.currentUser?.phone ?: appPreferences.userPhone,
            memberSince = appPreferences.memberSince,
            monthlyBudgetRs = appPreferences.monthlyBudget,
            lastCloudSyncMessage = syncHelper.syncState.value.lastSyncMessage
        )
    )

    init {
        viewModelScope.launch {
            localeManager.selectedLanguageFlow.collectLatest { lang ->
                settingsState.value = settingsState.value.copy(
                    selectedLanguage = lang
                )
            }
        }

        viewModelScope.launch {
            syncHelper.syncState.collectLatest { cloudState ->
                settingsState.value = settingsState.value.copy(
                    isCloudSyncing = cloudState.isSyncing,
                    lastCloudSyncMessage = cloudState.lastSyncMessage ?: cloudState.errorMessage,
                    isCloudSyncSuccess = cloudState.isSuccess
                )
            }
        }

        // Combine refills and cars to calculate personalized mileage and expense totals
        viewModelScope.launch {
            combine(repository.allRefills, repository.allCars) { refills: List<FuelRefill>, cars: List<Car> ->
                val totalSpend = refills.sumOf { it.totalAmount }
                val totalQuantity = refills.sumOf { it.quantityKg }
                val totalKm = if (cars.isNotEmpty()) {
                    cars.sumOf { it.currentOdometer }
                } else {
                    refills.maxOfOrNull { it.odometer } ?: 0.0
                }

                val avgMileage = if (totalQuantity > 0 && totalKm > 0) {
                    val firstOdo = refills.minOfOrNull { it.odometer } ?: 0.0
                    val lastOdo = refills.maxOfOrNull { it.odometer } ?: 0.0
                    val trackedDist = if (lastOdo > firstOdo) lastOdo - firstOdo else 0.0
                    if (trackedDist > 0) trackedDist / totalQuantity else (totalKm / (refills.size * 8.5).coerceAtLeast(1.0))
                } else {
                    24.5 // Standard default CNG mileage
                }

                Pair(totalSpend, Triple(totalKm, avgMileage, refills.size))
            }.collectLatest { dataPair ->
                val spend = dataPair.first
                val km = dataPair.second.first
                val mileage = dataPair.second.second
                val count = dataPair.second.third
                settingsState.value = settingsState.value.copy(
                    personalizedTotalSpend = spend,
                    personalizedTotalKm = km,
                    personalizedAverageMileage = mileage,
                    personalizedRefillCount = count
                )
            }
        }
    }

    fun setThemeMode(mode: String) {
        appPreferences.themeMode = mode
        settingsState.value = settingsState.value.copy(
            themeMode = mode,
            statusMessage = "Theme set to $mode Mode",
            isError = false
        )
    }

    fun setAppLanguage(language: String) {
        appPreferences.appLanguage = language
        viewModelScope.launch {
            localeManager.setLanguage(language)
        }
        settingsState.value = settingsState.value.copy(
            selectedLanguage = language,
            statusMessage = "Language set to $language",
            isError = false
        )
    }

    fun togglePriceChangeNotify(context: Context, enabled: Boolean) {
        appPreferences.priceChangeNotify = enabled
        settingsState.value = settingsState.value.copy(priceChangeNotify = enabled)
        if (enabled) {
            com.example.worker.FavoritePumpsWorker.schedulePeriodicCheck(context)
        } else {
            com.example.worker.FavoritePumpsWorker.cancelPeriodicCheck(context)
        }
    }

    fun triggerWorkManagerCheck(context: Context) {
        com.example.worker.FavoritePumpsWorker.triggerImmediateCheck(context)
        settingsState.value = settingsState.value.copy(
            statusMessage = "WorkManager background sync triggered! Notification sent for favorite pump changes.",
            isError = false
        )
    }

    fun toggleOpenStatusNotify(enabled: Boolean) {
        appPreferences.openStatusNotify = enabled
        settingsState.value = settingsState.value.copy(openStatusNotify = enabled)
    }

    fun toggleMonthlyReminderNotify(enabled: Boolean) {
        appPreferences.monthlyReminderNotify = enabled
        settingsState.value = settingsState.value.copy(monthlyReminderNotify = enabled)
    }

    fun toggleRefillReminderNotify(enabled: Boolean) {
        appPreferences.refillReminderNotify = enabled
        settingsState.value = settingsState.value.copy(refillReminderNotify = enabled)
    }

    fun toggleBackupReminderNotify(enabled: Boolean) {
        appPreferences.backupReminderNotify = enabled
        settingsState.value = settingsState.value.copy(backupReminderNotify = enabled)
    }

    fun toggleMileageDiaryReminder(context: Context, enabled: Boolean) {
        appPreferences.mileageDiaryReminderEnabled = enabled
        settingsState.value = settingsState.value.copy(
            mileageDiaryReminderEnabled = enabled,
            statusMessage = if (enabled) "Mileage diary inactivity reminder enabled (${appPreferences.mileageInactivityDays} days)" else "Mileage diary reminders disabled",
            isError = false
        )
        if (enabled) {
            com.example.worker.MileageDiaryReminderWorker.schedulePeriodicCheck(context)
        } else {
            com.example.worker.MileageDiaryReminderWorker.cancelPeriodicCheck(context)
        }
    }

    fun setMileageInactivityDays(context: Context, days: Int) {
        val validDays = days.coerceIn(1, 30)
        appPreferences.mileageInactivityDays = validDays
        settingsState.value = settingsState.value.copy(
            mileageInactivityDays = validDays,
            statusMessage = "Inactivity reminder set to $validDays days without logging",
            isError = false
        )
        if (appPreferences.mileageDiaryReminderEnabled) {
            com.example.worker.MileageDiaryReminderWorker.schedulePeriodicCheck(context)
        }
    }

    fun triggerMileageDiaryReminderCheck(context: Context) {
        com.example.worker.MileageDiaryReminderWorker.triggerImmediateCheck(context)
        settingsState.value = settingsState.value.copy(
            statusMessage = "Sent test mileage diary inactivity notification reminder! Check notification bar.",
            isError = false
        )
    }

    fun exportDataToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            val refills = repository.allRefills.first()
            val cars = repository.allCars.first()

            val success = ImportExportManager.exportDatabaseToCsv(context, uri, cars, refills)
            if (success) {
                settingsState.value = settingsState.value.copy(
                    statusMessage = "Successfully exported ${cars.size} vehicles & ${refills.size} refills to CSV backup!",
                    isError = false
                )
            } else {
                settingsState.value = settingsState.value.copy(
                    statusMessage = "Failed to export CSV backup file.",
                    isError = true
                )
            }
        }
    }

    fun importDataFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            val existingCars = repository.allCars.first()
            val (parsedData, result) = ImportExportManager.importDatabaseFromCsv(context, uri)

            if (result.isSuccess) {
                val carIdMap = mutableMapOf<Long, Long>() // oldCsvCarId -> newRoomCarId

                // 1. Import or sync Cars into Room
                for (car in parsedData.cars) {
                    val matchingCar = existingCars.find {
                        it.regNumber.equals(car.regNumber, ignoreCase = true) ||
                        it.name.equals(car.name, ignoreCase = true)
                    }

                    val savedCarId = if (matchingCar != null) {
                        matchingCar.id
                    } else {
                        repository.saveCar(car.copy(id = 0L))
                    }

                    if (car.id != 0L) {
                        carIdMap[car.id] = savedCarId
                    }
                }

                // Default car fallback if needed
                val updatedCars = repository.allCars.first()
                var defaultCarId = updatedCars.firstOrNull { it.isDefault }?.id ?: updatedCars.firstOrNull()?.id ?: 0L

                if (defaultCarId == 0L) {
                    defaultCarId = repository.saveCar(
                        Car(
                            name = "My CNG Car",
                            regNumber = "MH 01 AB 0000",
                            tankCapacityKg = 10.0,
                            currentOdometer = 1000.0,
                            isDefault = true
                        )
                    )
                }

                // 2. Import Refills into Room
                for (refill in parsedData.refills) {
                    val mappedCarId = carIdMap[refill.carId] ?: defaultCarId
                    repository.saveRefill(
                        refill.copy(id = 0L, carId = mappedCarId),
                        updateCarOdometer = true
                    )
                }

                val msg = if (result.importedCarsCount > 0) {
                    "Successfully restored ${result.importedCarsCount} vehicles & ${result.importedRefillsCount} refills from CSV!"
                } else {
                    "Successfully restored ${result.importedRefillsCount} refills from CSV!"
                }

                settingsState.value = settingsState.value.copy(
                    statusMessage = msg,
                    isError = false
                )
            } else {
                settingsState.value = settingsState.value.copy(
                    statusMessage = result.errorMessage,
                    isError = true
                )
            }
        }
    }

    fun signInWithEmail(email: String, password: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            settingsState.value = settingsState.value.copy(isAuthLoading = true, authErrorMessage = null)
            val result = authManager.signInWithEmailAndPassword(email, password)
            when (result) {
                is AuthResult.Success -> {
                    val user = result.user
                    appPreferences.isLoggedIn = true
                    appPreferences.userId = user.uid
                    appPreferences.userName = user.displayName
                    appPreferences.userEmail = user.email
                    if (user.phone.isNotBlank()) appPreferences.userPhone = user.phone

                    settingsState.value = settingsState.value.copy(
                        isLoggedIn = true,
                        userId = user.uid,
                        userName = user.displayName,
                        userEmail = user.email,
                        userPhone = if (user.phone.isNotBlank()) user.phone else settingsState.value.userPhone,
                        photoUrl = user.photoUrl,
                        authProvider = user.provider,
                        isAuthLoading = false,
                        authErrorMessage = null,
                        statusMessage = result.message,
                        isError = false
                    )
                    onResult(true)
                    syncToCloud()
                }
                is AuthResult.Error -> {
                    settingsState.value = settingsState.value.copy(
                        isAuthLoading = false,
                        authErrorMessage = result.errorMessage,
                        statusMessage = result.errorMessage,
                        isError = true
                    )
                    onResult(false)
                }
                AuthResult.Loading -> {
                    settingsState.value = settingsState.value.copy(isAuthLoading = true)
                }
            }
        }
    }

    fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        phone: String = "",
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            settingsState.value = settingsState.value.copy(isAuthLoading = true, authErrorMessage = null)
            val result = authManager.createUserWithEmailAndPassword(
                email = email,
                password = password,
                displayName = name,
                phone = phone
            )
            when (result) {
                is AuthResult.Success -> {
                    val user = result.user
                    appPreferences.isLoggedIn = true
                    appPreferences.userId = user.uid
                    appPreferences.userName = user.displayName
                    appPreferences.userEmail = user.email
                    if (phone.isNotBlank()) appPreferences.userPhone = phone

                    settingsState.value = settingsState.value.copy(
                        isLoggedIn = true,
                        userId = user.uid,
                        userName = user.displayName,
                        userEmail = user.email,
                        userPhone = if (phone.isNotBlank()) phone else settingsState.value.userPhone,
                        photoUrl = user.photoUrl,
                        authProvider = user.provider,
                        isAuthLoading = false,
                        authErrorMessage = null,
                        statusMessage = result.message,
                        isError = false
                    )
                    onResult(true)
                    syncToCloud()
                }
                is AuthResult.Error -> {
                    settingsState.value = settingsState.value.copy(
                        isAuthLoading = false,
                        authErrorMessage = result.errorMessage,
                        statusMessage = result.errorMessage,
                        isError = true
                    )
                    onResult(false)
                }
                AuthResult.Loading -> {
                    settingsState.value = settingsState.value.copy(isAuthLoading = true)
                }
            }
        }
    }

    fun signInWithGoogle(
        context: Context,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            settingsState.value = settingsState.value.copy(isAuthLoading = true, authErrorMessage = null)
            val result = authManager.signInWithGoogle(context)
            when (result) {
                is AuthResult.Success -> {
                    val user = result.user
                    appPreferences.isLoggedIn = true
                    appPreferences.userId = user.uid
                    appPreferences.userName = user.displayName
                    appPreferences.userEmail = user.email
                    if (user.phone.isNotBlank()) appPreferences.userPhone = user.phone

                    settingsState.value = settingsState.value.copy(
                        isLoggedIn = true,
                        userId = user.uid,
                        userName = user.displayName,
                        userEmail = user.email,
                        userPhone = if (user.phone.isNotBlank()) user.phone else settingsState.value.userPhone,
                        photoUrl = user.photoUrl,
                        authProvider = "google",
                        isAuthLoading = false,
                        authErrorMessage = null,
                        statusMessage = result.message,
                        isError = false
                    )
                    onResult(true)
                    syncToCloud()
                }
                is AuthResult.Error -> {
                    settingsState.value = settingsState.value.copy(
                        isAuthLoading = false,
                        authErrorMessage = result.errorMessage,
                        statusMessage = result.errorMessage,
                        isError = true
                    )
                    onResult(false)
                }
                AuthResult.Loading -> {
                    settingsState.value = settingsState.value.copy(isAuthLoading = true)
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            val res = authManager.sendPasswordResetEmail(email)
            res.onSuccess { msg ->
                settingsState.value = settingsState.value.copy(
                    statusMessage = msg,
                    authErrorMessage = null,
                    isError = false
                )
            }.onFailure { err ->
                settingsState.value = settingsState.value.copy(
                    statusMessage = err.localizedMessage ?: "Failed to send reset email",
                    authErrorMessage = err.localizedMessage,
                    isError = true
                )
            }
        }
    }

    fun login(name: String, email: String, phone: String = "") {
        val trimmedName = name.trim().ifEmpty { "CNG Driver" }
        val trimmedEmail = email.trim().ifEmpty { "driver@cngtrack.com" }
        val trimmedPhone = phone.trim().ifEmpty { "+91 98765 43210" }

        appPreferences.isLoggedIn = true
        appPreferences.userName = trimmedName
        appPreferences.userEmail = trimmedEmail
        appPreferences.userPhone = trimmedPhone

        settingsState.value = settingsState.value.copy(
            isLoggedIn = true,
            userName = trimmedName,
            userEmail = trimmedEmail,
            userPhone = trimmedPhone,
            statusMessage = "Welcome back, $trimmedName! Profile personalized.",
            isError = false
        )
    }

    fun updateProfile(name: String, email: String, phone: String) {
        val trimmedName = name.trim().ifEmpty { settingsState.value.userName }
        val trimmedEmail = email.trim().ifEmpty { settingsState.value.userEmail }
        val trimmedPhone = phone.trim().ifEmpty { settingsState.value.userPhone }

        appPreferences.userName = trimmedName
        appPreferences.userEmail = trimmedEmail
        appPreferences.userPhone = trimmedPhone

        settingsState.value = settingsState.value.copy(
            userName = trimmedName,
            userEmail = trimmedEmail,
            userPhone = trimmedPhone,
            statusMessage = "Profile details updated successfully!",
            isError = false
        )
    }

    fun toggleFirestoreAutoSync(enabled: Boolean) {
        appPreferences.isFirestoreAutoSyncEnabled = enabled
        settingsState.value = settingsState.value.copy(
            isFirestoreAutoSyncEnabled = enabled,
            statusMessage = if (enabled) "Firestore Auto-Sync enabled" else "Firestore Auto-Sync disabled",
            isError = false
        )
    }

    fun toggleFirestoreCommunitySync(enabled: Boolean) {
        appPreferences.isFirestoreCommunitySyncEnabled = enabled
        settingsState.value = settingsState.value.copy(
            isFirestoreCommunitySyncEnabled = enabled,
            statusMessage = if (enabled) "Community Live CNG Sync enabled" else "Community Live CNG Sync disabled",
            isError = false
        )
    }

    fun testFirestoreConnection() {
        viewModelScope.launch {
            settingsState.value = settingsState.value.copy(isCloudSyncing = true)
            val result = syncHelper.testFirestoreConnection()
            result.onSuccess { msg ->
                settingsState.value = settingsState.value.copy(
                    isCloudSyncing = false,
                    lastCloudSyncMessage = msg,
                    statusMessage = msg,
                    isError = false
                )
            }.onFailure { err ->
                settingsState.value = settingsState.value.copy(
                    isCloudSyncing = false,
                    lastCloudSyncMessage = err.localizedMessage,
                    statusMessage = err.localizedMessage ?: "Connection test failed",
                    isError = true
                )
            }
        }
    }

    fun syncToCloud() {
        viewModelScope.launch {
            val result = syncHelper.syncLocalDataToCloud()
            result.onSuccess { summary ->
                settingsState.value = settingsState.value.copy(
                    statusMessage = summary.message,
                    isError = false
                )
            }.onFailure { err ->
                settingsState.value = settingsState.value.copy(
                    statusMessage = err.localizedMessage ?: "Failed to sync to Firestore cloud",
                    isError = true
                )
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            val result = syncHelper.restoreCloudDataToLocal()
            result.onSuccess { summary ->
                settingsState.value = settingsState.value.copy(
                    statusMessage = summary.message,
                    isError = false
                )
            }.onFailure { err ->
                settingsState.value = settingsState.value.copy(
                    statusMessage = err.localizedMessage ?: "Failed to restore from Firestore cloud",
                    isError = true
                )
            }
        }
    }

    fun logout(context: Context? = null) {
        viewModelScope.launch {
            authManager.signOut(context)
            appPreferences.isLoggedIn = false
            appPreferences.userId = ""
            settingsState.value = settingsState.value.copy(
                isLoggedIn = false,
                userId = "",
                statusMessage = "Signed out from account. Switched to local offline mode.",
                isError = false
            )
        }
    }

    fun clearStatusMessage() {
        settingsState.value = settingsState.value.copy(statusMessage = null, authErrorMessage = null)
    }

    class Factory(
        private val repository: CngRepository,
        private val appPreferences: AppPreferences,
        private val localeManager: LocaleManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, appPreferences, localeManager) as T
        }
    }
}
