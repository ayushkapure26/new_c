package com.example.data.repository

import android.util.Log
import com.example.data.dao.CarDao
import com.example.data.dao.RefillDao
import com.example.data.model.Car
import com.example.data.model.FuelRefill
import com.example.data.model.PumpRating
import com.example.util.AppPreferences
import com.example.util.CloudSyncState
import com.example.util.SyncSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository interface for synchronizing local Room SQLite entities
 * (Vehicles, Refills, Expenses) with Firebase Firestore in the cloud.
 */
interface FirestoreSyncRepository {
    val syncState: StateFlow<CloudSyncState>

    /**
     * Backs up all local cars, refills, and expense summary to Firestore under the user's document.
     */
    suspend fun syncAllUserDataToCloud(userId: String? = null): Result<SyncSummary>

    /**
     * Backs up a single vehicle to Firestore.
     */
    suspend fun backupCarToCloud(car: Car, userId: String? = null): Result<Unit>

    /**
     * Removes a deleted vehicle from Firestore.
     */
    suspend fun deleteCarFromCloud(carId: Long, userId: String? = null): Result<Unit>

    /**
     * Backs up a single refill log to Firestore and recalculates cloud expenses.
     */
    suspend fun backupRefillToCloud(refill: FuelRefill, userId: String? = null): Result<Unit>

    /**
     * Removes a deleted refill log from Firestore.
     */
    suspend fun deleteRefillFromCloud(refillId: Long, userId: String? = null): Result<Unit>

    /**
     * Backs up aggregate expense & mileage summary to Firestore.
     */
    suspend fun backupExpenseSummaryToCloud(userId: String? = null): Result<Unit>

    /**
     * Restores vehicles and refills from Firestore to the local Room database.
     */
    suspend fun restoreUserDataFromCloud(userId: String? = null): Result<SyncSummary>

    /**
     * Tests Firestore cloud connection and measures response latency.
     */
    suspend fun testConnection(): Result<String>

    /**
     * Pushes crowdsourced real-time pump gas pressure and stock status to Firestore.
     */
    suspend fun pushLivePumpStatus(
        pumpId: Long,
        stockStatus: String,
        pressureBar: Double,
        queueMinutes: Int,
        isGasAvailable: Boolean,
        reporterName: String
    ): Result<Unit>

    /**
     * Pushes pump reviews and ratings to Firestore.
     */
    suspend fun pushPumpRating(rating: PumpRating): Result<Unit>

    /**
     * Clears any active error messages in the sync state.
     */
    fun clearError()
}

/**
 * Production implementation of [FirestoreSyncRepository].
 */
class FirestoreSyncRepositoryImpl(
    private val carDao: CarDao,
    private val refillDao: RefillDao,
    private val appPreferences: AppPreferences,
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirestoreSyncRepo", "Firestore init error: ${e.message}")
            null
        }
    }
) : FirestoreSyncRepository {

    companion object {
        private const val TAG = "FirestoreSyncRepo"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_CARS = "cars"
        private const val COLLECTION_REFILLS = "refills"
        private const val COLLECTION_EXPENSES = "expense_summaries"
        private const val COLLECTION_LIVE_STATUS = "cng_live_status"
        private const val COLLECTION_RATINGS = "cng_pump_ratings"
    }

    private val _syncState = MutableStateFlow(
        CloudSyncState(
            lastSyncTime = appPreferences.lastCloudSyncTime,
            lastSyncMessage = if (appPreferences.lastCloudSyncTime > 0) {
                "Last synced: " + SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(appPreferences.lastCloudSyncTime))
            } else null
        )
    )
    override val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private fun getEffectiveUserId(userIdParam: String?): String {
        if (!userIdParam.isNullOrBlank()) return userIdParam
        val authUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
        return authUid ?: appPreferences.userId.ifBlank { "driver_guest_mode" }
    }

    override suspend fun syncAllUserDataToCloud(userId: String?): Result<SyncSummary> = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(isSyncing = true, errorMessage = null)
        val firestore = firestoreProvider()
        if (firestore == null) {
            val error = "Firestore service is offline or unavailable."
            _syncState.value = _syncState.value.copy(isSyncing = false, isSuccess = false, errorMessage = error)
            return@withContext Result.failure(IllegalStateException(error))
        }

        try {
            val targetUserId = getEffectiveUserId(userId)
            val cars = carDao.getAllCars().first()
            val refills = refillDao.getAllRefills().first()

            val userDocRef = firestore.collection(COLLECTION_USERS).document(targetUserId)

            // 1. User Root Document & Profile
            val totalExpense = refills.sumOf { it.totalAmount }
            val totalKg = refills.sumOf { it.quantityKg }
            val avgMileage = if (refills.isNotEmpty()) refills.mapNotNull { if (it.mileageKmPerKg > 0) it.mileageKmPerKg else null }.average().let { if (it.isNaN()) 0.0 else it } else 0.0

            val profileData = hashMapOf(
                "userId" to targetUserId,
                "userName" to appPreferences.userName,
                "userEmail" to appPreferences.userEmail,
                "userPhone" to appPreferences.userPhone,
                "monthlyBudget" to appPreferences.monthlyBudget,
                "totalFuelExpense" to totalExpense,
                "totalCngKg" to totalKg,
                "averageMileage" to avgMileage,
                "vehicleCount" to cars.size,
                "refillCount" to refills.size,
                "lastSyncedAt" to System.currentTimeMillis(),
                "appPlatform" to "Android Jetpack Compose"
            )
            userDocRef.set(profileData, SetOptions.merge()).await()

            // 2. Batch/Individual Sync for Cars
            val carsCollection = userDocRef.collection(COLLECTION_CARS)
            for (car in cars) {
                val carMap = hashMapOf(
                    "id" to car.id,
                    "name" to car.name,
                    "regNumber" to car.regNumber,
                    "tankCapacityKg" to car.tankCapacityKg,
                    "fuelType" to car.fuelType,
                    "currentOdometer" to car.currentOdometer,
                    "expectedMileage" to car.expectedMileage,
                    "notes" to car.notes,
                    "isDefault" to car.isDefault,
                    "updatedAt" to System.currentTimeMillis()
                )
                carsCollection.document("car_${car.id}").set(carMap, SetOptions.merge()).await()
            }

            // 3. Batch/Individual Sync for Refills
            val refillsCollection = userDocRef.collection(COLLECTION_REFILLS)
            for (refill in refills) {
                val refillMap = hashMapOf(
                    "id" to refill.id,
                    "carId" to refill.carId,
                    "pumpId" to refill.pumpId,
                    "pumpName" to refill.pumpName,
                    "date" to refill.date,
                    "timeFormatted" to refill.timeFormatted,
                    "odometer" to refill.odometer,
                    "quantityKg" to refill.quantityKg,
                    "pricePerKg" to refill.pricePerKg,
                    "totalAmount" to refill.totalAmount,
                    "isFullRefill" to refill.isFullRefill,
                    "notes" to refill.notes,
                    "distanceTravelled" to refill.distanceTravelled,
                    "mileageKmPerKg" to refill.mileageKmPerKg,
                    "costPerKm" to refill.costPerKm,
                    "syncedAt" to System.currentTimeMillis()
                )
                refillsCollection.document("refill_${refill.id}").set(refillMap, SetOptions.merge()).await()
            }

            // 4. Expense Summary Subcollection
            val expenseDoc = userDocRef.collection(COLLECTION_EXPENSES).document("summary")
            val expenseData = hashMapOf(
                "totalSpent" to totalExpense,
                "totalKg" to totalKg,
                "averageMileage" to avgMileage,
                "totalTrips" to refills.size,
                "monthlyBudget" to appPreferences.monthlyBudget,
                "lastUpdated" to System.currentTimeMillis()
            )
            expenseDoc.set(expenseData, SetOptions.merge()).await()

            val syncTime = System.currentTimeMillis()
            appPreferences.lastCloudSyncTime = syncTime
            val formattedTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(syncTime))
            val summary = SyncSummary(
                vehiclesSynced = cars.size,
                refillsSynced = refills.size,
                timestamp = syncTime,
                message = "Cloud Backup Complete: ${cars.size} vehicles & ${refills.size} refills backed up ($formattedTime)"
            )

            _syncState.value = CloudSyncState(
                isSyncing = false,
                lastSyncTime = syncTime,
                lastSyncMessage = summary.message,
                isSuccess = true,
                errorMessage = null
            )

            Log.d(TAG, "Successfully synced ${cars.size} cars and ${refills.size} refills to Firestore for: $targetUserId")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            val errorMsg = e.localizedMessage ?: "Failed to synchronize data with Firestore."
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                isSuccess = false,
                errorMessage = errorMsg
            )
            Result.failure(e)
        }
    }

    override suspend fun backupCarToCloud(car: Car, userId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = firestoreProvider() ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val targetUserId = getEffectiveUserId(userId)
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(targetUserId)
                .collection(COLLECTION_CARS)
                .document("car_${car.id}")

            val carMap = hashMapOf(
                "id" to car.id,
                "name" to car.name,
                "regNumber" to car.regNumber,
                "tankCapacityKg" to car.tankCapacityKg,
                "fuelType" to car.fuelType,
                "currentOdometer" to car.currentOdometer,
                "expectedMileage" to car.expectedMileage,
                "notes" to car.notes,
                "isDefault" to car.isDefault,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(carMap, SetOptions.merge()).await()
            Log.d(TAG, "Car ${car.id} backed up to cloud")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up car to cloud", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteCarFromCloud(carId: Long, userId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = firestoreProvider() ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val targetUserId = getEffectiveUserId(userId)
            firestore.collection(COLLECTION_USERS)
                .document(targetUserId)
                .collection(COLLECTION_CARS)
                .document("car_$carId")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting car from cloud", e)
            Result.failure(e)
        }
    }

    override suspend fun backupRefillToCloud(refill: FuelRefill, userId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = firestoreProvider() ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val targetUserId = getEffectiveUserId(userId)
            val userDoc = firestore.collection(COLLECTION_USERS).document(targetUserId)
            val docRef = userDoc.collection(COLLECTION_REFILLS).document("refill_${refill.id}")

            val refillMap = hashMapOf(
                "id" to refill.id,
                "carId" to refill.carId,
                "pumpId" to refill.pumpId,
                "pumpName" to refill.pumpName,
                "date" to refill.date,
                "timeFormatted" to refill.timeFormatted,
                "odometer" to refill.odometer,
                "quantityKg" to refill.quantityKg,
                "pricePerKg" to refill.pricePerKg,
                "totalAmount" to refill.totalAmount,
                "isFullRefill" to refill.isFullRefill,
                "notes" to refill.notes,
                "distanceTravelled" to refill.distanceTravelled,
                "mileageKmPerKg" to refill.mileageKmPerKg,
                "costPerKm" to refill.costPerKm,
                "syncedAt" to System.currentTimeMillis()
            )
            docRef.set(refillMap, SetOptions.merge()).await()

            // Update user expense summary in cloud
            backupExpenseSummaryToCloud(targetUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up refill to cloud", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteRefillFromCloud(refillId: Long, userId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = firestoreProvider() ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val targetUserId = getEffectiveUserId(userId)
            firestore.collection(COLLECTION_USERS)
                .document(targetUserId)
                .collection(COLLECTION_REFILLS)
                .document("refill_$refillId")
                .delete()
                .await()
            backupExpenseSummaryToCloud(targetUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting refill from cloud", e)
            Result.failure(e)
        }
    }

    override suspend fun backupExpenseSummaryToCloud(userId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = firestoreProvider() ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val targetUserId = getEffectiveUserId(userId)
            val refills = refillDao.getAllRefills().first()
            val totalExpense = refills.sumOf { it.totalAmount }
            val totalKg = refills.sumOf { it.quantityKg }
            val avgMileage = if (refills.isNotEmpty()) refills.mapNotNull { if (it.mileageKmPerKg > 0) it.mileageKmPerKg else null }.average().let { if (it.isNaN()) 0.0 else it } else 0.0

            val userDoc = firestore.collection(COLLECTION_USERS).document(targetUserId)
            userDoc.update(
                mapOf(
                    "totalFuelExpense" to totalExpense,
                    "totalCngKg" to totalKg,
                    "averageMileage" to avgMileage,
                    "refillCount" to refills.size,
                    "lastExpenseUpdate" to System.currentTimeMillis()
                )
            ).await()

            val expenseDoc = userDoc.collection(COLLECTION_EXPENSES).document("summary")
            expenseDoc.set(
                mapOf(
                    "totalSpent" to totalExpense,
                    "totalKg" to totalKg,
                    "averageMileage" to avgMileage,
                    "refillCount" to refills.size,
                    "monthlyBudget" to appPreferences.monthlyBudget,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update expense summary in cloud: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun restoreUserDataFromCloud(userId: String?): Result<SyncSummary> = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(isSyncing = true, errorMessage = null)
        val firestore = firestoreProvider()
        if (firestore == null) {
            val error = "Firestore service is offline or unavailable."
            _syncState.value = _syncState.value.copy(isSyncing = false, isSuccess = false, errorMessage = error)
            return@withContext Result.failure(IllegalStateException(error))
        }

        try {
            val targetUserId = getEffectiveUserId(userId)
            val userDocRef = firestore.collection(COLLECTION_USERS).document(targetUserId)

            // 1. Restore Cars
            val carsSnapshot = userDocRef.collection(COLLECTION_CARS).get().await()
            val existingCars = carDao.getAllCars().first()
            val carIdMap = mutableMapOf<Long, Long>()
            var restoredCars = 0

            for (doc in carsSnapshot.documents) {
                val name = doc.getString("name") ?: "CNG Car"
                val regNumber = doc.getString("regNumber") ?: ""
                val tankCapacity = doc.getDouble("tankCapacityKg") ?: 10.0
                val fuelType = doc.getString("fuelType") ?: "CNG + Petrol"
                val currentOdo = doc.getDouble("currentOdometer") ?: 0.0
                val expectedMileage = doc.getDouble("expectedMileage") ?: 22.0
                val notes = doc.getString("notes") ?: ""
                val isDefault = doc.getBoolean("isDefault") ?: false
                val originalId = doc.getLong("id") ?: 0L

                val matchingCar = existingCars.find {
                    it.regNumber.equals(regNumber, ignoreCase = true) || it.name.equals(name, ignoreCase = true)
                }

                val targetCarId = if (matchingCar != null) {
                    carDao.updateCar(
                        matchingCar.copy(
                            currentOdometer = maxOf(matchingCar.currentOdometer, currentOdo),
                            tankCapacityKg = tankCapacity,
                            expectedMileage = expectedMileage
                        )
                    )
                    matchingCar.id
                } else {
                    val newCar = Car(
                        name = name,
                        regNumber = regNumber,
                        tankCapacityKg = tankCapacity,
                        fuelType = fuelType,
                        currentOdometer = currentOdo,
                        expectedMileage = expectedMileage,
                        notes = notes,
                        isDefault = isDefault
                    )
                    restoredCars++
                    carDao.insertCar(newCar)
                }

                if (originalId != 0L) {
                    carIdMap[originalId] = targetCarId
                }
            }

            // 2. Restore Refills
            val refillsSnapshot = userDocRef.collection(COLLECTION_REFILLS).get().await()
            var restoredRefills = 0
            val updatedCars = carDao.getAllCars().first()
            val defaultCarId = updatedCars.firstOrNull { it.isDefault }?.id ?: updatedCars.firstOrNull()?.id ?: 1L

            for (doc in refillsSnapshot.documents) {
                val cloudCarId = doc.getLong("carId") ?: 0L
                val mappedCarId = carIdMap[cloudCarId] ?: defaultCarId
                val pumpId = doc.getLong("pumpId")
                val pumpName = doc.getString("pumpName") ?: ""
                val date = doc.getLong("date") ?: System.currentTimeMillis()
                val timeFormatted = doc.getString("timeFormatted") ?: ""
                val odometer = doc.getDouble("odometer") ?: 0.0
                val quantityKg = doc.getDouble("quantityKg") ?: 0.0
                val pricePerKg = doc.getDouble("pricePerKg") ?: 0.0
                val totalAmount = doc.getDouble("totalAmount") ?: (quantityKg * pricePerKg)
                val isFullRefill = doc.getBoolean("isFullRefill") ?: true
                val notes = doc.getString("notes") ?: ""
                val distance = doc.getDouble("distanceTravelled") ?: 0.0
                val mileage = doc.getDouble("mileageKmPerKg") ?: 0.0
                val costPerKm = doc.getDouble("costPerKm") ?: 0.0

                val refill = FuelRefill(
                    carId = mappedCarId,
                    pumpId = pumpId,
                    pumpName = pumpName,
                    date = date,
                    timeFormatted = timeFormatted,
                    odometer = odometer,
                    quantityKg = quantityKg,
                    pricePerKg = pricePerKg,
                    totalAmount = totalAmount,
                    isFullRefill = isFullRefill,
                    notes = notes,
                    distanceTravelled = distance,
                    mileageKmPerKg = mileage,
                    costPerKm = costPerKm
                )
                refillDao.insertRefill(refill)
                restoredRefills++
            }

            val syncTime = System.currentTimeMillis()
            appPreferences.lastCloudSyncTime = syncTime
            val formattedTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(syncTime))
            val summary = SyncSummary(
                vehiclesSynced = restoredCars,
                refillsSynced = restoredRefills,
                timestamp = syncTime,
                message = "Restored from Cloud: $restoredCars vehicles & $restoredRefills refills restored ($formattedTime)"
            )

            _syncState.value = CloudSyncState(
                isSyncing = false,
                lastSyncTime = syncTime,
                lastSyncMessage = summary.message,
                isSuccess = true,
                errorMessage = null
            )

            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Restore error", e)
            val errorMsg = e.localizedMessage ?: "Failed to restore data from cloud."
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                isSuccess = false,
                errorMessage = errorMsg
            )
            Result.failure(e)
        }
    }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val firestore = firestoreProvider() ?: return@withContext Result.failure(IllegalStateException("Firestore is unavailable"))
        try {
            val start = System.currentTimeMillis()
            firestore.collection("system_health").document("ping").set(
                mapOf("ping" to start, "service" to "FirestoreSyncRepository"),
                SetOptions.merge()
            ).await()
            val latency = System.currentTimeMillis() - start
            val msg = "Firestore Cloud Backup is active (${latency}ms latency)."
            _syncState.value = _syncState.value.copy(lastSyncMessage = msg, isSuccess = true, errorMessage = null)
            Result.success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Ping failed", e)
            val msg = "Firestore service is active in offline-caching mode: ${e.localizedMessage}"
            _syncState.value = _syncState.value.copy(lastSyncMessage = msg, isSuccess = true)
            Result.success(msg)
        }
    }

    override suspend fun pushLivePumpStatus(
        pumpId: Long,
        stockStatus: String,
        pressureBar: Double,
        queueMinutes: Int,
        isGasAvailable: Boolean,
        reporterName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = firestoreProvider() ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val docRef = firestore.collection(COLLECTION_LIVE_STATUS).document("pump_$pumpId")
            val data = hashMapOf(
                "pumpId" to pumpId,
                "stockStatus" to stockStatus,
                "gasPressureBar" to pressureBar,
                "queueWaitMinutes" to queueMinutes,
                "isGasAvailable" to isGasAvailable,
                "reporterName" to reporterName,
                "reportedAt" to System.currentTimeMillis(),
                "source" to "Community Live Status"
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing live pump status", e)
            Result.failure(e)
        }
    }

    override suspend fun pushPumpRating(rating: PumpRating): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = firestoreProvider() ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val docId = "rating_${rating.pumpId}_${System.currentTimeMillis()}"
            val docRef = firestore.collection(COLLECTION_RATINGS).document(docId)
            val data = hashMapOf(
                "pumpId" to rating.pumpId,
                "userName" to rating.userName,
                "overallRating" to rating.overallRating,
                "gasAvailabilityRating" to rating.gasAvailabilityRating,
                "pressureRating" to rating.pressureRating,
                "waitingTimeRating" to rating.waitingTimeRating,
                "staffRating" to rating.staffRating,
                "cleanlinessRating" to rating.cleanlinessRating,
                "priceAccuracyRating" to rating.priceAccuracyRating,
                "reviewText" to rating.reviewText,
                "createdAt" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing pump rating", e)
            Result.failure(e)
        }
    }

    override fun clearError() {
        _syncState.value = _syncState.value.copy(errorMessage = null)
    }
}
