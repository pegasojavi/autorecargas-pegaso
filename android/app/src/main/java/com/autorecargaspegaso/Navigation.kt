package com.autorecargaspegaso

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.autorecargaspegaso.feature.map.MapScreen
import com.autorecargaspegaso.feature.map.MapViewModel
import com.autorecargaspegaso.feature.qrscanner.QrScannerScreen
import kotlinx.serialization.Serializable

@Serializable private data object MapRoute : NavKey
@Serializable private data object QrScannerRoute : NavKey
@Serializable private data object SettingsRoute : NavKey

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(MapRoute)
    val appContainer = (LocalContext.current.applicationContext as AutoRecargasPegasoApplication).appContainer

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<MapRoute> {
                val mapContext = LocalContext.current
                val viewModel: MapViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            MapViewModel(
                                repository = appContainer.chargerRepository,
                                geocodingRepository = appContainer.geocodingRepository,
                                openProviderApp = { charger ->
                                    val result = appContainer.chargerAppLauncher.resolve(charger)
                                    if (result is com.autorecargaspegaso.domain.LaunchResult.NoProviderInfo) {
                                        Toast.makeText(mapContext, mapContext.getString(R.string.provider_app_unknown), Toast.LENGTH_SHORT).show()
                                    } else {
                                        appContainer.chargerAppLauncher.launch(result)
                                    }
                                },
                                getDirections = { charger -> appContainer.chargerAppLauncher.launchDirections(charger) },
                            )
                        }
                    },
                )
                MapScreen(
                    viewModel = viewModel,
                    onOpenQrScanner = { backStack.add(QrScannerRoute) },
                    onOpenSettings = { backStack.add(SettingsRoute) },
                )
            }
            entry<QrScannerRoute> {
                val context = LocalContext.current
                QrScannerScreen(
                    onQrDetected = { rawValue -> handleScannedQr(rawValue, context); backStack.removeLastOrNull() },
                )
            }
            entry<SettingsRoute> {
                SettingsScreen(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}

/**
 * Interpretación del QR físico de un cargador (CLAUDE.md sección 5):
 * ninguna de las 13 redes documentadas tiene confirmado todavía un enlace
 * universal (App Link) propio — hasta que `researcher-android` lo verifique
 * red a red, la única interpretación honesta es: si el contenido es una URL
 * http(s), abrirla con el mecanismo nativo de Android, que ya resuelve solo
 * app-instalada-o-navegador si el operador tiene App Link configurado (o
 * abre el navegador sin más si no). No se inventa un mapeo a
 * `ProviderDirectory` por dominio sin tener esos datos verificados.
 */
private fun handleScannedQr(rawValue: String, context: android.content.Context) {
    val uri = runCatching { Uri.parse(rawValue) }.getOrNull()
    val isHttpUrl = uri?.scheme?.lowercase() in setOf("http", "https")

    val opened = if (isHttpUrl) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    } else {
        false
    }

    if (!opened) {
        Toast.makeText(context, context.getString(R.string.qr_not_recognized), Toast.LENGTH_LONG).show()
    }
}
