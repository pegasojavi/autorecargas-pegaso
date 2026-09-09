package com.autorecargaspegaso.network.geocoding

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Geocodificación (buscador de dirección/ciudad, CLAUDE.md sección 2) vía
 * Nominatim (OpenStreetMap) — mismo criterio que el mapa (osmdroid): sin
 * clave, sin cuenta. Política de uso de Nominatim: exige un `User-Agent`
 * identificable (obligatorio, ver [NominatimClientFactory]) y máx. ~1
 * petición/segundo — aceptable para una búsqueda puntual iniciada por el
 * usuario, nunca en bucle automático.
 */
interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 1,
        @Header("User-Agent") userAgent: String = NominatimClientFactory.USER_AGENT,
    ): List<NominatimResultDto>

    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
    }
}

@Serializable
data class NominatimResultDto(
    @SerialName("lat") val latitude: String,
    @SerialName("lon") val longitude: String,
    @SerialName("display_name") val displayName: String,
)
