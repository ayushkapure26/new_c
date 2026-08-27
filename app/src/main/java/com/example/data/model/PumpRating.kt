package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pump_ratings",
    foreignKeys = [
        ForeignKey(
            entity = Pump::class,
            parentColumns = ["id"],
            childColumns = ["pumpId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pumpId")]
)
data class PumpRating(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pumpId: Long,
    val userName: String = "CNG Driver",
    val overallRating: Float,
    val gasAvailabilityRating: Float = 4.0f,
    val pressureRating: Float = 4.0f,
    val waitingTimeRating: Float = 3.5f,
    val staffRating: Float = 4.0f,
    val cleanlinessRating: Float = 4.0f,
    val priceAccuracyRating: Float = 5.0f,
    val reviewText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
