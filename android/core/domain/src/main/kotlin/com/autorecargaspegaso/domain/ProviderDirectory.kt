package com.autorecargaspegaso.domain

/**
 * Metadatos de lanzamiento de la app de un operador (CLAUDE.md sección 3).
 * Puebla esto una tabla estática embebida en la app (JSON en `assets`),
 * nunca una llamada a un backend propio.
 */
data class ProviderAppInfo(
    val providerId: String,
    val displayName: String,
    val androidPackage: String,
    val deepLinkScheme: String?,
    val playStoreId: String,
)

/** Fuente de verdad de qué operadores conoce la app y sus datos de lanzamiento. */
interface ProviderDirectory {
    fun find(providerId: String): ProviderAppInfo?

    fun findAll(providerIds: List<String>): List<ProviderAppInfo> =
        providerIds.mapNotNull(::find)
}
