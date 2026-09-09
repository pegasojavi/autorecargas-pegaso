package com.autorecargaspegaso.domain

/** Tipo de conector físico del cargador (ampliar según lo que exponga OCM). */
enum class ConnectorType {
    TYPE_2,
    CCS,
    CHADEMO,
    UNKNOWN,
}

data class Connector(
    val type: ConnectorType,
    val powerKw: Double?,
)

/**
 * Un cargador tal como lo consume la app, ya mapeado desde el DTO de Open
 * Charge Map (CLAUDE.md sección 5) a nuestro propio modelo de dominio.
 *
 * [nativeProviderId] es el operador real del punto; [roamingProviderIds]
 * son apps de terceros que también dan acceso a él, resueltas a partir de
 * `RoamingPartnerships` (docs/providers/roaming-agreements.md) — nunca
 * vienen directamente de OCM.
 */
data class Charger(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val connectors: List<Connector>,
    val nativeProviderId: String,
    val roamingProviderIds: List<String> = emptyList(),
) {
    /** Todas las apps candidatas para este cargador, nativa incluida, sin duplicados. */
    val allProviderIds: List<String>
        get() = (listOf(nativeProviderId) + roamingProviderIds).distinct()
}
