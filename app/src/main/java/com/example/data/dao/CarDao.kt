package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Car
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY isDefault DESC, id ASC")
    fun getAllCars(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE id = :carId LIMIT 1")
    suspend fun getCarById(carId: Long): Car?

    @Query("SELECT * FROM cars WHERE isDefault = 1 LIMIT 1")
    fun getDefaultCar(): Flow<Car?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: Car): Long

    @Update
    suspend fun updateCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)

    @Query("UPDATE cars SET isDefault = 0")
    suspend fun clearDefaultCars()

    @Query("UPDATE cars SET isDefault = 1 WHERE id = :carId")
    suspend fun setDefaultCar(carId: Long)

    @Query("SELECT count(*) FROM cars")
    suspend fun getCarCount(): Int
}
