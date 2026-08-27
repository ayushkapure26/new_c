package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.Pump
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Robust LocationService utilizing Google Play Services FusedLocationProviderClient
 * to fetch precise coordinates for finding nearby CNG stations.
 */
class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    /**
     * Checks whether location permissions (FINE or COARSE) are granted.
     */
    fun hasPermission(): Boolean {
        return isPermissionGranted(context)
    }

    /**
     * Suspending function to retrieve current device coordinates with High Accuracy.
     * Falls back to lastKnownLocation if immediate coordinates are unavailable.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasPermission()) return null

        _isLocating.value = true
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val location = suspendCancellableCoroutine<Location?> { continuation ->
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        continuation.resume(loc)
                    } else {
                        // Fallback to lastLocation if fresh location is null
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc: Location? ->
                                continuation.resume(lastLoc)
                            }
                            .addOnFailureListener {
                                continuation.resume(null)
                            }
                    }
                }.addOnFailureListener {
                    // Fallback to lastLocation on failure
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLoc: Location? ->
                            continuation.resume(lastLoc)
                        }
                        .addOnFailureListener {
                            continuation.resume(null)
                        }
                }
            }

            if (location != null) {
                _currentLocation.value = location
            }
            location
        } catch (e: Exception) {
            null
        } finally {
            _isLocating.value = false
        }
    }

    /**
     * Callback-based retrieval of current coordinates for UI components.
     */
    @SuppressLint("MissingPermission")
    fun fetchCoordinates(
        onSuccess: (Location) -> Unit,
        onFailure: (Exception?) -> Unit = {}
    ) {
        if (!hasPermission()) {
            onFailure(SecurityException("Location permission not granted"))
            return
        }

        _isLocating.value = true
        val cancellationTokenSource = CancellationTokenSource()

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                _isLocating.value = false
                if (location != null) {
                    _currentLocation.value = location
                    onSuccess(location)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            _currentLocation.value = lastLoc
                            onSuccess(lastLoc)
                        } else {
                            onFailure(Exception("Unable to acquire location"))
                        }
                    }.addOnFailureListener { e ->
                        onFailure(e)
                    }
                }
            }.addOnFailureListener { e ->
                _isLocating.value = false
                // Attempt lastLocation fallback
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                    if (lastLoc != null) {
                        _currentLocation.value = lastLoc
                        onSuccess(lastLoc)
                    } else {
                        onFailure(e)
                    }
                }.addOnFailureListener {
                    onFailure(e)
                }
            }
        } catch (e: Exception) {
            _isLocating.value = false
            onFailure(e)
        }
    }

    /**
     * Cold Flow emitting continuous real-time location updates.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 10000L): Flow<Location> = callbackFlow {
        if (!hasPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                    trySend(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Computes distance from current user coordinates to a target CNG station.
     */
    fun getDistanceToStation(stationLat: Double, stationLng: Double): Double? {
        val loc = _currentLocation.value ?: return null
        return LocationHelper.calculateDistanceKm(
            loc.latitude,
            loc.longitude,
            stationLat,
            stationLng
        )
    }

    /**
     * Sorts and filters CNG pumps by proximity to current or specified coordinates.
     */
    fun findNearbyStations(
        pumps: List<Pump>,
        userLat: Double = _currentLocation.value?.latitude ?: LocationHelper.DEFAULT_LAT,
        userLng: Double = _currentLocation.value?.longitude ?: LocationHelper.DEFAULT_LNG,
        maxRadiusKm: Double = 50.0
    ): List<NearbyStationResult> {
        return pumps.map { pump ->
            val distance = LocationHelper.calculateDistanceKm(
                userLat,
                userLng,
                pump.latitude,
                pump.longitude
            )
            NearbyStationResult(pump = pump, distanceKm = distance)
        }
        .filter { it.distanceKm <= maxRadiusKm }
        .sortedBy { it.distanceKm }
    }

    data class NearbyStationResult(
        val pump: Pump,
        val distanceKm: Double
    )

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        @Volatile
        private var INSTANCE: LocationService? = null

        fun getInstance(context: Context): LocationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationService(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun isPermissionGranted(context: Context): Boolean {
            val fineGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            return fineGranted || coarseGranted
        }
    }
}
