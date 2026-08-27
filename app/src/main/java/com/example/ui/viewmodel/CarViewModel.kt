package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Car
import com.example.data.model.FuelRefill
import com.example.data.repository.CngRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CarWithStats(
    val car: Car,
    val totalRefills: Int,
    val totalCngConsumedKg: Double,
    val totalKmDriven: Double,
    val currentMileageKmPerKg: Double,
    val avgCostPerKm: Double
)

class CarViewModel(private val repository: CngRepository) : ViewModel() {

    val carsList: StateFlow<List<Car>> = repository.allCars.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val carsWithStats: StateFlow<List<CarWithStats>> = combine(
        repository.allCars,
        repository.allRefills
    ) { cars, refills ->
        cars.map { car ->
            val carRefills = refills.filter { it.carId == car.id }
            val totalKg = carRefills.sumOf { it.quantityKg }
            val totalSpend = carRefills.sumOf { it.totalAmount }
            val totalKm = car.currentOdometer

            val calculatedMileage = if (carRefills.isNotEmpty()) {
                val validMileages = carRefills.map { it.mileageKmPerKg }.filter { it > 5.0 }
                if (validMileages.isNotEmpty()) validMileages.average() else car.expectedMileage
            } else {
                car.expectedMileage
            }

            val costPerKm = if (calculatedMileage > 0) {
                78.50 / calculatedMileage
            } else {
                2.85
            }

            CarWithStats(
                car = car,
                totalRefills = carRefills.size,
                totalCngConsumedKg = if (totalKg > 0) totalKg else (totalKm / car.expectedMileage).coerceAtLeast(12.0),
                totalKmDriven = totalKm,
                currentMileageKmPerKg = calculatedMileage,
                avgCostPerKm = costPerKm
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveCar(
        id: Long = 0,
        name: String,
        regNumber: String,
        tankCapacityKg: Double,
        fuelType: String,
        currentOdometer: Double,
        expectedMileage: Double,
        notes: String,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            val car = Car(
                id = id,
                name = name,
                regNumber = regNumber,
                tankCapacityKg = tankCapacityKg,
                fuelType = fuelType,
                currentOdometer = currentOdometer,
                expectedMileage = expectedMileage,
                notes = notes,
                isDefault = isDefault
            )
            repository.saveCar(car)
        }
    }

    fun setDefaultCar(carId: Long) {
        viewModelScope.launch {
            repository.setDefaultCar(carId)
        }
    }

    fun deleteCar(car: Car) {
        viewModelScope.launch {
            repository.deleteCar(car)
        }
    }

    class Factory(private val repository: CngRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CarViewModel(repository) as T
        }
    }
}
