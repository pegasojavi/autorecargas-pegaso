package com.autorecargaspegaso.feature.applauncher

import kotlinx.serialization.Serializable

@Serializable
internal data class ProvidersFileDto(
    val providers: List<ProviderAppInfoDto>,
)

@Serializable
internal data class ProviderAppInfoDto(
    val providerId: String,
    val displayName: String,
    val androidPackage: String,
    val deepLinkScheme: String? = null,
    val playStoreId: String,
)

@Serializable
internal data class RoamingFileDto(
    val partnerships: Map<String, List<String>>,
)
