package com.autorecargaspegaso.network.ocm

import com.autorecargaspegaso.common.AppError
import com.autorecargaspegaso.domain.Charger

interface ChargerRepository {
    suspend fun nearbyChargers(latitude: Double, longitude: Double, distanceKm: Double = 25.0): Result<List<Charger>>

    /**
     * Rectángulo visible del mapa (`north`/`south`/`east`/`west`, grados) —
     * úsalo cuando quieras "todo lo que hay en pantalla" (pan/zoom del
     * usuario), no un círculo alrededor de un punto. Ver
     * [OpenChargeMapApi.chargersInBoundingBox] para el porqué.
     */
    suspend fun chargersInBoundingBox(north: Double, south: Double, east: Double, west: Double): Result<List<Charger>>
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
                // El límite fijo por defecto de la API (100) truncaba zonas
                // con muchos cargadores (p. ej. A Coruña) de forma no
                // determinista -- resultados distintos en cada llamada
                // idéntica, sin orden estable garantizado por OCM al
                // recortar. Se escala con el radio consultado (más área,
                // más resultados esperables) para no truncar en la práctica.
                maxResults = maxResultsFor(distanceKm),
            ).map(mapper::map)
        }.recoverCatching { throwable ->
            throw AppErrorException(AppError.Unexpected(throwable))
        }

    // Tope subido de 2000 a 5000 en proporción al nuevo tope de radio en
    // MapScreen.reportMapMoved (100 -> 500 km): si no se sube también este
    // límite, una zona grande y densa consultada a 500 km se seguía
    // truncando en 2000 resultados aunque la query ya pidiera el radio
    // correcto.
    private fun maxResultsFor(distanceKm: Double): Int = (distanceKm * 20).toInt().coerceIn(200, 5000)

    override suspend fun chargersInBoundingBox(north: Double, south: Double, east: Double, west: Double): Result<List<Charger>> =
        runCatching {
            // Formato exigido por OCM: esquina superior-izquierda (norte,
            // oeste) primero, inferior-derecha (sur, este) después.
            val boundingBox = "($north,$west),($south,$east)"
            api.chargersInBoundingBox(
                boundingBox = boundingBox,
                openDataOnly = true,
                apiKey = apiKey,
                // Misma heurística que maxResultsFor, pero a partir de la
                // diagonal real del rectángulo visible en vez de un radio —
                // sin tope superior de "área" (ese tope, primero 100 km y
                // luego 500 km, era precisamente la causa del corte circular
                // reportado en dispositivo: por diseño, un rectángulo nunca
                // puede producir ese artefacto, solo el radio lo producía).
                // El único límite que queda es maxresults, ya acotado igual
                // que antes (200-5000) — riesgo ya documentado y aceptado:
                // no se sabe si OCM impone un tope propio por debajo de eso,
                // a vigilar en zonas grandes y densas reales.
                maxResults = maxResultsFor(haversineKm(north, west, south, east)),
            ).map(mapper::map)
        }.recoverCatching { throwable ->
            throw AppErrorException(AppError.Unexpected(throwable))
        }
}

/** Fórmula de Haversine — distancia en km entre dos puntos, usada para dimensionar `maxresults` según el tamaño real del rectángulo visible (no para definir el área de la consulta, que ya es exacta con `boundingbox`). */
private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadiusKm * c
}

/** Envuelve un [AppError] como excepción para poder propagarlo dentro de [Result.failure]. */
class AppErrorException(val error: AppError) : Exception(error.toString())
