package com.autorecargaspegaso.network.ocm

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Cliente de solo lectura contra Open Charge Map (CLAUDE.md secciones 4/5).
 * `opendata=true` es obligatorio en toda petición — nunca se debe consumir
 * ni mostrar un punto con licencia restringida como si fuera abierto.
 */
interface OpenChargeMapApi {
    @GET("v3/poi")
    suspend fun nearbyChargers(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("distance") distanceKm: Double = 25.0,
        @Query("distanceunit") distanceUnit: String = "KM",
        @Query("maxresults") maxResults: Int = 100,
        @Query("opendata") openDataOnly: Boolean = true,
        @Query("compact") compact: Boolean = true,
        @Query("verbose") verbose: Boolean = false,
        // Obligatoria (confirmado en vivo, 2026-09-09): OCM devuelve 403
        // sin ella en /v3/poi, ya no es "opcional para volumen bajo" como
        // se documentó al elegir el dataset (CLAUDE.md sección 0/5). Sin
        // una clave real configurada en OpenChargeMapClientFactory, toda
        // llamada falla con 403 — no es un bug de este cliente.
        @Query("key") apiKey: String,
    ): List<OcmPoiDto>

    companion object {
        const val BASE_URL = "https://api.openchargemap.io/"
    }
}
