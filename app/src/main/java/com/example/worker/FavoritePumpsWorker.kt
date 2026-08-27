package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.model.Pump
import com.example.util.NotificationHelper
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class FavoritePumpsWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(appContext)
            val pumpDao = db.pumpDao()

            var favoritePumps = pumpDao.getFavoritePumpsList()
            if (favoritePumps.isEmpty()) {
                // If no pump is favorited yet, check all pumps
                favoritePumps = pumpDao.getAllPumpsList().take(2)
            }

            if (favoritePumps.isEmpty()) {
                return Result.success()
            }

            val updatedNotifications = mutableListOf<String>()

            for (pump in favoritePumps) {
                // Simulate price or operational status change detection (e.g., from IoT/live server sync)
                val priceDelta = if (Random.nextBoolean()) 0.25 else -0.15
                val newPrice = (kotlin.math.round((pump.pricePerKg + priceDelta) * 100.0) / 100.0).coerceAtLeast(60.0)
                val newGasAvailable = if (Random.nextInt(100) > 85) !pump.isGasAvailable else true
                val newPressure = if (newGasAvailable) (190.0 + Random.nextDouble(0.0, 35.0)) else 0.0

                val priceChanged = kotlin.math.abs(newPrice - pump.pricePerKg) >= 0.05
                val statusChanged = (newGasAvailable != pump.isGasAvailable)

                if (priceChanged || statusChanged) {
                    val updatedPump = pump.copy(
                        pricePerKg = newPrice,
                        isGasAvailable = newGasAvailable,
                        gasPressureBar = (kotlin.math.round(newPressure * 10.0) / 10.0),
                        lastUpdatedTime = System.currentTimeMillis(),
                        dataSourceType = "Live WorkManager Sync"
                    )

                    pumpDao.updatePump(updatedPump)

                    val statusText = if (newGasAvailable) "Open & Gas Available (${updatedPump.gasPressureBar} bar)" else "Temporarily Out of Stock"
                    val msg = "${pump.name}: Price ₹${String.format(java.util.Locale.US, "%.2f", newPrice)}/kg | $statusText"
                    updatedNotifications.add(msg)

                    NotificationHelper.showPumpUpdateNotification(
                        context = appContext,
                        notificationId = pump.id.toInt(),
                        title = "Favorite CNG Pump Updated ⛽",
                        message = msg
                    )
                }
            }

            if (updatedNotifications.isEmpty() && favoritePumps.isNotEmpty()) {
                // Post generic sync notice if values were stable
                val firstPump = favoritePumps.first()
                NotificationHelper.showPumpUpdateNotification(
                    context = appContext,
                    notificationId = 999,
                    title = "Favorite CNG Station Status Verified",
                    message = "${firstPump.name} is ${if (firstPump.isOpen && firstPump.isGasAvailable) "OPEN" else "CLOSED"} @ ₹${String.format(java.util.Locale.US, "%.2f", firstPump.pricePerKg)}/kg"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "cng_favorite_pumps_periodic_check"
        const val WORK_NAME_ONETIME = "cng_favorite_pumps_onetime_check"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<FavoritePumpsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        fun triggerImmediateCheck(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<FavoritePumpsWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONETIME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }

        fun cancelPeriodicCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        }
    }
}
