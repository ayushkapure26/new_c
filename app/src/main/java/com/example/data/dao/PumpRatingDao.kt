package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PumpRating
import kotlinx.coroutines.flow.Flow

@Dao
interface PumpRatingDao {
    @Query("SELECT * FROM pump_ratings WHERE pumpId = :pumpId ORDER BY timestamp DESC")
    fun getRatingsForPump(pumpId: Long): Flow<List<PumpRating>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: PumpRating): Long
}
