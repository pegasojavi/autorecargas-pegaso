package com.autorecargaspegaso.network.ocm

import com.autorecargaspegaso.common.AppError
import com.autorecargaspegaso.domain.Charger

interface ChargerRepository {
    suspend fun nearbyChargers(latitude: Double, longitude: Double, distanceKm: Double = 25.0): Result<List<Charger>>
}

class OcmChargerRepository(
    private val api: OpenChargeMapApi,
    private val mapper: ChargerMapper,
    /**
     * Clave de OCM (CLAUDE.md sección 0/5, obligatoria desde 2026-09-09).
     * Vacía por defecto: hasta que se dé de alta una real, toda llamada
     * fallará con 403 — comportamiento esperado, no un bug de este
     * repositorio. Registro gratuito en https://openchargemap.org.
     */
    private val apiKey: String,
) : ChargerRepository {

    override suspend fun nearbyChargers(latitude: Double, longitude: Double, distanceKm: Double): Result<List<Charger>> =
        runCatching {
            api.nearbyChargers(
                latitude = latitude,
                longitude = longitude,
                distanceKm = distanceKm,
                openDataOnly = true,
                apiKey = apiKey,
            ).mapNotNull(mapper::map)
        }.recoverCatching { throwable ->
            throw AppErrorException(AppError.Unexpected(throwable))
        }
}

/** Envuelve un [AppError] como excepción para poder propagarlo dentro de [Result.failure]. */
class AppErrorException(val error: AppError) : Exception(error.toString())
