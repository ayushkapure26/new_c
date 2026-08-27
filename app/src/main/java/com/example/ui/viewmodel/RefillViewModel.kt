package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Car
import com.example.data.model.Pump
import com.example.data.model.Refill
import com.example.data.repository.CngRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RefillViewModel(private val repository: CngRepository) : ViewModel() {

    val refillsList: StateFlow<List<Refill>> = repository.allRefills.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val carsList: StateFlow<List<Car>> = repository.allCars.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pumpsList: StateFlow<List<Pump>> = repository.allPumps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveRefill(
        id: Long = 0,
        carId: Long,
        pumpId: Long?,
        pumpName: String,
        odometer: Double,
        quantityKg: Double,
        pricePerKg: Double,
        totalAmount: Double,
        isFullRefill: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeFormatted = timeFormatter.format(Date())

            val refill = Refill(
                id = id,
                carId = carId,
                pumpId = pumpId,
                pumpName = if (pumpName.isNotBlank()) pumpName else "CNG Station",
                date = System.currentTimeMillis(),
                timeFormatted = timeFormatted,
                odometer = odometer,
                quantityKg = quantityKg,
                pricePerKg = pricePerKg,
                totalAmount = if (totalAmount > 0) totalAmount else (quantityKg * pricePerKg),
                isFullRefill = isFullRefill,
                notes = notes
            )

            repository.saveRefill(refill)
        }
    }

    fun deleteRefill(refill: Refill) {
        viewModelScope.launch {
            repository.deleteRefill(refill)
        }
    }

    class Factory(private val repository: CngRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RefillViewModel(repository) as T
        }
    }
}
