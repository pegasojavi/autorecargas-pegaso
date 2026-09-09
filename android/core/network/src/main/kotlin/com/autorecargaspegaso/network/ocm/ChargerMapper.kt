package com.autorecargaspegaso.network.ocm

import com.autorecargaspegaso.domain.Charger
import com.autorecargaspegaso.domain.Connector
import com.autorecargaspegaso.domain.ConnectorType
import com.autorecargaspegaso.domain.RoamingPartnerships

/**
 * Traduce el DTO de OCM al modelo de dominio [Charger], resolviendo
 * `roamingProviderIds` vía [RoamingPartnerships] (CLAUDE.md sección 5).
 *
 * **Importante (corregido 2026-09-09, bug real detectado en dispositivo):**
 * un cargador cuyo operador NO está en [OcmOperatorMapping] **ya no se
 * descarta**. Antes se ocultaba silenciosamente (`return null`), lo que
 * hacía desaparecer la mayoría de cargadores reales de una zona (operadores
 * pequeños/independientes, o redes de las 13 documentadas que aún no están
 * en el mapeo) — el mapa parecía casi vacío sin ningún error visible. Ahora
 * se muestra siempre, con `nativeProviderId = "unmapped:<slug>"` y
 * [Charger.operatorDisplayName] con el nombre real de OCM, para que la UI
 * pueda mostrar de qué red es aunque `ChargerAppLauncher` todavía no sepa
 * abrir su app (devuelve `NoProviderInfo`, gestionado por la UI).
 */
class ChargerMapper(
    private val roamingPartnerships: RoamingPartnerships,
) {
    fun map(dto: OcmPoiDto): Charger {
        val operatorTitle = dto.operatorInfo?.title
        val mappedProviderId = OcmOperatorMapping.providerIdFor(operatorTitle)
        val providerId = mappedProviderId ?: "unmapped:${slugify(operatorTitle)}"

        return Charger(
            id = dto.uuid ?: dto.id.toString(),
            name = dto.addressInfo.title ?: operatorTitle ?: "Cargador",
            latitude = dto.addressInfo.latitude,
            longitude = dto.addressInfo.longitude,
            address = listOfNotNull(dto.addressInfo.addressLine1, dto.addressInfo.town)
                .joinToString(", ")
                .ifBlank { null },
            connectors = dto.connections.map(::mapConnector),
            nativeProviderId = providerId,
            // Solo tiene sentido buscar roaming si el operador nativo está
            // identificado — un id sintético "unmapped:*" nunca tendrá
            // entradas en RoamingPartnerships.
            roamingProviderIds = if (mappedProviderId != null) roamingPartnerships.rolesFor(mappedProviderId) else emptyList(),
            operatorDisplayName = operatorTitle,
        )
    }

    private fun slugify(title: String?): String =
        title?.lowercase()?.replace(Regex("[^a-z0-9]+"), "-")?.trim('-')?.ifBlank { null } ?: "desconocido"

    private fun mapConnector(dto: OcmConnectionDto): Connector = Connector(
        type = mapConnectorType(dto.connectionType?.title),
        powerKw = dto.powerKw,
    )

    private fun mapConnectorType(title: String?): ConnectorType = when {
        title == null -> ConnectorType.UNKNOWN
        title.contains("Type 2", ignoreCase = true) -> ConnectorType.TYPE_2
        title.contains("CCS", ignoreCase = true) -> ConnectorType.CCS
        title.contains("CHAdeMO", ignoreCase = true) -> ConnectorType.CHADEMO
        else -> ConnectorType.UNKNOWN
    }
}
