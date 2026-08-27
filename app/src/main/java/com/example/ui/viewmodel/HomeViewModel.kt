package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Car
import com.example.data.model.Pump
import com.example.data.model.Refill
import com.example.data.repository.CngRepository
import com.example.util.LocationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val currentCngPrice: Double = 75.59,
    val nearbyPumpsCount: Int = 0,
    val openPumpsCount: Int = 0,
    val averageGasPressure: Double = 210.0,
    val currentMonthExpense: Double = 0.0,
    val currentMonthRefillsCount: Int = 0,
    val currentMonthCngKg: Double = 0.0,
    val activeCarMileage: Double = 26.5,
    val activeCarName: String = "No Vehicle Added",
    val activeCar: Car? = null,
    val allCars: List<Car> = emptyList(),
    val estimatedRangeKm: Double = 180.0,
    val tankFillPercent: Float = 0.75f,
    val co2SavedKg: Int = 72,
    val treesEquivalent: Int = 4,
    val monthlyPetrolSavings: Double = 3450.0,
    val lastRefill: Refill? = null,
    val nearestPump: Pump? = null,
    val nearestPumpDistanceKm: Double = 1.8,
    val nearbyPumpsList: List<Pump> = emptyList(),
    val favoritePumpsList: List<Pump> = emptyList()
)

class HomeViewModel(private val repository: CngRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allPumps,
        repository.allRefills,
        repository.defaultCar,
        repository.allCars
    ) { pumps, refills, defaultCar, cars ->

        val activeCar = defaultCar ?: cars.firstOrNull()

        // Current Month Refills calculation
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val monthRefills = refills.filter { refill ->
            val refCal = Calendar.getInstance().apply { timeInMillis = refill.date }
            refCal.get(Calendar.MONTH) == currentMonth && refCal.get(Calendar.YEAR) == currentYear
        }

        val monthExpense = monthRefills.sumOf { it.totalAmount }
        val monthCngKg = monthRefills.sumOf { it.quantityKg }
        val openPumps = pumps.filter { it.isOpen && it.isGasAvailable }
        val avgPrice = if (pumps.isNotEmpty()) pumps.map { it.pricePerKg }.average() else 75.59
        val avgPressure = if (openPumps.isNotEmpty()) openPumps.map { it.gasPressureBar }.average() else 210.0

        // Calculate active car mileage from refills
        val carRefills = if (activeCar != null) refills.filter { it.carId == activeCar.id } else emptyList()
        val validMileages = carRefills.map { it.mileageKmPerKg }.filter { it > 0 }
        val calculatedMileage = if (validMileages.isNotEmpty()) validMileages.average() else (activeCar?.expectedMileage ?: 26.5)

        // Estimated tank capacity & remaining range
        val tankCapacity = activeCar?.tankCapacityKg ?: 10.0
        val lastRefillEntry = refills.firstOrNull()
        val lastFilledQty = lastRefillEntry?.quantityKg ?: (tankCapacity * 0.75)
        val tankFillRatio = ((lastFilledQty / tankCapacity).coerceIn(0.2, 1.0)).toFloat()
        val estimatedRange = kotlin.math.round(tankCapacity * tankFillRatio * calculatedMileage)

        // Eco & Savings
        val totalCngKgEstimate = if (monthCngKg > 0) monthCngKg else 28.0
        val co2Saved = (totalCngKgEstimate * 2.75).toInt()
        val trees = kotlin.math.max(1, (co2Saved / 18.0).toInt())
        // Petrol equivalent: ~15km/L at ₹100/L vs CNG at ₹75.59/kg with ~26.5 km/kg
        val cngCostPerKm = if (calculatedMileage > 0) (avgPrice / calculatedMileage) else 2.85
        val petrolCostPerKm = 100.0 / 15.0 // ~6.67
        val totalKmDrivenThisMonth = totalCngKgEstimate * calculatedMileage
        val petrolEquivalentCost = totalKmDrivenThisMonth * petrolCostPerKm
        val actualCngCost = if (monthExpense > 0) monthExpense else (totalCngKgEstimate * avgPrice)
        val savings = kotlin.math.max(0.0, petrolEquivalentCost - actualCngCost)

        // Nearest pump
        val nearest = pumps.firstOrNull { it.isOpen && it.isGasAvailable } ?: pumps.firstOrNull()

        DashboardUiState(
            currentCngPrice = if (avgPrice > 0) (kotlin.math.round(avgPrice * 100.0) / 100.0) else 75.59,
            nearbyPumpsCount = pumps.size,
            openPumpsCount = openPumps.size,
            averageGasPressure = if (avgPressure > 0) (kotlin.math.round(avgPressure)) else 210.0,
            currentMonthExpense = monthExpense,
            currentMonthRefillsCount = monthRefills.size,
            currentMonthCngKg = (kotlin.math.round(monthCngKg * 10.0) / 10.0),
            activeCarMileage = (kotlin.math.round(calculatedMileage * 10.0) / 10.0),
            activeCarName = activeCar?.name ?: "No Vehicle Added",
            activeCar = activeCar,
            allCars = cars,
            estimatedRangeKm = estimatedRange,
            tankFillPercent = tankFillRatio,
            co2SavedKg = co2Saved,
            treesEquivalent = trees,
            monthlyPetrolSavings = kotlin.math.round(savings),
            lastRefill = lastRefillEntry,
            nearestPump = nearest,
            nearestPumpDistanceKm = 1.8,
            nearbyPumpsList = pumps.take(5),
            favoritePumpsList = pumps.filter { it.isFavorite }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun switchActiveCar(carId: Long) {
        viewModelScope.launch {
            repository.setDefaultCar(carId)
        }
    }

    class Factory(private val repository: CngRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
