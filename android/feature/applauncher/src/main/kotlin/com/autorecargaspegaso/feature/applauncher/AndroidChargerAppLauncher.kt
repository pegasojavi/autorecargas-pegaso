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
 * Implementación Android de [ChargerAppLauncher] (CLAUDE.md secciones 0/3):
 * resuelve sin preguntar al usuario — una única app candidata instalada
 * gana; en cualquier otro caso (ninguna o varias instaladas) gana la app
 * del operador nativo del cargador.
 */
class AndroidChargerAppLauncher(
    private val context: Context,
    private val providerDirectory: ProviderDirectory,
) : ChargerAppLauncher {

    override fun resolve(charger: Charger): LaunchResult {
        val candidates = providerDirectory.findAll(charger.allProviderIds)
        if (candidates.isEmpty()) return LaunchResult.NoProviderInfo

        val installed = candidates.filter { isInstalled(it.androidPackage) }

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
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${provider.playStoreId}"))
        if (!safeStartActivity(marketIntent)) {
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
}
