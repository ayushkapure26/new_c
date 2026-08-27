package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_history")
data class PriceHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pumpId: Long,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pressure_history")
data class PressureHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pumpId: Long,
    val pressureBar: Double,
    val timestamp: Long = System.currentTimeMillis()
)
