package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Pump
import kotlinx.coroutines.flow.Flow

@Dao
interface PumpDao {
    @Query("SELECT * FROM pumps ORDER BY isFavorite DESC, rating DESC")
    fun getAllPumps(): Flow<List<Pump>>

    @Query("SELECT * FROM pumps WHERE id = :pumpId LIMIT 1")
    suspend fun getPumpById(pumpId: Long): Pump?

    @Query("SELECT * FROM pumps WHERE isFavorite = 1")
    fun getFavoritePumps(): Flow<List<Pump>>

    @Query("SELECT * FROM pumps WHERE isFavorite = 1")
    suspend fun getFavoritePumpsList(): List<Pump>

    @Query("SELECT * FROM pumps")
    suspend fun getAllPumpsList(): List<Pump>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPump(pump: Pump): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pumps: List<Pump>)

    @Update
    suspend fun updatePump(pump: Pump)

    @Query("UPDATE pumps SET isFavorite = :isFavorite WHERE id = :pumpId")
    suspend fun setFavorite(pumpId: Long, isFavorite: Boolean)

    @Query("SELECT count(*) FROM pumps")
    suspend fun getPumpCount(): Int
}
