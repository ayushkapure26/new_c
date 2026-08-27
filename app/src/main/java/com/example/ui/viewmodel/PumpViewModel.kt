package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CachedSearch
import com.example.data.model.Pump
import com.example.data.model.PumpRating
import com.example.data.repository.CngRepository
import com.example.util.FirestoreSyncHelper
import com.example.util.LocationHelper
import com.example.util.LocationHelper.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PumpSortOption {
    DISTANCE_LOW,
    PRICE_LOW,
    RATING_HIGH
}

enum class SuggestionType {
    PUMP_NAME,
    AREA_LOCATION,
    CITY_REGION
}

data class PumpSearchSuggestion(
    val title: String,
    val subtitle: String,
    val type: SuggestionType,
    val pump: Pump? = null,
    val textToApply: String
)

data class PumpFilterState(
    val searchQuery: String = "",
    val openNowOnly: Boolean = false,
    val gasAvailableOnly: Boolean = false,
    val pressureAvailableOnly: Boolean = false,
    val stockStatusFilter: String = "ALL", // "ALL", "AVAILABLE", "OUT_OF_STOCK", "NEEDS_UPDATE"
    val selectedHighwayCorridor: String = "All Corridors", // Trip Mode highway filter
    val isTripModeActive: Boolean = false,
    val selectedCity: String = "Delhi NCR",
    val sortOption: PumpSortOption = PumpSortOption.DISTANCE_LOW,
    val userLocation: UserLocation = UserLocation(LocationHelper.DEFAULT_LAT, LocationHelper.DEFAULT_LNG, "Delhi NCR", false),
    val isGpsGranted: Boolean = false,
    val showLocationRationale: Boolean = false,
    val isOfflineMode: Boolean = false,
    val activeCachedSearchInfo: String? = null
)

data class PumpDisplayItem(
    val pump: Pump,
    val distanceKm: Double,
    val isCachedOffline: Boolean = true
)

class PumpViewModel(
    private val repository: CngRepository,
    private val syncHelper: FirestoreSyncHelper? = null
) : ViewModel() {

    companion object {
        val POPULAR_HIGHWAY_CORRIDORS = listOf(
            "All Corridors",
            "Mumbai - Pune Expressway",
            "Delhi - Agra Yamuna Expy",
            "Ahmedabad - Surat NH48",
            "Mumbai - Nashik NH160",
            "Delhi - Jaipur NH48",
            "Indore - Ujjain Highway",
            "Bengaluru - Chennai Corridor"
        )
    }

    val filterState = MutableStateFlow(PumpFilterState())

    // Cached searches stream for offline lookup
    val cachedSearches: StateFlow<List<CachedSearch>> = repository.allCachedSearches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pumpsList: StateFlow<List<PumpDisplayItem>> = combine(
        repository.allPumps,
        filterState
    ) { allPumps, filter ->
        var list = allPumps.map { pump ->
            val dist = LocationHelper.calculateDistanceKm(
                filter.userLocation.latitude,
                filter.userLocation.longitude,
                pump.latitude,
                pump.longitude
            )
            PumpDisplayItem(pump, dist, isCachedOffline = true)
        }

        // Apply Search Filter
        if (filter.searchQuery.isNotBlank()) {
            val q = filter.searchQuery.lowercase()
            list = list.filter {
                it.pump.name.lowercase().contains(q) ||
                        it.pump.address.lowercase().contains(q) ||
                        it.pump.city.lowercase().contains(q) ||
                        it.pump.provider.lowercase().contains(q) ||
                        it.pump.highwayCorridor.lowercase().contains(q)
            }
        }

        // Apply Highway Corridor / Trip Mode filter
        if (filter.selectedHighwayCorridor != "All Corridors") {
            list = list.filter {
                it.pump.highwayCorridor.equals(filter.selectedHighwayCorridor, ignoreCase = true)
            }
        }

        // Apply Stock Status filter (Green Available vs Red Out of Stock)
        if (filter.stockStatusFilter != "ALL") {
            list = list.filter {
                it.pump.stockStatus.equals(filter.stockStatusFilter, ignoreCase = true)
            }
        }

        // Apply Toggle Filters
        if (filter.openNowOnly) {
            list = list.filter { it.pump.isOpen }
        }
        if (filter.gasAvailableOnly) {
            list = list.filter { it.pump.isGasAvailable }
        }
        if (filter.pressureAvailableOnly) {
            list = list.filter { it.pump.gasPressureBar > 180.0 }
        }

        // Sorting
        list = when (filter.sortOption) {
            PumpSortOption.DISTANCE_LOW -> list.sortedBy { it.distanceKm }
            PumpSortOption.PRICE_LOW -> list.sortedBy { it.pump.pricePerKg }
            PumpSortOption.RATING_HIGH -> list.sortedByDescending { it.pump.rating }
        }

        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchSuggestions: StateFlow<List<PumpSearchSuggestion>> = combine(
        repository.allPumps,
        filterState
    ) { allPumps, filter ->
        val query = filter.searchQuery.trim().lowercase()
        if (query.length < 1) {
            emptyList()
        } else {
            val suggestions = mutableListOf<PumpSearchSuggestion>()

            // 1. Matching Pump Names
            allPumps.filter { it.name.lowercase().contains(query) }
                .take(4)
                .forEach { pump ->
                    suggestions.add(
                        PumpSearchSuggestion(
                            title = pump.name,
                            subtitle = "${pump.address} • ${pump.city}",
                            type = SuggestionType.PUMP_NAME,
                            pump = pump,
                            textToApply = pump.name
                        )
                    )
                }

            // 2. Matching Areas / Landmarks in address
            allPumps.flatMap { pump ->
                val parts = pump.address.split(",", "-").map { it.trim() }
                parts.filter { part ->
                    part.length >= 3 && part.lowercase().contains(query) && !pump.name.lowercase().contains(part.lowercase())
                }.map { area ->
                    PumpSearchSuggestion(
                        title = area,
                        subtitle = "Area / Location in ${pump.city}",
                        type = SuggestionType.AREA_LOCATION,
                        pump = null,
                        textToApply = area
                    )
                }
            }
            .distinctBy { it.title.lowercase() }
            .take(3)
            .forEach { suggestions.add(it) }

            // 3. Matching Cities
            LocationHelper.INDIAN_CITIES
                .filter { it.cityName.lowercase().contains(query) }
                .take(2)
                .forEach { cityLoc ->
                    suggestions.add(
                        PumpSearchSuggestion(
                            title = cityLoc.cityName,
                            subtitle = "City Region",
                            type = SuggestionType.CITY_REGION,
                            pump = null,
                            textToApply = cityLoc.cityName
                        )
                    )
                }

            suggestions.distinctBy { it.title.lowercase() }.take(6)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        filterState.value = filterState.value.copy(
            searchQuery = query,
            activeCachedSearchInfo = null
        )
        if (query.isNotBlank() && query.length >= 3) {
            cacheCurrentSearch(query)
        }
    }

    fun applySuggestion(suggestion: PumpSearchSuggestion) {
        updateSearchQuery(suggestion.textToApply)
        if (suggestion.type == SuggestionType.CITY_REGION) {
            val cityLoc = LocationHelper.INDIAN_CITIES.find { it.cityName.equals(suggestion.textToApply, ignoreCase = true) }
            if (cityLoc != null) {
                selectCity(cityLoc)
            }
        }
        cacheCurrentSearch(suggestion.textToApply)
    }

    fun applyCachedSearch(cached: CachedSearch) {
        val matchingCity = LocationHelper.INDIAN_CITIES.find { it.cityName.equals(cached.city, ignoreCase = true) }
        val targetLocation = if (matchingCity != null) {
            matchingCity
        } else {
            UserLocation(cached.latitude, cached.longitude, cached.city, false)
        }

        filterState.value = filterState.value.copy(
            searchQuery = cached.query,
            selectedCity = cached.city,
            userLocation = targetLocation,
            activeCachedSearchInfo = "Offline Cached: ${cached.query} (${cached.resultCount} stations)"
        )
    }

    private fun cacheCurrentSearch(query: String) {
        viewModelScope.launch {
            val currentList = pumpsList.value
            val previewNames = currentList.take(2).joinToString(", ") { it.pump.name }
            repository.saveCachedSearch(
                query = query,
                city = filterState.value.selectedCity,
                latitude = filterState.value.userLocation.latitude,
                longitude = filterState.value.userLocation.longitude,
                resultCount = currentList.size,
                previewStationNames = previewNames
            )
        }
    }

    fun deleteCachedSearch(id: Long) {
        viewModelScope.launch {
            repository.deleteCachedSearch(id)
        }
    }

    fun clearAllCachedSearches() {
        viewModelScope.launch {
            repository.clearAllSearchCache()
        }
    }

    fun toggleOfflineMode() {
        val newMode = !filterState.value.isOfflineMode
        filterState.value = filterState.value.copy(
            isOfflineMode = newMode,
            activeCachedSearchInfo = if (newMode) "Offline Mode Active - Showing Cached Map Data" else null
        )
    }

    fun clearSearchQuery() {
        filterState.value = filterState.value.copy(
            searchQuery = "",
            activeCachedSearchInfo = null
        )
    }

    fun toggleOpenNow() {
        filterState.value = filterState.value.copy(openNowOnly = !filterState.value.openNowOnly)
    }

    fun toggleGasAvailable() {
        filterState.value = filterState.value.copy(gasAvailableOnly = !filterState.value.gasAvailableOnly)
    }

    fun togglePressureAvailable() {
        filterState.value = filterState.value.copy(pressureAvailableOnly = !filterState.value.pressureAvailableOnly)
    }

    fun setSortOption(option: PumpSortOption) {
        filterState.value = filterState.value.copy(sortOption = option)
    }

    fun selectCity(cityLocation: UserLocation) {
        filterState.value = filterState.value.copy(
            selectedCity = cityLocation.cityName,
            userLocation = cityLocation.copy(isGpsBased = false)
        )
        cacheCurrentSearch(cityLocation.cityName)
    }

    fun setGpsLocation(lat: Double, lng: Double) {
        filterState.value = filterState.value.copy(
            isGpsGranted = true,
            showLocationRationale = false,
            userLocation = UserLocation(lat, lng, "Current GPS Location", true)
        )
    }

    fun setGpsDenied() {
        filterState.value = filterState.value.copy(
            isGpsGranted = false,
            showLocationRationale = false
        )
    }

    fun toggleFavorite(pumpId: Long, currentFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavoritePump(pumpId, !currentFavorite)
        }
    }

    fun addRating(
        pumpId: Long,
        userName: String,
        overallRating: Float,
        gasRating: Float,
        pressureRating: Float,
        waitingRating: Float,
        staffRating: Float,
        cleanlinessRating: Float,
        priceAccuracyRating: Float,
        reviewText: String
    ) {
        viewModelScope.launch {
            val rating = PumpRating(
                pumpId = pumpId,
                userName = if (userName.isNotBlank()) userName else "CNG Driver",
                overallRating = overallRating,
                gasAvailabilityRating = gasRating,
                pressureRating = pressureRating,
                waitingTimeRating = waitingRating,
                staffRating = staffRating,
                cleanlinessRating = cleanlinessRating,
                priceAccuracyRating = priceAccuracyRating,
                reviewText = reviewText
            )
            repository.addPumpRating(rating)
            syncHelper?.pushPumpRatingToFirestore(rating)
        }
    }

    fun setHighwayCorridor(corridor: String) {
        filterState.value = filterState.value.copy(
            selectedHighwayCorridor = corridor,
            isTripModeActive = corridor != "All Corridors"
        )
    }

    fun setStockStatusFilter(status: String) {
        filterState.value = filterState.value.copy(stockStatusFilter = status)
    }

    fun toggleTripMode() {
        val nextMode = !filterState.value.isTripModeActive
        filterState.value = filterState.value.copy(
            isTripModeActive = nextMode,
            selectedHighwayCorridor = if (nextMode && filterState.value.selectedHighwayCorridor == "All Corridors") "Mumbai - Pune Expressway" else if (!nextMode) "All Corridors" else filterState.value.selectedHighwayCorridor
        )
    }

    fun reportLivePumpStatus(
        pumpId: Long,
        stockStatus: String,
        pressureBar: Double,
        queueMinutes: Int,
        isGasAvailable: Boolean,
        reporterName: String
    ) {
        viewModelScope.launch {
            repository.reportLivePumpStatus(
                pumpId = pumpId,
                stockStatus = stockStatus,
                pressureBar = pressureBar,
                queueMinutes = queueMinutes,
                isGasAvailable = isGasAvailable,
                reporterName = reporterName
            )
            syncHelper?.pushLivePumpStatusToFirestore(
                pumpId = pumpId,
                stockStatus = stockStatus,
                pressureBar = pressureBar,
                queueMinutes = queueMinutes,
                isGasAvailable = isGasAvailable,
                reporterName = reporterName
            )
        }
    }

    fun reportIncorrectInfo(pumpId: Long, reason: String) {
        viewModelScope.launch {
            repository.reportIncorrectPumpInfo(pumpId, reason)
        }
    }

    fun getRatingsForPump(pumpId: Long) = repository.getRatingsForPump(pumpId)

    class Factory(
        private val repository: CngRepository,
        private val syncHelper: FirestoreSyncHelper? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PumpViewModel(repository, syncHelper) as T
        }
    }
}

