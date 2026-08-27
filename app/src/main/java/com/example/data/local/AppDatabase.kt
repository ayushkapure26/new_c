package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CachedSearchDao
import com.example.data.dao.CarDao
import com.example.data.dao.PriceHistoryDao
import com.example.data.dao.PumpDao
import com.example.data.dao.PumpRatingDao
import com.example.data.dao.RefillDao
import com.example.data.model.CachedSearch
import com.example.data.model.Car
import com.example.data.model.PriceHistory
import com.example.data.model.PressureHistory
import com.example.data.model.Pump
import com.example.data.model.CNGStation
import com.example.data.model.PumpRating
import com.example.data.model.Refill
import com.example.data.model.FuelRefill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Car::class,
        CNGStation::class,
        FuelRefill::class,
        PumpRating::class,
        PriceHistory::class,
        PressureHistory::class,
        CachedSearch::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun pumpDao(): PumpDao
    abstract fun refillDao(): RefillDao
    abstract fun pumpRatingDao(): PumpRatingDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun cachedSearchDao(): CachedSearchDao

    fun cngStationDao(): PumpDao = pumpDao()
    fun fuelRefillDao(): RefillDao = refillDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cng_track_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                        }
                    })
                    .build()
                INSTANCE = instance

                // Ensure initial seed data is populated asynchronously if database tables are empty
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ensureDatabaseSeeded(instance)
                    } catch (_: Exception) {
                        // Handled safely without crashing
                    }
                }

                instance
            }
        }

        suspend fun ensureDatabaseSeeded(db: AppDatabase) {
            try {
                val carDao = db.carDao()
                val pumpDao = db.pumpDao()
                val refillDao = db.refillDao()
                val ratingDao = db.pumpRatingDao()
                val priceHistoryDao = db.priceHistoryDao()
                val cachedSearchDao = db.cachedSearchDao()

                // Check and Seed Cars if empty
                if (carDao.getCarCount() == 0) {
                    val defaultCarId = carDao.insertCar(
                        Car(
                            name = "Maruti WagonR CNG",
                            regNumber = "DL 01 AB 1234",
                            tankCapacityKg = 10.0,
                            fuelType = "CNG + Petrol",
                            currentOdometer = 28450.0,
                            expectedMileage = 26.5,
                            notes = "Daily commute vehicle",
                            isDefault = true
                        )
                    )

                    carDao.insertCar(
                        Car(
                            name = "Tata Tiago iCNG",
                            regNumber = "MH 02 CD 5678",
                            tankCapacityKg = 12.0,
                            fuelType = "CNG + Petrol",
                            currentOdometer = 14200.0,
                            expectedMileage = 24.0,
                            notes = "Family car",
                            isDefault = false
                        )
                    )

                    // Seed Sample Refills for Car 1 if refills empty
                    if (refillDao.getRefillCount() == 0) {
                        val now = System.currentTimeMillis()
                        val dayMs = 86400000L

                        val refill1 = Refill(
                            carId = defaultCarId,
                            pumpId = 1L,
                            pumpName = "IGL CNG Station - Ring Road Connaught Place",
                            date = now - (15 * dayMs),
                            timeFormatted = "08:30 AM",
                            odometer = 27800.0,
                            quantityKg = 8.5,
                            pricePerKg = 75.59,
                            totalAmount = 642.51,
                            isFullRefill = true,
                            notes = "Morning fill, quick line",
                            distanceTravelled = 220.0,
                            mileageKmPerKg = 25.88,
                            costPerKm = 2.92
                        )

                        val refill2 = Refill(
                            carId = defaultCarId,
                            pumpId = 1L,
                            pumpName = "IGL CNG Station - Ring Road Connaught Place",
                            date = now - (7 * dayMs),
                            timeFormatted = "06:15 PM",
                            odometer = 28120.0,
                            quantityKg = 9.2,
                            pricePerKg = 75.59,
                            totalAmount = 695.42,
                            isFullRefill = true,
                            notes = "High pressure 220 bar",
                            distanceTravelled = 320.0,
                            mileageKmPerKg = 34.78,
                            costPerKm = 2.17
                        )

                        val refill3 = Refill(
                            carId = defaultCarId,
                            pumpId = 2L,
                            pumpName = "MGL CNG Pump - Bandra Kurla Complex",
                            date = now - (2 * dayMs),
                            timeFormatted = "09:45 AM",
                            odometer = 28450.0,
                            quantityKg = 8.8,
                            pricePerKg = 76.00,
                            totalAmount = 668.80,
                            isFullRefill = true,
                            notes = "Highway trip fill up",
                            distanceTravelled = 330.0,
                            mileageKmPerKg = 37.50,
                            costPerKm = 2.02
                        )

                        refillDao.insertRefill(refill1)
                        refillDao.insertRefill(refill2)
                        refillDao.insertRefill(refill3)
                    }
                }

                // Check and Seed Pumps if empty
                if (pumpDao.getPumpCount() == 0) {
                    val samplePumps = listOf(
                        Pump(
                            id = 1,
                            name = "IGL CNG Station - Ring Road Connaught Place",
                            address = "Sector 1, Ring Road, Connaught Place, New Delhi",
                            city = "Delhi NCR",
                            latitude = 28.6315,
                            longitude = 77.2167,
                            isOpen = true,
                            pricePerKg = 75.59,
                            gasPressureBar = 215.0,
                            isGasAvailable = true,
                            stockStatus = "AVAILABLE", // Green
                            queueWaitMinutes = 8,
                            provider = "IGL",
                            highwayCorridor = "Delhi - Agra Yamuna Expy",
                            isSmartPick = true,
                            reportedByDriver = "Driver Community (10m ago)",
                            rating = 4.6,
                            ratingCount = 84,
                            phone = "+91 11 2345 6789",
                            isFavorite = true,
                            dataSourceType = "Live Community Feed"
                        ),
                        Pump(
                            id = 2,
                            name = "MGL CNG Pump - Bandra Kurla Complex",
                            address = "BKC Avenue, Bandra East, Mumbai, Maharashtra",
                            city = "Mumbai",
                            latitude = 19.0657,
                            longitude = 72.8687,
                            isOpen = true,
                            pricePerKg = 76.00,
                            gasPressureBar = 220.0,
                            isGasAvailable = true,
                            stockStatus = "AVAILABLE", // Green
                            queueWaitMinutes = 5,
                            provider = "MGL",
                            highwayCorridor = "Mumbai - Pune Expressway",
                            isSmartPick = true,
                            reportedByDriver = "Driver Amit K. (5m ago)",
                            rating = 4.8,
                            ratingCount = 120,
                            phone = "+91 22 6789 0123",
                            isFavorite = true,
                            dataSourceType = "Live Community Feed"
                        ),
                        Pump(
                            id = 3,
                            name = "MNGL CNG Station - Hinjawadi Phase 1",
                            address = "Near IT Park, Hinjawadi Phase 1, Pune, Maharashtra",
                            city = "Pune",
                            latitude = 18.5912,
                            longitude = 73.7389,
                            isOpen = true,
                            pricePerKg = 89.50,
                            gasPressureBar = 195.0,
                            isGasAvailable = true,
                            stockStatus = "AVAILABLE", // Green
                            queueWaitMinutes = 12,
                            provider = "MNGL",
                            highwayCorridor = "Mumbai - Pune Expressway",
                            isSmartPick = true,
                            reportedByDriver = "Driver Sagar P. (15m ago)",
                            rating = 4.2,
                            ratingCount = 56,
                            phone = "+91 20 8765 4321",
                            isFavorite = false,
                            dataSourceType = "Driver Verified"
                        ),
                        Pump(
                            id = 4,
                            name = "Adani Total Gas CNG - SG Highway",
                            address = "Opp. Nirma University, SG Highway, Ahmedabad, Gujarat",
                            city = "Ahmedabad",
                            latitude = 23.1287,
                            longitude = 72.5452,
                            isOpen = true,
                            pricePerKg = 78.20,
                            gasPressureBar = 210.0,
                            isGasAvailable = true,
                            stockStatus = "AVAILABLE", // Green
                            queueWaitMinutes = 10,
                            provider = "Adani Gas",
                            highwayCorridor = "Ahmedabad - Surat NH48",
                            isSmartPick = true,
                            reportedByDriver = "Driver Patel R. (22m ago)",
                            rating = 4.5,
                            ratingCount = 62,
                            phone = "+91 79 4567 8901",
                            isFavorite = false,
                            dataSourceType = "Live Community Feed"
                        ),
                        Pump(
                            id = 5,
                            name = "GAIL Gas CNG Pump - Electronic City",
                            address = "Hosur Main Road, Electronic City Phase 1, Bengaluru",
                            city = "Bengaluru",
                            latitude = 12.8452,
                            longitude = 77.6602,
                            isOpen = false,
                            pricePerKg = 82.50,
                            gasPressureBar = 0.0,
                            isGasAvailable = false,
                            stockStatus = "OUT_OF_STOCK", // Red
                            queueWaitMinutes = 0,
                            provider = "GAIL Gas",
                            highwayCorridor = "Bengaluru - Chennai Corridor",
                            isSmartPick = false,
                            reportedByDriver = "Driver Sunil K. (Maintenance)",
                            rating = 3.9,
                            ratingCount = 38,
                            phone = "+91 80 1234 5678",
                            isFavorite = false,
                            dataSourceType = "Community Alert: Pump Dry"
                        ),
                        Pump(
                            id = 6,
                            name = "Torrent Gas CNG Station - Malviya Nagar",
                            address = "Apex Circle, Malviya Nagar, Jaipur, Rajasthan",
                            city = "Jaipur",
                            latitude = 26.8521,
                            longitude = 75.8152,
                            isOpen = true,
                            pricePerKg = 84.00,
                            gasPressureBar = 205.0,
                            isGasAvailable = true,
                            stockStatus = "AVAILABLE", // Green
                            queueWaitMinutes = 15,
                            provider = "Torrent Gas",
                            highwayCorridor = "Delhi - Jaipur NH48",
                            isSmartPick = true,
                            reportedByDriver = "Driver Community (35m ago)",
                            rating = 4.4,
                            ratingCount = 45,
                            phone = "+91 141 234 5678",
                            isFavorite = false,
                            dataSourceType = "Driver Verified"
                        ),
                        Pump(
                            id = 7,
                            name = "Gujarat Gas CNG - Varachha Main Road",
                            address = "Near Mini Bazar, Varachha, Surat, Gujarat",
                            city = "Surat",
                            latitude = 21.2185,
                            longitude = 72.8624,
                            isOpen = true,
                            pricePerKg = 77.80,
                            gasPressureBar = 215.0,
                            isGasAvailable = true,
                            stockStatus = "AVAILABLE", // Green
                            queueWaitMinutes = 6,
                            provider = "Gujarat Gas",
                            highwayCorridor = "Ahmedabad - Surat NH48",
                            isSmartPick = true,
                            reportedByDriver = "Driver Jignesh S. (18m ago)",
                            rating = 4.7,
                            ratingCount = 78,
                            phone = "+91 261 245 6789",
                            isFavorite = true,
                            dataSourceType = "Live Community Feed"
                        ),
                        Pump(
                            id = 8,
                            name = "Maharashtra Natural Gas - Mumbai-Nashik Highway",
                            address = "NH 160, Dwarka Circle, Nashik, Maharashtra",
                            city = "Nashik",
                            latitude = 19.9872,
                            longitude = 73.8055,
                            isOpen = true,
                            pricePerKg = 86.50,
                            gasPressureBar = 185.0,
                            isGasAvailable = true,
                            stockStatus = "NEEDS_UPDATE", // Amber
                            queueWaitMinutes = 20,
                            provider = "MNGL",
                            highwayCorridor = "Mumbai - Nashik NH160",
                            isSmartPick = false,
                            reportedByDriver = "Last report > 2h ago",
                            rating = 4.1,
                            ratingCount = 39,
                            phone = "+91 253 234 5678",
                            isFavorite = false,
                            dataSourceType = "Awaiting Driver Verification"
                        ),
                        Pump(
                            id = 9,
                            name = "Avantika Gas CNG - Vijay Nagar Square",
                            address = "AB Road, Vijay Nagar, Indore, Madhya Pradesh",
                            city = "Indore",
                            latitude = 22.7533,
                            longitude = 75.8937,
                            isOpen = true,
                            pricePerKg = 88.00,
                            gasPressureBar = 210.0,
                            isGasAvailable = true,
                            stockStatus = "AVAILABLE", // Green
                            queueWaitMinutes = 9,
                            provider = "Avantika Gas",
                            highwayCorridor = "Indore - Ujjain Highway",
                            isSmartPick = true,
                            reportedByDriver = "Driver Vikas M. (12m ago)",
                            rating = 4.5,
                            ratingCount = 52,
                            phone = "+91 731 456 7890",
                            isFavorite = false,
                            dataSourceType = "Live Community Feed"
                        ),
                        Pump(
                            id = 10,
                            name = "Haryana City Gas - Expressway Toll Plaza",
                            address = "KMP Expressway Junction, Gurugram / Delhi NCR",
                            city = "Delhi NCR",
                            latitude = 28.4595,
                            longitude = 77.0266,
                            isOpen = true,
                            pricePerKg = 76.50,
                            gasPressureBar = 225.0,
                            isGasAvailable = true,
                            stockStatus = "AVAILABLE", // Green
                            queueWaitMinutes = 4,
                            provider = "IGL",
                            highwayCorridor = "Delhi - Agra Yamuna Expy",
                            isSmartPick = true,
                            reportedByDriver = "Driver Rajesh K. (4m ago)",
                            rating = 4.9,
                            ratingCount = 110,
                            phone = "+91 124 234 5678",
                            isFavorite = true,
                            dataSourceType = "High Pressure Highway Hub"
                        )
                    )
                    pumpDao.insertAll(samplePumps)
                }

                // Check and Seed Ratings if empty
                ratingDao.insertRating(
                    PumpRating(
                        pumpId = 1L,
                        userName = "Amit Sharma",
                        overallRating = 5.0f,
                        gasAvailabilityRating = 5.0f,
                        pressureRating = 5.0f,
                        waitingTimeRating = 4.0f,
                        staffRating = 5.0f,
                        cleanlinessRating = 4.5f,
                        priceAccuracyRating = 5.0f,
                        reviewText = "Excellent pressure (215+ bar)! Tank filled completely with 9.2 kg."
                    )
                )

                // Check and Seed Historical Prices if empty
                if (priceHistoryDao.getCount() == 0) {
                    val now = System.currentTimeMillis()
                    val monthMs = 30 * 86400000L
                    val historicalPrices = listOf(
                        PriceHistory(pumpId = 1L, price = 73.50, timestamp = now - (7 * monthMs)),
                        PriceHistory(pumpId = 1L, price = 74.00, timestamp = now - (6 * monthMs)),
                        PriceHistory(pumpId = 1L, price = 74.50, timestamp = now - (5 * monthMs)),
                        PriceHistory(pumpId = 1L, price = 74.80, timestamp = now - (4 * monthMs)),
                        PriceHistory(pumpId = 1L, price = 75.20, timestamp = now - (3 * monthMs)),
                        PriceHistory(pumpId = 1L, price = 75.59, timestamp = now - (2 * monthMs)),
                        PriceHistory(pumpId = 1L, price = 75.80, timestamp = now - (1 * monthMs)),
                        PriceHistory(pumpId = 1L, price = 76.20, timestamp = now)
                    )
                    priceHistoryDao.insertAllPriceHistory(historicalPrices)
                }

                // Check and Seed Cached Searches if empty
                if (cachedSearchDao.getCacheCount() == 0) {
                    val now = System.currentTimeMillis()
                    val initialCaches = listOf(
                        CachedSearch(
                            query = "Connaught Place",
                            city = "Delhi NCR",
                            latitude = 28.6315,
                            longitude = 77.2167,
                            timestamp = now - (2 * 3600000L),
                            resultCount = 3,
                            previewStationNames = "IGL Connaught Place, IGL Daryaganj"
                        ),
                        CachedSearch(
                            query = "Bandra Kurla Complex",
                            city = "Mumbai",
                            latitude = 19.0657,
                            longitude = 72.8687,
                            timestamp = now - (6 * 3600000L),
                            resultCount = 2,
                            previewStationNames = "MGL BKC Avenue, MGL Kalanagar"
                        ),
                        CachedSearch(
                            query = "Hinjawadi",
                            city = "Pune",
                            latitude = 18.5912,
                            longitude = 73.7389,
                            timestamp = now - (12 * 3600000L),
                            resultCount = 2,
                            previewStationNames = "MNGL Hinjawadi Phase 1, MNGL Wakad"
                        ),
                        CachedSearch(
                            query = "SG Highway",
                            city = "Ahmedabad",
                            latitude = 23.1287,
                            longitude = 72.5452,
                            timestamp = now - (24 * 3600000L),
                            resultCount = 2,
                            previewStationNames = "Adani Total Gas SG Highway"
                        )
                    )
                    initialCaches.forEach { cachedSearchDao.insertCachedSearch(it) }
                }
            } catch (_: Exception) {
                // Safeguard against any race conditions during database initialization
            }
        }
    }
}
