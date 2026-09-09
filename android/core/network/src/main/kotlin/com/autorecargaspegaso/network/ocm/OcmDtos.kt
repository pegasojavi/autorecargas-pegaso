package com.autorecargaspegaso.network.ocm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subconjunto de campos de la respuesta real de la API de Open Charge Map
 * (CLAUDE.md sección 5) que necesitamos — no todo el esquema. Ver
 * https://openchargemap.org/site/develop/api para el esquema completo.
 *
 * Solo se consultan puntos con `opendata=true` (parámetro de query, ver
 * [OpenChargeMapApi]) — este DTO no distingue eso, es responsabilidad de
 * quien construye la petición no pedir nunca datos no abiertos.
 */
@Serializable
data class OcmPoiDto(
    @SerialName("ID") val id: Int,
    @SerialName("UUID") val uuid: String? = null,
    @SerialName("AddressInfo") val addressInfo: OcmAddressInfoDto,
    @SerialName("Connections") val connections: List<OcmConnectionDto> = emptyList(),
    @SerialName("OperatorInfo") val operatorInfo: OcmOperatorInfoDto? = null,
    @SerialName("OperatorID") val operatorId: Int? = null,
    @SerialName("StatusType") val statusType: OcmStatusTypeDto? = null,
)

@Serializable
data class OcmAddressInfoDto(
    @SerialName("Title") val title: String? = null,
    @SerialName("AddressLine1") val addressLine1: String? = null,
    @SerialName("Town") val town: String? = null,
    @SerialName("Postcode") val postcode: String? = null,
    @SerialName("Latitude") val latitude: Double,
    @SerialName("Longitude") val longitude: Double,
)

@Serializable
data class OcmConnectionDto(
    @SerialName("ConnectionType") val connectionType: OcmConnectionTypeDto? = null,
    @SerialName("PowerKW") val powerKw: Double? = null,
    @SerialName("Quantity") val quantity: Int? = null,
)

@Serializable
data class OcmConnectionTypeDto(
    @SerialName("Title") val title: String? = null,
)

@Serializable
data class OcmOperatorInfoDto(
    @SerialName("ID") val id: Int? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("WebsiteURL") val websiteUrl: String? = null,
)

@Serializable
data class OcmStatusTypeDto(
    @SerialName("IsOperational") val isOperational: Boolean? = null,
    @SerialName("Title") val title: String? = null,
)
