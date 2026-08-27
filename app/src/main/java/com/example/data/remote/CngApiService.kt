package com.example.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class ApiPumpDto(
    val id: Long,
    val name: String,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val isOpen: Boolean,
    val pricePerKg: Double,
    val gasPressureBar: Double,
    val isGasAvailable: Boolean,
    val rating: Double,
    val ratingCount: Int,
    val lastUpdatedTime: Long,
    val phone: String,
    val dataSourceType: String
)

interface CngApiService {
    @GET("pumps/nearby")
    suspend fun getNearbyPumps(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radiusKm") radiusKm: Double = 25.0
    ): Response<List<ApiPumpDto>>

    @GET("pumps/{id}/live-status")
    suspend fun getLiveStatus(
        @Path("id") pumpId: Long
    ): Response<ApiPumpDto>
}
