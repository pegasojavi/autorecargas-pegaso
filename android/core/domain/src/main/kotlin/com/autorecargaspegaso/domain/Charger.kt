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
    /**
     * Nombre del operador tal como lo da OCM, aunque [nativeProviderId] no
     * esté en `ProviderDirectory` todavía (CLAUDE.md sección 5) — permite
     * mostrar "de qué red es" un cargador aunque aún no sepamos abrir su
     * app. `null` si OCM no trae operador.
     */
    val operatorDisplayName: String? = null,
) {
    /** Todas las apps candidatas para este cargador, nativa incluida, sin duplicados. */
    val allProviderIds: List<String>
        get() = (listOf(nativeProviderId) + roamingProviderIds).distinct()
}
