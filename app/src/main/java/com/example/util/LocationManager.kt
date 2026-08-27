package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LocationManager helper class using FusedLocationProviderClient to manage
 * location permissions and provide current coordinates for the pump finder map.
 */
class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _hasPermission = MutableStateFlow(isPermissionGranted(context))
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private var locationCallback: LocationCallback? = null

    /**
     * Checks if location permission (fine or coarse) has been granted.
     */
    fun hasLocationPermission(): Boolean {
        val granted = isPermissionGranted(context)
        _hasPermission.value = granted
        return granted
    }

    /**
     * Updates internal permission state flow.
     */
    fun updatePermissionState() {
        hasLocationPermission()
    }

    /**
     * Requests the current location once using FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentCoordinates(
        onSuccess: (Location) -> Unit,
        onFailure: () -> Unit = {}
    ) {
        if (!hasLocationPermission()) {
            onFailure()
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    _currentLocation.value = location
                    onSuccess(location)
                } else {
                    // Fallback to lastLocation if getCurrentLocation returns null
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            _currentLocation.value = lastLoc
                            onSuccess(lastLoc)
                        } else {
                            onFailure()
                        }
                    }.addOnFailureListener {
                        onFailure()
                    }
                }
            }.addOnFailureListener {
                onFailure()
            }
        } catch (e: Exception) {
            onFailure()
        }
    }

    /**
     * Starts continuous location updates for active tracking on the map.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(intervalMs: Long = 10000L) {
        if (!hasLocationPermission()) return

        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            // Permission or service error fallback
        }
    }

    /**
     * Stops location updates.
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    /**
     * Calculates distance in kilometers between user coordinates and target station.
     */
    fun calculateDistanceKm(targetLat: Double, targetLng: Double): Double? {
        val loc = _currentLocation.value ?: return null
        return LocationHelper.calculateDistanceKm(
            loc.latitude,
            loc.longitude,
            targetLat,
            targetLng
        )
    }

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        fun isPermissionGranted(context: Context): Boolean {
            val fineLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            return fineLocationGranted || coarseLocationGranted
        }
    }
}
