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
        // TODO: añadir @Query("key") con una API key propia de OCM antes de
        // producción — funciona sin ella para volúmenes bajos, pero con
        // límite de tasa más estricto (CLAUDE.md sección 0/5).
    ): List<OcmPoiDto>

    companion object {
        const val BASE_URL = "https://api.openchargemap.io/"
    }
}
