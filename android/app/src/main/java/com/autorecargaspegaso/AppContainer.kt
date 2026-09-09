package com.autorecargaspegaso

import android.content.Context
import com.autorecargaspegaso.feature.applauncher.AndroidChargerAppLauncher
import com.autorecargaspegaso.feature.applauncher.JsonProviderDirectory
import com.autorecargaspegaso.feature.applauncher.JsonRoamingPartnerships
import com.autorecargaspegaso.network.ocm.ChargerMapper
import com.autorecargaspegaso.network.ocm.ChargerRepository
import com.autorecargaspegaso.network.ocm.OcmChargerRepository
import com.autorecargaspegaso.network.ocm.OpenChargeMapClientFactory

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
}
