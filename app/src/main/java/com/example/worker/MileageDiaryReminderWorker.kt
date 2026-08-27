package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.util.AppPreferences
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Background CoroutineWorker that periodically checks for driver inactivity in the mileage diary.
 * If the user has not logged any CNG refill or odometer update for the configured number of days
 * (e.g. 1, 2, 3, 5, or 7 days), it sends a reminder push notification.
 */
class MileageDiaryReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val appPreferences = AppPreferences(appContext)
            
            // Check if reminder is enabled in user settings
            if (!appPreferences.mileageDiaryReminderEnabled) {
                return Result.success()
            }

            val thresholdDays = appPreferences.mileageInactivityDays.coerceAtLeast(1)
            val thresholdMillis = TimeUnit.DAYS.toMillis(thresholdDays.toLong())
            val currentTime = System.currentTimeMillis()

            val db = AppDatabase.getDatabase(appContext)
            val refillDao = db.refillDao()
            val refills = refillDao.getAllRefills().first()

            val latestRefill = refills.maxByOrNull { it.date }

            val (isInactive, daysInactive, lastDateText) = if (latestRefill != null) {
                val diffMillis = currentTime - latestRefill.date
                val days = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt().coerceAtLeast(0)
                val formatted = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(latestRefill.date))
                Triple(diffMillis >= thresholdMillis, days, formatted)
            } else {
                // If no refills have ever been logged, treat as inactive if app has been used
                Triple(true, thresholdDays, null)
            }

            if (isInactive) {
                // Throttle notifications so user is not alerted more than once every 20 hours
                val lastSent = appPreferences.lastMileageReminderSentTimestamp
                val timeSinceLastSent = currentTime - lastSent
                val minThrottleMillis = TimeUnit.HOURS.toMillis(20)

                // Only send if throttle period has elapsed or this is a forced manual trigger
                if (timeSinceLastSent >= minThrottleMillis || isRunAttemptOverOne) {
                    val title = "Mileage Diary Reminder 🚗"
                    val message = if (lastDateText != null) {
                        "It's been $daysInactive days since your last refill on $lastDateText. Update your odometer to keep mileage & budget analytics accurate!"
                    } else {
                        "No fuel refills logged yet! Record your recent CNG refill and odometer reading to start tracking fuel efficiency."
                    }

                    NotificationHelper.showMileageDiaryReminderNotification(
                        context = appContext,
                        notificationId = NOTIFICATION_ID,
                        title = title,
                        message = message
                    )

                    appPreferences.lastMileageReminderSentTimestamp = currentTime
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private val isRunAttemptOverOne: Boolean
        get() = runAttemptCount > 0

    companion object {
        const val WORK_NAME_PERIODIC = "cng_mileage_diary_reminder_periodic"
        const val WORK_NAME_ONETIME = "cng_mileage_diary_reminder_onetime"
        const val NOTIFICATION_ID = 8802

        /**
         * Schedules the periodic background check for mileage diary inactivity.
         * Runs every 12 hours to catch inactivity without draining battery.
         */
        fun schedulePeriodicCheck(context: Context) {
            val periodicRequest = PeriodicWorkRequestBuilder<MileageDiaryReminderWorker>(
                12, TimeUnit.HOURS,
                30, TimeUnit.MINUTES // 30 min flex window
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        }

        /**
         * Triggers an immediate one-time check or demo notification for Settings testing.
         */
        fun triggerImmediateCheck(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<MileageDiaryReminderWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONETIME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }

        /**
         * Cancels periodic inactivity checks if disabled by the user.
         */
        fun cancelPeriodicCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        }
    }
}
