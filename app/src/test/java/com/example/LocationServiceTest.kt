package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Pump
import com.example.util.LocationHelper
import com.example.util.LocationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocationServiceTest {

    @Test
    fun testLocationServiceSingleton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instance1 = LocationService.getInstance(context)
        val instance2 = LocationService.getInstance(context)
        assertNotNull(instance1)
        assertEquals(instance1, instance2)
    }

    @Test
    fun testDistanceCalculation() {
        // Distance between Connaught Place (28.6315, 77.2167) and India Gate (28.6129, 77.2295) is approx ~2.4 km
        val distance = LocationHelper.calculateDistanceKm(28.6315, 77.2167, 28.6129, 77.2295)
        assertTrue(distance in 2.0..3.0)
    }

    @Test
    fun testFindNearbyStationsOrdering() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val locationService = LocationService.getInstance(context)

        val userLat = 28.6139
        val userLng = 77.2090

        val nearPump = Pump(
            id = 1L,
            name = "Near Station",
            address = "Near Point",
            city = "Delhi NCR",
            latitude = 28.6200,
            longitude = 77.2100,
            pricePerKg = 75.5,
            isOpen = true,
            isGasAvailable = true,
            gasPressureBar = 200.0,
            rating = 4.5,
            ratingCount = 10,
            isFavorite = false
        )

        val farPump = Pump(
            id = 2L,
            name = "Far Station",
            address = "Far Point",
            city = "Delhi NCR",
            latitude = 28.7500,
            longitude = 77.3000,
            pricePerKg = 75.5,
            isOpen = true,
            isGasAvailable = true,
            gasPressureBar = 200.0,
            rating = 4.0,
            ratingCount = 5,
            isFavorite = false
        )

        val results = locationService.findNearbyStations(
            pumps = listOf(farPump, nearPump),
            userLat = userLat,
            userLng = userLng,
            maxRadiusKm = 50.0
        )

        assertEquals(2, results.size)
        // Closest should be first
        assertEquals(nearPump.id, results[0].pump.id)
        assertEquals(farPump.id, results[1].pump.id)
        assertTrue(results[0].distanceKm < results[1].distanceKm)
    }
}
