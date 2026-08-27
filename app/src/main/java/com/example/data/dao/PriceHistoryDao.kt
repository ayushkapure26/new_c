package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PriceHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history ORDER BY timestamp ASC")
    fun getAllPriceHistory(): Flow<List<PriceHistory>>

    @Query("SELECT * FROM price_history WHERE pumpId = :pumpId ORDER BY timestamp ASC")
    fun getPriceHistoryForPump(pumpId: Long): Flow<List<PriceHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(priceHistory: PriceHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPriceHistory(histories: List<PriceHistory>)

    @Query("SELECT count(*) FROM price_history")
    suspend fun getCount(): Int
}
