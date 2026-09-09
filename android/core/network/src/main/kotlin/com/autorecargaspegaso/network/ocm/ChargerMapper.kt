package com.autorecargaspegaso.network.ocm

import com.autorecargaspegaso.domain.Charger
import com.autorecargaspegaso.domain.Connector
import com.autorecargaspegaso.domain.ConnectorType
import com.autorecargaspegaso.domain.RoamingPartnerships

/**
 * Traduce el DTO de OCM al modelo de dominio [Charger], resolviendo
 * `roamingProviderIds` vía [RoamingPartnerships] (CLAUDE.md sección 5).
 *
 * Los puntos cuyo operador no está en [OcmOperatorMapping] se descartan
 * (devuelve `null`) en vez de asociarlos a una app al azar — es preferible
 * no mostrar el punto a mandar al usuario a una app incorrecta.
 */
class ChargerMapper(
    private val roamingPartnerships: RoamingPartnerships,
) {
    fun map(dto: OcmPoiDto): Charger? {
        val providerId = OcmOperatorMapping.providerIdFor(dto.operatorInfo?.title) ?: return null

        return Charger(
            id = dto.uuid ?: dto.id.toString(),
            name = dto.addressInfo.title ?: dto.operatorInfo?.title ?: "Cargador",
            latitude = dto.addressInfo.latitude,
            longitude = dto.addressInfo.longitude,
            address = listOfNotNull(dto.addressInfo.addressLine1, dto.addressInfo.town)
                .joinToString(", ")
                .ifBlank { null },
            connectors = dto.connections.map(::mapConnector),
            nativeProviderId = providerId,
            roamingProviderIds = roamingPartnerships.rolesFor(providerId),
        )
    }

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
