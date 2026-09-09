package com.autorecargaspegaso.network.geocoding

data class GeocodedPlace(val latitude: Double, val longitude: Double, val label: String)

interface GeocodingRepository {
    suspend fun search(query: String): Result<GeocodedPlace>
}

class NominatimGeocodingRepository(
    private val api: NominatimApi,
) : GeocodingRepository {

    override suspend fun search(query: String): Result<GeocodedPlace> = runCatching {
        val results = api.search(query = query)
        val first = results.firstOrNull() ?: error("Sin resultados para \"$query\"")
        GeocodedPlace(
            latitude = first.latitude.toDouble(),
            longitude = first.longitude.toDouble(),
            label = first.displayName,
        )
    }
}
