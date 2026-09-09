package com.autorecargaspegaso.network.ocm

import com.autorecargaspegaso.common.AppError
import com.autorecargaspegaso.domain.Charger

interface ChargerRepository {
    suspend fun nearbyChargers(latitude: Double, longitude: Double, distanceKm: Double = 25.0): Result<List<Charger>>
}

class OcmChargerRepository(
    private val api: OpenChargeMapApi,
    private val mapper: ChargerMapper,
) : ChargerRepository {

    override suspend fun nearbyChargers(latitude: Double, longitude: Double, distanceKm: Double): Result<List<Charger>> =
        runCatching {
            api.nearbyChargers(latitude = latitude, longitude = longitude, distanceKm = distanceKm, openDataOnly = true)
                .mapNotNull(mapper::map)
        }.recoverCatching { throwable ->
            throw AppErrorException(AppError.Unexpected(throwable))
        }
}

/** Envuelve un [AppError] como excepción para poder propagarlo dentro de [Result.failure]. */
class AppErrorException(val error: AppError) : Exception(error.toString())
