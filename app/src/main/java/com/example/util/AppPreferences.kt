package com.example.util

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cng_track_prefs", Context.MODE_PRIVATE)

    var priceChangeNotify: Boolean
        get() = prefs.getBoolean("notify_price_change", true)
        set(value) = prefs.edit().putBoolean("notify_price_change", value).apply()

    var openStatusNotify: Boolean
        get() = prefs.getBoolean("notify_open_status", true)
        set(value) = prefs.edit().putBoolean("notify_open_status", value).apply()

    var monthlyReminderNotify: Boolean
        get() = prefs.getBoolean("notify_monthly_reminder", true)
        set(value) = prefs.edit().putBoolean("notify_monthly_reminder", value).apply()

    var refillReminderNotify: Boolean
        get() = prefs.getBoolean("notify_refill_reminder", true)
        set(value) = prefs.edit().putBoolean("notify_refill_reminder", value).apply()

    var backupReminderNotify: Boolean
        get() = prefs.getBoolean("notify_backup_reminder", false)
        set(value) = prefs.edit().putBoolean("notify_backup_reminder", value).apply()

    var selectedCity: String
        get() = prefs.getString("selected_city", "Delhi NCR") ?: "Delhi NCR"
        set(value) = prefs.edit().putString("selected_city", value).apply()

    var isGpsPermissionPrompted: Boolean
        get() = prefs.getBoolean("gps_prompted", false)
        set(value) = prefs.edit().putBoolean("gps_prompted", value).apply()

    var appLanguage: String
        get() = prefs.getString("app_language", "English") ?: "English"
        set(value) = prefs.edit().putString("app_language", value).apply()

    var themeMode: String
        get() = prefs.getString("theme_mode", "System") ?: "System"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("user_is_logged_in", false)
        set(value) = prefs.edit().putBoolean("user_is_logged_in", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "Driver") ?: "Driver"
        set(value) = prefs.edit().putString("user_name", value).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "driver@cngtrack.com") ?: "driver@cngtrack.com"
        set(value) = prefs.edit().putString("user_email", value).apply()

    var userPhone: String
        get() = prefs.getString("user_phone", "+91 98765 43210") ?: "+91 98765 43210"
        set(value) = prefs.edit().putString("user_phone", value).apply()

    var memberSince: String
        get() = prefs.getString("user_member_since", "August 2026") ?: "August 2026"
        set(value) = prefs.edit().putString("user_member_since", value).apply()

    var userId: String
        get() = prefs.getString("user_id", "") ?: ""
        set(value) = prefs.edit().putString("user_id", value).apply()

    var monthlyBudget: Double
        get() = prefs.getFloat("monthly_budget", 5000f).toDouble()
        set(value) = prefs.edit().putFloat("monthly_budget", value.toFloat()).apply()

    var isAppTourCompleted: Boolean
        get() = prefs.getBoolean("app_tour_completed", false)
        set(value) = prefs.edit().putBoolean("app_tour_completed", value).apply()

    var lastCloudSyncTime: Long
        get() = prefs.getLong("last_cloud_sync_time", 0L)
        set(value) = prefs.edit().putLong("last_cloud_sync_time", value).apply()

    var isFirestoreAutoSyncEnabled: Boolean
        get() = prefs.getBoolean("firestore_auto_sync_enabled", true)
        set(value) = prefs.edit().putBoolean("firestore_auto_sync_enabled", value).apply()

    var isFirestoreCommunitySyncEnabled: Boolean
        get() = prefs.getBoolean("firestore_community_sync_enabled", true)
        set(value) = prefs.edit().putBoolean("firestore_community_sync_enabled", value).apply()

    var mileageDiaryReminderEnabled: Boolean
        get() = prefs.getBoolean("mileage_diary_reminder_enabled", true)
        set(value) = prefs.edit().putBoolean("mileage_diary_reminder_enabled", value).apply()

    var mileageInactivityDays: Int
        get() = prefs.getInt("mileage_inactivity_days", 3)
        set(value) = prefs.edit().putInt("mileage_inactivity_days", value).apply()

    var lastMileageReminderSentTimestamp: Long
        get() = prefs.getLong("last_mileage_reminder_sent_ts", 0L)
        set(value) = prefs.edit().putLong("last_mileage_reminder_sent_ts", value).apply()
}
