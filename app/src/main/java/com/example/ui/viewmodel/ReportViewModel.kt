package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.PriceHistory
import com.example.data.model.Refill
import com.example.data.repository.CngRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimePeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class ChartDataPoint(
    val label: String,
    val value: Float
)

data class ReportSummaryState(
    val selectedPeriod: TimePeriod = TimePeriod.MONTHLY,
    val totalExpense: Double = 0.0,
    val totalQuantityKg: Double = 0.0,
    val totalRefillsCount: Int = 0,
    val averagePricePerKg: Double = 0.0,
    val averageMileage: Double = 0.0,
    val averageCostPerKm: Double = 0.0,
    val totalDistanceKm: Double = 0.0,
    val expenseChartPoints: List<ChartDataPoint> = emptyList(),
    val mileageChartPoints: List<ChartDataPoint> = emptyList(),
    val priceHistoryChartPoints: List<ChartDataPoint> = emptyList(),
    val minPriceHistory: Double = 0.0,
    val maxPriceHistory: Double = 0.0,
    val latestPriceHistory: Double = 0.0
)

class ReportViewModel(private val repository: CngRepository) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTHLY)

    val allRefills: StateFlow<List<Refill>> = repository.allRefills.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCars: StateFlow<List<com.example.data.model.Car>> = repository.allCars.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val reportState: StateFlow<ReportSummaryState> = combine(
        repository.allRefills,
        _selectedPeriod,
        repository.priceHistory
    ) { refills, period, priceHistories ->
        calculateReport(refills, period, priceHistories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportSummaryState()
    )

    fun setTimePeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    private fun calculateReport(
        allRefills: List<Refill>,
        period: TimePeriod,
        priceHistories: List<PriceHistory>
    ): ReportSummaryState {
        val now = Calendar.getInstance()
        val filteredRefills = when (period) {
            TimePeriod.WEEKLY -> {
                val sevenDaysAgo = now.timeInMillis - (7 * 86400000L)
                allRefills.filter { it.date >= sevenDaysAgo }
            }
            TimePeriod.MONTHLY -> {
                val currentMonth = now.get(Calendar.MONTH)
                val currentYear = now.get(Calendar.YEAR)
                allRefills.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
            }
            TimePeriod.YEARLY -> {
                val currentYear = now.get(Calendar.YEAR)
                allRefills.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    cal.get(Calendar.YEAR) == currentYear
                }
            }
        }

        val totalExpense = filteredRefills.sumOf { it.totalAmount }
        val totalKg = filteredRefills.sumOf { it.quantityKg }
        val refillCount = filteredRefills.size
        val avgPrice = if (totalKg > 0) totalExpense / totalKg else 0.0

        val validMileages = filteredRefills.map { it.mileageKmPerKg }.filter { it > 0 }
        val avgMileage = if (validMileages.isNotEmpty()) validMileages.average() else 0.0

        val totalDist = filteredRefills.sumOf { it.distanceTravelled }
        val avgCostPerKm = if (totalDist > 0) totalExpense / totalDist else 0.0

        // Build Refill Expense/Mileage Chart points
        val expensePoints = mutableListOf<ChartDataPoint>()
        val mileagePoints = mutableListOf<ChartDataPoint>()

        if (period == TimePeriod.WEEKLY) {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            days.forEachIndexed { idx, day ->
                val dayExpense = filteredRefills.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday is 1
                    val normalizedIdx = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                    normalizedIdx == idx
                }.sumOf { it.totalAmount }.toFloat()

                expensePoints.add(ChartDataPoint(day, dayExpense))
            }
        } else {
            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            months.forEachIndexed { idx, monthName ->
                val monthExpense = allRefills.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    cal.get(Calendar.MONTH) == idx
                }.sumOf { it.totalAmount }.toFloat()

                val monthMileage = allRefills.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    cal.get(Calendar.MONTH) == idx && it.mileageKmPerKg > 0
                }.map { it.mileageKmPerKg }.let { if (it.isNotEmpty()) it.average().toFloat() else 0f }

                expensePoints.add(ChartDataPoint(monthName, monthExpense))
                mileagePoints.add(ChartDataPoint(monthName, monthMileage))
            }
        }

        // Process Stored PriceHistory Data
        val priceHistoryPoints = mutableListOf<ChartDataPoint>()
        val monthFormat = SimpleDateFormat("MMM", Locale.US)

        val sortedHistories = priceHistories.sortedBy { it.timestamp }
        val minPrice = if (sortedHistories.isNotEmpty()) sortedHistories.minOf { it.price } else 0.0
        val maxPrice = if (sortedHistories.isNotEmpty()) sortedHistories.maxOf { it.price } else 0.0
        val latestPrice = if (sortedHistories.isNotEmpty()) sortedHistories.last().price else 0.0

        if (sortedHistories.isNotEmpty()) {
            sortedHistories.forEach { ph ->
                val monthLabel = monthFormat.format(Date(ph.timestamp))
                priceHistoryPoints.add(ChartDataPoint(monthLabel, ph.price.toFloat()))
            }
        } else {
            // Fallback derived from Refill records if PriceHistory entity is empty
            val refillsByMonth = allRefills.groupBy {
                monthFormat.format(Date(it.date))
            }
            refillsByMonth.forEach { (monthLabel, list) ->
                val avgMonthPrice = list.map { it.pricePerKg }.average().toFloat()
                if (avgMonthPrice > 0) {
                    priceHistoryPoints.add(ChartDataPoint(monthLabel, avgMonthPrice))
                }
            }
        }

        return ReportSummaryState(
            selectedPeriod = period,
            totalExpense = totalExpense,
            totalQuantityKg = totalKg,
            totalRefillsCount = refillCount,
            averagePricePerKg = avgPrice,
            averageMileage = avgMileage,
            averageCostPerKm = avgCostPerKm,
            totalDistanceKm = totalDist,
            expenseChartPoints = expensePoints,
            mileageChartPoints = mileagePoints,
            priceHistoryChartPoints = priceHistoryPoints,
            minPriceHistory = minPrice,
            maxPriceHistory = maxPrice,
            latestPriceHistory = latestPrice
        )
    }

    class Factory(private val repository: CngRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportViewModel(repository) as T
        }
    }
}
