package com.autorecargaspegaso.feature.applauncher

import android.content.Context
import com.autorecargaspegaso.domain.ProviderAppInfo
import com.autorecargaspegaso.domain.ProviderDirectory
import com.autorecargaspegaso.domain.RoamingPartnerships
import kotlinx.serialization.json.Json

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * `ProviderDirectory` respaldado por `assets/providers.json` — tabla
 * estática embebida en la app, nunca una llamada a un backend propio
 * (CLAUDE.md sección 3). Datos poblados desde `docs/providers/`.
 */
class JsonProviderDirectory(context: Context) : ProviderDirectory {

    private val byId: Map<String, ProviderAppInfo> = context.assets
        .open("providers.json")
        .bufferedReader()
        .use { it.readText() }
        .let { lenientJson.decodeFromString(ProvidersFileDto.serializer(), it) }
        .providers
        .associate { dto ->
            dto.providerId to ProviderAppInfo(
                providerId = dto.providerId,
                displayName = dto.displayName,
                androidPackage = dto.androidPackage,
                deepLinkScheme = dto.deepLinkScheme,
                playStoreId = dto.playStoreId,
            )
        }

    override fun find(providerId: String): ProviderAppInfo? = byId[providerId]
}

/**
 * `RoamingPartnerships` respaldado por `assets/roaming.json`
 * (docs/providers/roaming-agreements.md) — igual de estático que
 * [JsonProviderDirectory], nunca deriva esto del dataset de OCM.
 */
class JsonRoamingPartnerships(context: Context) : RoamingPartnerships {

    private val partnerships: Map<String, List<String>> = context.assets
        .open("roaming.json")
        .bufferedReader()
        .use { it.readText() }
        .let { lenientJson.decodeFromString(RoamingFileDto.serializer(), it) }
        .partnerships

    override fun rolesFor(nativeProviderId: String): List<String> =
        partnerships[nativeProviderId].orEmpty()
}
