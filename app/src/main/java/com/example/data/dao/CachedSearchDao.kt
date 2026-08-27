package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CachedSearch
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedSearchDao {
    @Query("SELECT * FROM cached_searches ORDER BY timestamp DESC LIMIT 20")
    fun getAllCachedSearches(): Flow<List<CachedSearch>>

    @Query("SELECT * FROM cached_searches ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentCachedSearches(): List<CachedSearch>

    @Query("SELECT * FROM cached_searches WHERE LOWER(`query`) = LOWER(:query) AND LOWER(city) = LOWER(:city) LIMIT 1")
    suspend fun findCachedSearch(query: String, city: String): CachedSearch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedSearch(cachedSearch: CachedSearch): Long

    @Query("DELETE FROM cached_searches WHERE id = :id")
    suspend fun deleteCachedSearch(id: Long)

    @Query("DELETE FROM cached_searches")
    suspend fun clearAllCache()

    @Query("SELECT count(*) FROM cached_searches")
    suspend fun getCacheCount(): Int
}
