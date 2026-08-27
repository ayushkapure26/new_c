package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utility for redirecting to Google Maps turn-by-turn navigation or location coordinates
 * with support for both Driving and Walking transit modes.
 */
object NavigationUtil {

    /**
     * Redirect directly into Google Maps application with turn-by-turn navigation
     * to the exact CNG pump location (latitude, longitude).
     *
     * @param mode "d" for driving (default) or "w" for walking / pedestrian.
     */
    fun navigateToPump(
        context: Context,
        latitude: Double,
        longitude: Double,
        pumpName: String = "CNG Station",
        mode: String = "d"
    ) {
        val encodedName = Uri.encode(pumpName)
        val travelModeParam = if (mode == "w") "walking" else "driving"

        // 1. Google Maps Navigation Intent (Turn-by-Turn GPS navigation mode)
        val navUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=$mode")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (navIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(navIntent)
                return
            }
        } catch (e: Exception) {
            // Android 11+ package visibility might throw or return null; proceed to fallbacks
        }

        // 2. Direct Geo URI with marker label
        try {
            val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedName)")
            val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (geoIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(geoIntent)
                return
            }
        } catch (e: Exception) {
            // Proceed to web fallback
        }

        // 3. Universal Google Maps Web Directions Fallback
        try {
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=$travelModeParam")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch Google Maps for this station location", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Convenience helper to launch Walking navigation in Google Maps.
     */
    fun navigateWalking(
        context: Context,
        latitude: Double,
        longitude: Double,
        pumpName: String = "CNG Station"
    ) {
        navigateToPump(context, latitude, longitude, pumpName, mode = "w")
    }
}
