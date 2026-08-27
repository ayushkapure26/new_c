package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import kotlin.math.*

object LocationHelper {

    // Default Fallback Center (Delhi NCR)
    const val DEFAULT_LAT = 28.6139
    const val DEFAULT_LNG = 77.2090

    data class UserLocation(
        val latitude: Double,
        val longitude: Double,
        val cityName: String = "Delhi NCR",
        val isGpsBased: Boolean = false
    )

    data class DistanceEstimate(
        val straightLineDistanceKm: Double,
        val drivingDistanceKm: Double,
        val walkingDistanceKm: Double,
        val estimatedDrivingTimeMinutes: Int,
        val estimatedWalkingTimeMinutes: Int
    )

    fun calculateDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0 // Earth radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (round(r * c * 10.0) / 10.0)
    }

    /**
     * Calculates walking and driving distance and estimated transit times
     * from user's current Google Maps location to a target CNG station.
     */
    fun calculateDetailedDistance(
        userLat: Double,
        userLng: Double,
        destLat: Double,
        destLng: Double
    ): DistanceEstimate {
        val straightKm = calculateDistanceKm(userLat, userLng, destLat, destLng)
        // Driving road network factor ~1.30x straight line
        val drivingKm = round(max(0.1, straightKm * 1.30) * 10.0) / 10.0
        // Walking pedestrian path factor ~1.15x straight line
        val walkingKm = round(max(0.1, straightKm * 1.15) * 10.0) / 10.0

        // Estimated transit times: Driving ~ 28 km/h, Walking ~ 4.8 km/h
        val drivingTimeMin = ((drivingKm / 28.0) * 60).toInt().coerceAtLeast(1)
        val walkingTimeMin = ((walkingKm / 4.8) * 60).toInt().coerceAtLeast(1)

        return DistanceEstimate(
            straightLineDistanceKm = straightKm,
            drivingDistanceKm = drivingKm,
            walkingDistanceKm = walkingKm,
            estimatedDrivingTimeMinutes = drivingTimeMin,
            estimatedWalkingTimeMinutes = walkingTimeMin
        )
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        onSuccess: (UserLocation) -> Unit,
        onFailure: () -> Unit
    ) {
        val locationService = LocationService.getInstance(context)
        locationService.fetchCoordinates(
            onSuccess = { location ->
                onSuccess(
                    UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        cityName = "Current Location",
                        isGpsBased = true
                    )
                )
            },
            onFailure = {
                onFailure()
            }
        )
    }

    val INDIAN_CITIES = listOf(
        UserLocation(28.6139, 77.2090, "Delhi NCR", false),
        UserLocation(19.0760, 72.8777, "Mumbai", false),
        UserLocation(18.5204, 73.8567, "Pune", false),
        UserLocation(23.0225, 72.5714, "Ahmedabad", false),
        UserLocation(12.9716, 77.5946, "Bengaluru", false),
        UserLocation(17.3850, 78.4867, "Hyderabad", false),
        UserLocation(26.9124, 75.7873, "Jaipur", false)
    )
}
