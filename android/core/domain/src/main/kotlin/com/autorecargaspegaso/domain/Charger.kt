package com.autorecargaspegaso.domain

/**
 * Tipo de conector físico del cargador. Los 9 tipos relevantes en Europa
 * (a petición del usuario), verificados contra la lista real de
 * `ConnectionTypes` de Open Charge Map (`GET /v3/referencedata`) — OCM
 * distingue explícitamente CCS de Type 1 y de Type 2 (p. ej. "CCS (Type 2)"
 * como título propio), así que CCS1/CCS2 se mapean por separado, no como un
 * único "CCS" genérico. [UNKNOWN] es un cajón de sastre interno (conectores
 * fuera de Europa como NEMA/GB-T/AS 3112, o sin dato) — no se muestra como
 * chip de filtro propio.
 */
enum class ConnectorType {
    TYPE_1,
    TYPE_2,
    TYPE_3,
    CCS1,
    CCS2,
    CHADEMO,
    TESLA,
    DOMESTIC,
    WIRELESS,
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
