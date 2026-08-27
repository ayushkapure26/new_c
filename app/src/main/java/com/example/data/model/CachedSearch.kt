package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_searches")
data class CachedSearch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val resultCount: Int = 0,
    val previewStationNames: String = ""
)
