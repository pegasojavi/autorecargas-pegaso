package com.autorecargaspegaso

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.autorecargaspegaso.feature.applauncher.AndroidChargerAppLauncher
import com.autorecargaspegaso.feature.applauncher.JsonProviderDirectory
import com.autorecargaspegaso.feature.applauncher.JsonRoamingPartnerships
import com.autorecargaspegaso.feature.map.ChargerFiltersRepository
import com.autorecargaspegaso.feature.map.DataStoreChargerFiltersRepository
import com.autorecargaspegaso.network.geocoding.GeocodingRepository
import com.autorecargaspegaso.network.geocoding.NominatimClientFactory
import com.autorecargaspegaso.network.geocoding.NominatimGeocodingRepository
import com.autorecargaspegaso.network.ocm.ChargerMapper
import com.autorecargaspegaso.network.ocm.ChargerRepository
import com.autorecargaspegaso.network.ocm.OcmChargerRepository
import com.autorecargaspegaso.network.ocm.OpenChargeMapClientFactory

/**
 * Delegado de propiedad de [Context] para el `DataStore<Preferences>` de
 * filtros del mapa (CLAUDE.md sección 4.1) — debe ser una propiedad de nivel
 * superior en un único fichero, según la recomendación oficial de
 * DataStore, para garantizar una única instancia por proceso aunque se
 * acceda desde varios `Context` (p. ej. `Application` vs. `applicationContext`).
 */
private val Context.mapFiltersDataStore: DataStore<Preferences> by preferencesDataStore(name = "map_filters")

/**
 * Composición manual de dependencias (sin Hilt todavía — CLAUDE.md
 * sección 4 lo pide, pero se difiere hasta fijar versiones de KSP
 * compatibles con AGP 9.0.1/Kotlin 2.3.20; ver informe de scaffolding).
 * Un único punto de construcción, vive en [AutoRecargasPegasoApplication].
 */
class AppContainer(context: Context) {
    private val roamingPartnerships = JsonRoamingPartnerships(context)
    private val chargerMapper = ChargerMapper(roamingPartnerships)
    private val openChargeMapApi = OpenChargeMapClientFactory.create(debugLogging = BuildConfig.DEBUG)

    val providerDirectory = JsonProviderDirectory(context)
    val chargerRepository: ChargerRepository =
        OcmChargerRepository(openChargeMapApi, chargerMapper, apiKey = BuildConfig.OCM_API_KEY)
    val chargerAppLauncher = AndroidChargerAppLauncher(context.applicationContext, providerDirectory)
    val geocodingRepository: GeocodingRepository = NominatimGeocodingRepository(NominatimClientFactory.create())
    val chargerFiltersRepository: ChargerFiltersRepository =
        DataStoreChargerFiltersRepository(context.applicationContext.mapFiltersDataStore)
}
