package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a CNG Station / Dispenser for local data persistence.
 * Enhanced with CNG Margdarshak real-time stock status, provider branding, and trip corridors.
 */
@Entity(tableName = "pumps")
data class CNGStation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val city: String = "Delhi NCR",
    val latitude: Double,
    val longitude: Double,
    val isOpen: Boolean = true,
    val pricePerKg: Double,
    val gasPressureBar: Double = 210.0,
    val isGasAvailable: Boolean = true,
    val stockStatus: String = "AVAILABLE", // AVAILABLE (Green), OUT_OF_STOCK (Red), NEEDS_UPDATE (Amber)
    val queueWaitMinutes: Int = 10,
    val provider: String = "IGL", // IGL, MGL, MNGL, Adani Gas, Torrent Gas, Gujarat Gas, GAIL Gas, HPCL, BPCL, IOCL
    val highwayCorridor: String = "City Local", // e.g. "Mumbai - Pune Expressway", "Delhi - Agra Yamuna Expy", etc.
    val isSmartPick: Boolean = false,
    val reportedByDriver: String = "Driver Community",
    val rating: Double = 4.2,
    val ratingCount: Int = 18,
    val lastUpdatedTime: Long = System.currentTimeMillis(),
    val phone: String = "+91 9876543210",
    val isFavorite: Boolean = false,
    val dataSourceType: String = "Live Community Feed"
)

/**
 * Typealias for backwards compatibility and brevity across the codebase.
 */
typealias Pump = CNGStation
