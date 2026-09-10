package com.autorecargaspegaso.feature.applauncher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.autorecargaspegaso.domain.Charger
import com.autorecargaspegaso.domain.ChargerAppLauncher
import com.autorecargaspegaso.domain.LaunchResult
import com.autorecargaspegaso.domain.ProviderAppInfo
import com.autorecargaspegaso.domain.ProviderDirectory

/**
 * Implementación Android de [ChargerAppLauncher] (CLAUDE.md secciones 0/3,
 * revisión 2026-09-10 de la regla de resolución multi-app): resuelve sin
 * preguntar al usuario cuando hay una única app candidata instalada (esa
 * gana) o ninguna instalada (gana la del operador nativo); si hay dos o más
 * candidatas instaladas a la vez, devuelve [LaunchResult.NeedsDisambiguation]
 * en vez de decidir sola, para que el llamador muestre un selector.
 */
class AndroidChargerAppLauncher(
    private val context: Context,
    private val providerDirectory: ProviderDirectory,
) : ChargerAppLauncher {

    override fun resolve(charger: Charger): LaunchResult {
        val candidates = providerDirectory.findAll(charger.allProviderIds)
        if (candidates.isEmpty()) return LaunchResult.NoProviderInfo

        val installed = candidates.filter { isInstalled(it.androidPackage) }

        if (installed.size >= 2) {
            return LaunchResult.NeedsDisambiguation(installed)
        }

        val chosen = when {
            installed.size == 1 -> installed.single()
            else -> candidates.firstOrNull { it.providerId == charger.nativeProviderId } ?: candidates.first()
        }

        return if (isInstalled(chosen.androidPackage)) {
            LaunchResult.OpenedApp(chosen)
        } else {
            LaunchResult.OpenedStore(chosen)
        }
    }

    override fun launch(result: LaunchResult) {
        when (result) {
            is LaunchResult.OpenedApp -> launchApp(result.provider)
            is LaunchResult.OpenedStore -> launchStore(result.provider)
            LaunchResult.NoProviderInfo -> Unit // nada que lanzar; la UI debe haber filtrado este caso antes
            is LaunchResult.NeedsDisambiguation -> Unit // la UI debe mostrar el selector y volver a llamar con la elegida
        }
    }

    /** Acción "Cómo llegar" (CLAUDE.md sección 3): independiente del lanzador de apps de proveedor. */
    fun launchDirections(charger: Charger) {
        val uri = Uri.parse("geo:${charger.latitude},${charger.longitude}?q=${charger.latitude},${charger.longitude}(${Uri.encode(charger.name)})")
        safeStartActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun launchApp(provider: ProviderAppInfo) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(provider.androidPackage)
        if (launchIntent != null) {
            safeStartActivity(launchIntent)
        } else {
            launchStore(provider)
        }
    }

    private fun launchStore(provider: ProviderAppInfo) {
        // `market://` es un esquema genérico: en fabricantes con tienda propia
        // (Huawei AppGallery, Xiaomi GetApps, "App Mall" y similares) esa app
        // puede registrarse también para manejarlo y ganarle a Google Play si
        // no se fuerza el paquete explícitamente. CLAUDE.md sección 0/3 asume
        // Google Play como tienda de referencia en Android — forzar
        // `com.android.vending` es obligatorio, no opcional.
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${provider.playStoreId}"))
            .setPackage(GOOGLE_PLAY_PACKAGE)
        if (!safeStartActivity(marketIntent)) {
            // Sin Google Play instalado (o si el intent anterior falla por
            // cualquier motivo): ficha web, que abre en el navegador y no
            // depende de qué tienda de apps tenga el fabricante por defecto.
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${provider.playStoreId}"),
            )
            safeStartActivity(webIntent)
        }
    }

    private fun isInstalled(packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    private fun safeStartActivity(intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private companion object {
        const val GOOGLE_PLAY_PACKAGE = "com.android.vending"
    }
}
