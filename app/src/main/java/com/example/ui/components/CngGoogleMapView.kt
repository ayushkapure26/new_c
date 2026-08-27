package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.CNGStation
import com.example.data.model.Pump
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.PumpDisplayItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Google Maps SDK Interactive Map View that plots the user's live location
 * and nearby CNGStation Room entities as interactive markers.
 */
@Composable
fun CngGoogleMapView(
    pumps: List<PumpDisplayItem>,
    userLatitude: Double = 28.6139,
    userLongitude: Double = 77.2090,
    onPumpSelected: (Pump) -> Unit = {},
    onNavigateClick: (Pump) -> Unit = {},
    onToggleFavorite: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val hasLocationPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val userLatLng = remember(userLatitude, userLongitude) {
        LatLng(userLatitude, userLongitude)
    }

    // Determine initial camera focus: user's location or closest pump
    val initialLatLng = remember(pumps, userLatitude, userLongitude) {
        LatLng(userLatitude, userLongitude)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 13.0f)
    }

    var selectedPumpItem by remember { mutableStateOf<PumpDisplayItem?>(null) }
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true,
            mapToolbarEnabled = true
        )
    }

    val mapProperties = remember(currentMapType, hasLocationPermission) {
        MapProperties(
            mapType = currentMapType,
            isMyLocationEnabled = hasLocationPermission
        )
    }

    // Move camera when user location or pumps change
    LaunchedEffect(userLatitude, userLongitude) {
        if (selectedPumpItem == null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 13.5f))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // 1. Google Maps Compose Surface
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .testTag("google_map_view"),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapClick = {
                selectedPumpItem = null
            }
        ) {
            // A. User Current Location Pin & Accuracy Halo
            Circle(
                center = userLatLng,
                radius = 350.0,
                fillColor = Color(0x3300BCD4),
                strokeColor = Color(0xFF00ACC1),
                strokeWidth = 3f,
                zIndex = 1.0f
            )

            Marker(
                state = MarkerState(position = userLatLng),
                title = "Your Current Location",
                snippet = "GPS: ${String.format(Locale.US, "%.4f", userLatitude)}, ${String.format(Locale.US, "%.4f", userLongitude)}",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                zIndex = 10.0f
            )

            // B. Plot CNG Station Room Entity Markers with Margdarshak Tri-Color System
            pumps.forEach { item ->
                val station = item.pump
                val position = LatLng(station.latitude, station.longitude)
                
                // Margdarshak Tri-Color Code: Green=In Stock, Red=Dry, Amber=Needs update
                val markerHue = when {
                    station.stockStatus == "OUT_OF_STOCK" || !station.isGasAvailable -> BitmapDescriptorFactory.HUE_RED
                    station.stockStatus == "NEEDS_UPDATE" -> BitmapDescriptorFactory.HUE_ORANGE
                    station.isSmartPick -> BitmapDescriptorFactory.HUE_CYAN
                    else -> BitmapDescriptorFactory.HUE_GREEN
                }

                Marker(
                    state = MarkerState(position = position),
                    title = "${if (station.isSmartPick) "⭐ " else ""}${station.name}",
                    snippet = "₹${String.format(Locale.US, "%.2f", station.pricePerKg)}/kg • ${station.gasPressureBar.toInt()} Bar • ${if (station.stockStatus == "OUT_OF_STOCK") "🔴 DRY" else "🟢 IN STOCK"} • ${String.format(Locale.US, "%.1f", item.distanceKm)} km",
                    icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                    zIndex = if (station.isSmartPick) 8.0f else 5.0f,
                    onClick = {
                        selectedPumpItem = item
                        onPumpSelected(station)
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(position, 14.5f))
                        }
                        true
                    }
                )
            }
        }

        // 2. Map Controls Overlay (Top Right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Map Type Toggle Button (Normal / Satellite / Terrain)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = {
                        currentMapType = when (currentMapType) {
                            MapType.NORMAL -> MapType.HYBRID
                            MapType.HYBRID -> MapType.TERRAIN
                            else -> MapType.NORMAL
                        }
                    },
                    modifier = Modifier.testTag("btn_change_map_type")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Change Map Type",
                        tint = DarkTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Recenter to user's exact GPS location
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 14.0f))
                        }
                    },
                    modifier = Modifier.testTag("btn_recenter_user_gps")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter to My Location",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. User Location & Station Count Badge Overlay (Top Left)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkTeal.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "GPS: ${String.format(Locale.US, "%.2f", userLatitude)}°, ${String.format(Locale.US, "%.2f", userLongitude)}°",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = DarkTeal,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${pumps.size} Nearby Stations",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 4. Interactive Bottom Station Info Card Callout
        AnimatedVisibility(
            visible = selectedPumpItem != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            selectedPumpItem?.let { item ->
                val pump = item.pump
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_selected_pump_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pump.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    if (pump.isFavorite) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = "Favorite",
                                            tint = Color.Red,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = pump.address,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = { selectedPumpItem = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close card",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metric badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldGreen.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("CNG PRICE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${String.format(Locale.US, "%.2f", pump.pricePerKg)}/kg", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkTeal.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("PRESSURE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${pump.gasPressureBar.toInt()} Bar", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = DarkTeal)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format(Locale.US, "%.1f", item.distanceKm)} km", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: Navigate in Google Maps & View Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    launchGoogleMapsNavigation(context, pump.latitude, pump.longitude, pump.name)
                                    onNavigateClick(pump)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DarkTeal),
                                color = DarkTeal.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onPumpSelected(pump) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Launches external Google Maps Turn-by-Turn GPS Navigation intent.
 */
fun launchGoogleMapsNavigation(context: Context, latitude: Double, longitude: Double, label: String = "CNG Station") {
    com.example.util.NavigationUtil.navigateToPump(context, latitude, longitude, label)
}
