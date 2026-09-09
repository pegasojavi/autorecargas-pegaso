package com.autorecargaspegaso

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(MapRoute)
    val appContainer = (LocalContext.current.applicationContext as AutoRecargasPegasoApplication).appContainer

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<MapRoute> {
                val viewModel: MapViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            MapViewModel(
                                repository = appContainer.chargerRepository,
                                openProviderApp = { charger ->
                                    appContainer.chargerAppLauncher.launch(appContainer.chargerAppLauncher.resolve(charger))
                                },
                                getDirections = { charger -> appContainer.chargerAppLauncher.launchDirections(charger) },
                            )
                        }
                    },
                )
                MapScreen(viewModel = viewModel, onOpenQrScanner = { backStack.add(QrScannerRoute) })
            }
            entry<QrScannerRoute> {
                QrScannerScreen(
                    onQrDetected = { rawValue ->
                        // TODO fast-follow (CLAUDE.md sección 5): decodificar rawValue contra
                        // ProviderDirectory / enlaces universales por red. De momento solo
                        // volvemos al mapa tras la primera lectura.
                        backStack.removeLastOrNull()
                    },
                )
            }
        },
    )
}
