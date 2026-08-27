package com.example

import com.example.util.AuthResult
import com.example.util.AuthUser
import com.example.util.FirebaseAuthManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit tests for CNG Driver authentication and user modeling.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun authUser_creation_hasExpectedFields() {
        val user = AuthUser(
            uid = "driver_123",
            email = "aayushkapure506@gmail.com",
            displayName = "Aayush Kapure",
            phone = "+91 98765 43210",
            provider = "google"
        )
        assertEquals("driver_123", user.uid)
        assertEquals("aayushkapure506@gmail.com", user.email)
        assertEquals("Aayush Kapure", user.displayName)
        assertEquals("google", user.provider)
    }

    @Test
    fun firebaseAuthManager_emailValidation() = runBlocking {
        val manager = FirebaseAuthManager()
        val invalidEmailResult = manager.signInWithEmailAndPassword("invalid-email", "password123")
        assertTrue(invalidEmailResult is AuthResult.Error)
        assertEquals("Please enter a valid email address.", (invalidEmailResult as AuthResult.Error).errorMessage)

        val shortPasswordResult = manager.signInWithEmailAndPassword("test@example.com", "123")
        assertTrue(shortPasswordResult is AuthResult.Error)
        assertEquals("Password must be at least 6 characters.", (shortPasswordResult as AuthResult.Error).errorMessage)
    }

    @Test
    fun syncSummary_and_cloudSyncState_dataIntegrity() {
        val summary = com.example.util.SyncSummary(
            vehiclesSynced = 3,
            refillsSynced = 12,
            timestamp = 1700000000000L,
            message = "Synced 3 vehicles & 12 refills"
        )
        assertEquals(3, summary.vehiclesSynced)
        assertEquals(12, summary.refillsSynced)
        assertTrue(summary.message.contains("12 refills"))

        val syncState = com.example.util.CloudSyncState(
            isSyncing = false,
            lastSyncTime = 1700000000000L,
            lastSyncMessage = "Success",
            isSuccess = true,
            errorMessage = null
        )
        assertFalse(syncState.isSyncing)
        assertTrue(syncState.isSuccess)
        assertEquals("Success", syncState.lastSyncMessage)
    }

    @Test
    fun mileageDiaryReminder_settingsState_defaultsAndValidation() {
        val state = com.example.ui.viewmodel.SettingsUiState(
            mileageDiaryReminderEnabled = true,
            mileageInactivityDays = 3
        )
        assertTrue(state.mileageDiaryReminderEnabled)
        assertEquals(3, state.mileageInactivityDays)

        val customState = state.copy(
            mileageDiaryReminderEnabled = false,
            mileageInactivityDays = 7
        )
        assertFalse(customState.mileageDiaryReminderEnabled)
        assertEquals(7, customState.mileageInactivityDays)
    }

    @Test
    fun mileageDiaryReminderWorker_constants_valid() {
        assertEquals("cng_mileage_diary_reminder_periodic", com.example.worker.MileageDiaryReminderWorker.WORK_NAME_PERIODIC)
        assertEquals("cng_mileage_diary_reminder_onetime", com.example.worker.MileageDiaryReminderWorker.WORK_NAME_ONETIME)
        assertEquals(8802, com.example.worker.MileageDiaryReminderWorker.NOTIFICATION_ID)
        assertEquals("cng_mileage_diary_reminders", com.example.util.NotificationHelper.CHANNEL_MILEAGE_REMINDER_ID)
    }
}

