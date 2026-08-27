package com.example.data.repository

import com.example.data.dao.CachedSearchDao
import com.example.data.dao.CarDao
import com.example.data.dao.PriceHistoryDao
import com.example.data.dao.PumpDao
import com.example.data.dao.PumpRatingDao
import com.example.data.dao.RefillDao
import com.example.data.model.CachedSearch
import com.example.data.model.Car
import com.example.data.model.PriceHistory
import com.example.data.model.Pump
import com.example.data.model.PumpRating
import com.example.data.model.Refill
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class CngRepository(
    private val carDao: CarDao,
    private val pumpDao: PumpDao,
    private val refillDao: RefillDao,
    private val pumpRatingDao: PumpRatingDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val cachedSearchDao: CachedSearchDao,
    var firestoreSyncRepository: FirestoreSyncRepository? = null
) {
    // Price History operations
    val priceHistory: Flow<List<PriceHistory>> = priceHistoryDao.getAllPriceHistory()

    suspend fun addPriceHistory(priceHistory: PriceHistory): Long {
        return priceHistoryDao.insertPriceHistory(priceHistory)
    }
    // Car operations
    val allCars: Flow<List<Car>> = carDao.getAllCars()
    val defaultCar: Flow<Car?> = carDao.getDefaultCar()

    suspend fun getCarById(id: Long): Car? = carDao.getCarById(id)

    suspend fun saveCar(car: Car): Long {
        val resultId = if (car.id == 0L) {
            val count = carDao.insertCar(car)
            if (car.isDefault) {
                carDao.clearDefaultCars()
                carDao.setDefaultCar(count)
            }
            count
        } else {
            if (car.isDefault) {
                carDao.clearDefaultCars()
            }
            carDao.updateCar(car)
            car.id
        }

        // Auto-backup to Firestore cloud if user is active
        try {
            firestoreSyncRepository?.let { syncRepo ->
                val savedCar = car.copy(id = resultId)
                syncRepo.backupCarToCloud(savedCar)
            }
        } catch (_: Exception) {}

        return resultId
    }

    suspend fun deleteCar(car: Car) {
        carDao.deleteCar(car)
        try {
            firestoreSyncRepository?.deleteCarFromCloud(car.id)
        } catch (_: Exception) {}
    }

    suspend fun setDefaultCar(carId: Long) {
        carDao.clearDefaultCars()
        carDao.setDefaultCar(carId)
    }

    // Pump operations
    val allPumps: Flow<List<Pump>> = pumpDao.getAllPumps()
    val favoritePumps: Flow<List<Pump>> = pumpDao.getFavoritePumps()

    suspend fun getPumpById(id: Long): Pump? = pumpDao.getPumpById(id)

    suspend fun toggleFavoritePump(pumpId: Long, isFavorite: Boolean) {
        pumpDao.setFavorite(pumpId, isFavorite)
    }

    suspend fun savePump(pump: Pump): Long {
        return if (pump.id == 0L) {
            pumpDao.insertPump(pump)
        } else {
            pumpDao.updatePump(pump)
            pump.id
        }
    }

    suspend fun reportIncorrectPumpInfo(pumpId: Long, note: String) {
        val pump = pumpDao.getPumpById(pumpId)
        pump?.let {
            val updated = it.copy(
                dataSourceType = "User flagged: $note",
                lastUpdatedTime = System.currentTimeMillis()
            )
            pumpDao.updatePump(updated)
        }
    }

    suspend fun reportLivePumpStatus(
        pumpId: Long,
        stockStatus: String,
        pressureBar: Double,
        queueMinutes: Int,
        isGasAvailable: Boolean,
        reporterName: String
    ) {
        val pump = pumpDao.getPumpById(pumpId)
        pump?.let {
            val updated = it.copy(
                stockStatus = stockStatus,
                gasPressureBar = pressureBar,
                queueWaitMinutes = queueMinutes,
                isGasAvailable = isGasAvailable,
                isOpen = isGasAvailable,
                reportedByDriver = "Driver $reporterName (Just now)",
                dataSourceType = "Live Community Verified",
                lastUpdatedTime = System.currentTimeMillis()
            )
            pumpDao.updatePump(updated)
        }

        // Sync live report to Firestore
        try {
            firestoreSyncRepository?.pushLivePumpStatus(
                pumpId = pumpId,
                stockStatus = stockStatus,
                pressureBar = pressureBar,
                queueMinutes = queueMinutes,
                isGasAvailable = isGasAvailable,
                reporterName = reporterName
            )
        } catch (_: Exception) {}
    }

    // Refill operations
    val allRefills: Flow<List<Refill>> = refillDao.getAllRefills()
    val lastRefill: Flow<Refill?> = refillDao.getLastRefill()

    fun getRefillsByCar(carId: Long): Flow<List<Refill>> = refillDao.getRefillsByCar(carId)

    suspend fun getRefillById(id: Long): Refill? = refillDao.getRefillById(id)

    suspend fun saveRefill(
        refill: Refill,
        updateCarOdometer: Boolean = true
    ): Long {
        // Calculate auto metrics based on previous refill odometer reading if available
        var distance = refill.distanceTravelled
        var mileage = refill.mileageKmPerKg
        var costPerKm = refill.costPerKm

        val prevRefill = refillDao.getPreviousRefill(refill.carId, refill.date)
        if (prevRefill != null && refill.odometer > prevRefill.odometer) {
            distance = refill.odometer - prevRefill.odometer
            if (refill.quantityKg > 0) {
                mileage = distance / refill.quantityKg
            }
            if (distance > 0) {
                costPerKm = refill.totalAmount / distance
            }
        }

        val calculatedRefill = refill.copy(
            distanceTravelled = if (distance > 0) distance else refill.distanceTravelled,
            mileageKmPerKg = if (mileage > 0) mileage else refill.mileageKmPerKg,
            costPerKm = if (costPerKm > 0) costPerKm else refill.costPerKm
        )

        val id = if (calculatedRefill.id == 0L) {
            refillDao.insertRefill(calculatedRefill)
        } else {
            refillDao.updateRefill(calculatedRefill)
            calculatedRefill.id
        }

        // Update Car odometer if this refill is higher
        if (updateCarOdometer) {
            val car = carDao.getCarById(refill.carId)
            if (car != null && refill.odometer > car.currentOdometer) {
                val updatedCar = car.copy(currentOdometer = refill.odometer)
                carDao.updateCar(updatedCar)
                try {
                    firestoreSyncRepository?.backupCarToCloud(updatedCar)
                } catch (_: Exception) {}
            }
        }

        // Auto-backup to Firestore cloud
        try {
            firestoreSyncRepository?.let { syncRepo ->
                val savedRefill = calculatedRefill.copy(id = id)
                syncRepo.backupRefillToCloud(savedRefill)
            }
        } catch (_: Exception) {}

        return id
    }

    suspend fun deleteRefill(refill: Refill) {
        refillDao.deleteRefill(refill)
        try {
            firestoreSyncRepository?.deleteRefillFromCloud(refill.id)
        } catch (_: Exception) {}
    }

    // Rating & Review operations
    fun getRatingsForPump(pumpId: Long): Flow<List<PumpRating>> =
        pumpRatingDao.getRatingsForPump(pumpId)

    suspend fun addPumpRating(rating: PumpRating) {
        pumpRatingDao.insertRating(rating)
        // Recalculate average rating for pump
        val pump = pumpDao.getPumpById(rating.pumpId)
        if (pump != null) {
            val newCount = pump.ratingCount + 1
            val newAvg = ((pump.rating * pump.ratingCount) + rating.overallRating) / newCount
            val updatedPump = pump.copy(
                rating = (kotlin.math.round(newAvg * 10.0) / 10.0),
                ratingCount = newCount,
                lastUpdatedTime = System.currentTimeMillis()
            )
            pumpDao.updatePump(updatedPump)
        }

        try {
            firestoreSyncRepository?.pushPumpRating(rating)
        } catch (_: Exception) {}
    }

    // Map Search Caching operations (Offline Mode)
    val allCachedSearches: Flow<List<CachedSearch>> = cachedSearchDao.getAllCachedSearches()

    suspend fun saveCachedSearch(
        query: String,
        city: String,
        latitude: Double,
        longitude: Double,
        resultCount: Int,
        previewStationNames: String
    ): Long {
        val existing = cachedSearchDao.findCachedSearch(query, city)
        val searchToSave = CachedSearch(
            id = existing?.id ?: 0L,
            query = query,
            city = city,
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis(),
            resultCount = resultCount,
            previewStationNames = previewStationNames
        )
        return cachedSearchDao.insertCachedSearch(searchToSave)
    }

    suspend fun deleteCachedSearch(id: Long) {
        cachedSearchDao.deleteCachedSearch(id)
    }

    suspend fun clearAllSearchCache() {
        cachedSearchDao.clearAllCache()
    }
}
