package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.CachedSearch
import com.example.data.model.Pump
import com.example.ui.components.CngMapCanvas
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LightEmerald
import com.example.ui.viewmodel.PumpDisplayItem
import com.example.ui.viewmodel.PumpSortOption
import com.example.ui.viewmodel.PumpViewModel
import com.example.ui.viewmodel.SuggestionType
import com.example.util.AppLocalization
import com.example.util.LocalizedStrings
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.CngGoogleMapView
import com.example.ui.components.CngMapCanvas

@Composable
fun PumpFinderScreen(
    pumpViewModel: PumpViewModel,
    selectedLanguage: String = "English",
    onRequestLocationPermission: () -> Unit = {},
    onSelectPump: (Pump) -> Unit
) {
    val context = LocalContext.current
    val strings = remember(selectedLanguage) { AppLocalization.getStrings(selectedLanguage) }
    val filterState by pumpViewModel.filterState.collectAsState()
    val pumpsList by pumpViewModel.pumpsList.collectAsState()
    val suggestions by pumpViewModel.searchSuggestions.collectAsState()
    val cachedSearches by pumpViewModel.cachedSearches.collectAsState()

    var showMapToggle by rememberSaveable { mutableStateOf(false) }
    var showCityDropdown by rememberSaveable { mutableStateOf(false) }
    var showSuggestions by rememberSaveable { mutableStateOf(true) }
    var showOfflineInfoDialog by rememberSaveable { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val cacheDateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isGpsActive = filterState.isGpsGranted && filterState.userLocation.isGpsBased

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Search & Auto-complete Bar
        Box(modifier = Modifier.fillMaxWidth().zIndex(10f)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = filterState.searchQuery,
                        onValueChange = {
                            pumpViewModel.updateSearchQuery(it)
                            showSuggestions = true
                        },
                        placeholder = { Text(strings.searchPlaceholder, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldGreen) },
                        trailingIcon = if (filterState.searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = {
                                    pumpViewModel.clearSearchQuery()
                                    showSuggestions = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pump_search_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // GPS Location Icon Button
                    IconButton(
                        onClick = {
                            val locService = com.example.util.LocationService.getInstance(context)
                            if (locService.hasPermission()) {
                                LocationHelper.fetchCurrentLocation(
                                    context = context,
                                    onSuccess = { loc ->
                                        pumpViewModel.setGpsLocation(loc.latitude, loc.longitude)
                                    },
                                    onFailure = {
                                        pumpViewModel.setGpsDenied()
                                    }
                                )
                            } else {
                                onRequestLocationPermission()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isGpsActive) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .testTag("btn_gps_quick_locate")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Locate with GPS",
                            tint = if (isGpsActive) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // City Picker Button
                    Box {
                        OutlinedButton(
                            onClick = { showCityDropdown = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = DarkTeal
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = filterState.selectedCity, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showCityDropdown,
                            onDismissRequest = { showCityDropdown = false }
                        ) {
                            LocationHelper.INDIAN_CITIES.forEach { cityLoc ->
                                DropdownMenuItem(
                                    text = { Text(cityLoc.cityName) },
                                    onClick = {
                                        pumpViewModel.selectCity(cityLoc)
                                        showCityDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Auto-complete Suggestions Dropdown
                if (showSuggestions && suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("search_autocomplete_card"),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            suggestions.forEachIndexed { index, suggestion ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            pumpViewModel.applySuggestion(suggestion)
                                            showSuggestions = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (icon, iconTint) = when (suggestion.type) {
                                        SuggestionType.PUMP_NAME -> Icons.Default.LocalGasStation to EmeraldGreen
                                        SuggestionType.AREA_LOCATION -> Icons.Default.LocationOn to DarkTeal
                                        SuggestionType.CITY_REGION -> Icons.Default.Explore to DarkTeal
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = suggestion.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.NorthWest,
                                        contentDescription = "Select suggestion",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // User Current GPS Location Status & Map Quick Launcher Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("user_location_status_bar"),
            shape = RoundedCornerShape(12.dp),
            color = if (isGpsActive) EmeraldGreen.copy(alpha = 0.12f) else DarkTeal.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, if (isGpsActive) EmeraldGreen.copy(alpha = 0.3f) else DarkTeal.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isGpsActive) Icons.Default.MyLocation else Icons.Default.LocationOn,
                        contentDescription = "Current Location",
                        tint = if (isGpsActive) EmeraldGreen else DarkTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (isGpsActive) "Live GPS Location" else "Selected Location: ${filterState.selectedCity}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.4f", filterState.userLocation.latitude)}° N, ${String.format(Locale.US, "%.4f", filterState.userLocation.longitude)}° E",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isGpsActive) {
                        OutlinedButton(
                            onClick = {
                                val locService = com.example.util.LocationService.getInstance(context)
                                if (locService.hasPermission()) {
                                    LocationHelper.fetchCurrentLocation(
                                        context = context,
                                        onSuccess = { loc ->
                                            pumpViewModel.setGpsLocation(loc.latitude, loc.longitude)
                                        },
                                        onFailure = {
                                            pumpViewModel.setGpsDenied()
                                        }
                                    )
                                } else {
                                    onRequestLocationPermission()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = EmeraldGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Locate Me", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Quick Map Action
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (showMapToggle) DarkTeal else EmeraldGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMapToggle = !showMapToggle }
                            .testTag("quick_toggle_map_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showMapToggle) Icons.Default.ViewList else Icons.Default.Map,
                                contentDescription = "Toggle Google Map",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showMapToggle) "List" else "Map",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Offline Map Search Caching Banner & Recent Searches
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (filterState.isOfflineMode) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (filterState.isOfflineMode) Icons.Default.WifiOff else Icons.Default.OfflinePin,
                            contentDescription = null,
                            tint = if (filterState.isOfflineMode) Color(0xFF2E7D32) else DarkTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.offlineCacheActive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filterState.isOfflineMode) Color(0xFF1B5E20) else DarkTeal
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.offlineModeToggle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = filterState.isOfflineMode,
                            onCheckedChange = { pumpViewModel.toggleOfflineMode() },
                            modifier = Modifier.size(width = 36.dp, height = 24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreen
                            )
                        )
                    }
                }

                // If an active cache notification is set
                if (filterState.activeCachedSearchInfo != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💾 ${filterState.activeCachedSearchInfo}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }

                // Recent Cached Search Pills
                if (cachedSearches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.cachedSearchesTitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(cachedSearches) { cached ->
                            val isCurrent = filterState.searchQuery.equals(cached.query, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isCurrent) EmeraldGreen else Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        pumpViewModel.applyCachedSearch(cached)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.OfflinePin,
                                        contentDescription = null,
                                        tint = if (isCurrent) Color.White else EmeraldGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${cached.query} (${cached.resultCount})",
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Bar (Trip Mode Corridors, Stock Status, Open Now, Nearby, Highest Rated, Lowest Price)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("filter_chips_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Margdarshak Highway Corridor / Trip Mode Chip
            item {
                FilterChip(
                    selected = filterState.isTripModeActive,
                    onClick = { pumpViewModel.toggleTripMode() },
                    label = { 
                        Text(
                            text = if (filterState.isTripModeActive) "🛣️ ${filterState.selectedHighwayCorridor.split("-").first().trim()}" else "🛣️ Highway Corridors",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("chip_trip_mode"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DarkTeal,
                        selectedLabelColor = Color.White
                    )
                )
            }

            // In-Stock Filter Chip (Tri-color Green)
            item {
                val isSelected = filterState.stockStatusFilter == "AVAILABLE"
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        pumpViewModel.setStockStatusFilter(if (isSelected) "ALL" else "AVAILABLE")
                    },
                    label = { Text("🟢 In Stock Only", fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_stock_available"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE8F5E9),
                        selectedLabelColor = Color(0xFF1B5E20)
                    )
                )
            }

            item {
                FilterChip(
                    selected = filterState.openNowOnly,
                    onClick = { pumpViewModel.toggleOpenNow() },
                    label = { Text(strings.openNow, fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_open_now"),
                    leadingIcon = if (filterState.openNowOnly) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }

            item {
                FilterChip(
                    selected = filterState.sortOption == PumpSortOption.DISTANCE_LOW,
                    onClick = {
                        pumpViewModel.setSortOption(PumpSortOption.DISTANCE_LOW)
                    },
                    label = { Text(strings.nearby, fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_nearby"),
                    leadingIcon = if (filterState.sortOption == PumpSortOption.DISTANCE_LOW) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }

            item {
                FilterChip(
                    selected = filterState.sortOption == PumpSortOption.RATING_HIGH,
                    onClick = {
                        if (filterState.sortOption == PumpSortOption.RATING_HIGH) {
                            pumpViewModel.setSortOption(PumpSortOption.DISTANCE_LOW)
                        } else {
                            pumpViewModel.setSortOption(PumpSortOption.RATING_HIGH)
                        }
                    },
                    label = { Text(strings.highestRated, fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_highest_rated"),
                    leadingIcon = if (filterState.sortOption == PumpSortOption.RATING_HIGH) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }

            item {
                FilterChip(
                    selected = filterState.sortOption == PumpSortOption.PRICE_LOW,
                    onClick = {
                        if (filterState.sortOption == PumpSortOption.PRICE_LOW) {
                            pumpViewModel.setSortOption(PumpSortOption.DISTANCE_LOW)
                        } else {
                            pumpViewModel.setSortOption(PumpSortOption.PRICE_LOW)
                        }
                    },
                    label = { Text(strings.lowestPrice, fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_lowest_price")
                )
            }

            item {
                FilterChip(
                    selected = filterState.gasAvailableOnly,
                    onClick = { pumpViewModel.toggleGasAvailable() },
                    label = { Text(strings.gasAvailable, fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_gas_available")
                )
            }

            item {
                FilterChip(
                    selected = filterState.pressureAvailableOnly,
                    onClick = { pumpViewModel.togglePressureAvailable() },
                    label = { Text(strings.highPressure, fontSize = 12.sp) },
                    modifier = Modifier.testTag("chip_high_pressure")
                )
            }
        }

        // Highway Corridor Selection Carousel when Trip Mode is active
        AnimatedVisibility(visible = filterState.isTripModeActive) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = "Margdarshak Trip Corridors:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PumpViewModel.POPULAR_HIGHWAY_CORRIDORS) { corridor ->
                        val isCorridorActive = filterState.selectedHighwayCorridor.equals(corridor, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCorridorActive) DarkTeal else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { pumpViewModel.setHighwayCorridor(corridor) }
                        ) {
                            Text(
                                text = corridor,
                                fontSize = 11.sp,
                                fontWeight = if (isCorridorActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCorridorActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // View Mode Toggle (List / Google Map)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${strings.stationsFound}: ${pumpsList.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (showMapToggle) DarkTeal else DarkTeal.copy(alpha = 0.1f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showMapToggle = !showMapToggle }
                    .testTag("toggle_map_view_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showMapToggle) Icons.Default.ViewList else Icons.Default.Map,
                        contentDescription = "Toggle Google Map",
                        tint = if (showMapToggle) Color.White else DarkTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showMapToggle) "List View" else "Google Map",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showMapToggle) Color.White else DarkTeal
                    )
                }
            }
        }

        if (showMapToggle) {
            Spacer(modifier = Modifier.height(8.dp))
            CngGoogleMapView(
                pumps = pumpsList,
                userLatitude = filterState.userLocation.latitude,
                userLongitude = filterState.userLocation.longitude,
                onPumpSelected = { pump -> onSelectPump(pump) },
                onNavigateClick = { pump ->
                    com.example.util.NavigationUtil.navigateToPump(
                        context = context,
                        latitude = pump.latitude,
                        longitude = pump.longitude,
                        pumpName = pump.name
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            )
        } else {
            // Pump Stations List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(pumpsList) { item ->
                    PumpStationCard(
                        displayItem = item,
                        strings = strings,
                        dateFormat = dateFormat,
                        onViewDetails = { onSelectPump(item.pump) },
                        onNavigate = {
                            com.example.util.NavigationUtil.navigateToPump(
                                context = context,
                                latitude = item.pump.latitude,
                                longitude = item.pump.longitude,
                                pumpName = item.pump.name
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PumpStationCard(
    displayItem: PumpDisplayItem,
    strings: LocalizedStrings,
    dateFormat: SimpleDateFormat,
    onViewDetails: () -> Unit,
    onNavigate: () -> Unit
) {
    val pump = displayItem.pump

    val stockBg = when (pump.stockStatus) {
        "OUT_OF_STOCK" -> Color(0xFFFFEBEE)
        "NEEDS_UPDATE" -> Color(0xFFFFF8E1)
        else -> Color(0xFFE8F5E9)
    }
    val stockColor = when (pump.stockStatus) {
        "OUT_OF_STOCK" -> Color(0xFFD32F2F)
        "NEEDS_UPDATE" -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }
    val stockText = when (pump.stockStatus) {
        "OUT_OF_STOCK" -> "🔴 DRY"
        "NEEDS_UPDATE" -> "🟡 UNVERIFIED"
        else -> "🟢 IN STOCK"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (pump.isSmartPick) BorderStroke(1.5.dp, Color(0xFFFFD54F)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Station Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pump.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (pump.isSmartPick) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFD54F)
                            ) {
                                Text(
                                    text = "★ SMART PICK",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${pump.provider} • ${pump.address} • ${displayItem.distanceKm} km",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Margdarshak Tri-Color Stock Pill
                Box(
                    modifier = Modifier
                        .background(color = stockBg, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stockText,
                        color = stockColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Highway & Queue Badge Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkTeal.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = DarkTeal,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${pump.highwayCorridor} • ⏱️ ~${pump.queueWaitMinutes}m wait",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTeal,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (pump.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price, Pressure & Rating Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(strings.cngPrice, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${pump.pricePerKg}/kg", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                }

                Column {
                    Text(strings.pressure, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${pump.gasPressureBar.toInt()} bar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
                }

                Column {
                    Text(strings.rating, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${pump.rating}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Text("(${pump.ratingCount})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Timestamp & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pump.reportedByDriver} • ${dateFormat.format(Date(pump.lastUpdatedTime))}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(strings.details, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.navigate, fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}
