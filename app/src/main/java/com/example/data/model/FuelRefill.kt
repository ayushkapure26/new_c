package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Fuel/CNG Refill entry for local data persistence.
 */
@Entity(
    tableName = "refills",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId"), Index("pumpId")]
)
data class FuelRefill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val carId: Long,
    val pumpId: Long? = null,
    val pumpName: String = "",
    val date: Long = System.currentTimeMillis(),
    val timeFormatted: String = "",
    val odometer: Double,
    val quantityKg: Double,
    val pricePerKg: Double,
    val totalAmount: Double,
    val isFullRefill: Boolean = true,
    val notes: String = "",
    val distanceTravelled: Double = 0.0,
    val mileageKmPerKg: Double = 0.0,
    val costPerKm: Double = 0.0
)

/**
 * Typealias for backwards compatibility and brevity across the codebase.
 */
typealias Refill = FuelRefill
