package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val regNumber: String,
    val tankCapacityKg: Double,
    val fuelType: String = "CNG + Petrol",
    val currentOdometer: Double,
    val expectedMileage: Double = 22.0,
    val notes: String = "",
    val isDefault: Boolean = false
)
