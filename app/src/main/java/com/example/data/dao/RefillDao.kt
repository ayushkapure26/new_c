package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Refill
import kotlinx.coroutines.flow.Flow

@Dao
interface RefillDao {
    @Query("SELECT * FROM refills ORDER BY date DESC, id DESC")
    fun getAllRefills(): Flow<List<Refill>>

    @Query("SELECT * FROM refills WHERE carId = :carId ORDER BY date DESC, id DESC")
    fun getRefillsByCar(carId: Long): Flow<List<Refill>>

    @Query("SELECT * FROM refills WHERE id = :refillId LIMIT 1")
    suspend fun getRefillById(refillId: Long): Refill?

    @Query("SELECT * FROM refills WHERE carId = :carId AND date < :currentDate ORDER BY date DESC LIMIT 1")
    suspend fun getPreviousRefill(carId: Long, currentDate: Long): Refill?

    @Query("SELECT * FROM refills ORDER BY date DESC LIMIT 1")
    fun getLastRefill(): Flow<Refill?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefill(refill: Refill): Long

    @Update
    suspend fun updateRefill(refill: Refill)

    @Delete
    suspend fun deleteRefill(refill: Refill)

    @Query("DELETE FROM refills")
    suspend fun deleteAllRefills()

    @Query("SELECT count(*) FROM refills")
    suspend fun getRefillCount(): Int
}
