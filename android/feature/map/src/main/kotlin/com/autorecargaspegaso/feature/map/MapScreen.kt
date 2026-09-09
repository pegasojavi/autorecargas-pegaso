package com.autorecargaspegaso.feature.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autorecargaspegaso.domain.Charger
import com.autorecargaspegaso.domain.ConnectorType
import com.autorecargaspegaso.feature.chargerdetail.ChargerDetailContent
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * [viewModel] se inyecta desde la composición manual del `app` module (sin
 * Hilt todavía, ver informe de scaffolding) — este módulo no sabe construir
 * un [MapViewModel] por sí solo porque necesita `ChargerRepository` y el
 * lanzador de apps de proveedor, que son responsabilidad de `app`.
 *
 * Mapa vía osmdroid (OpenStreetMap): sin API key, sin dependencia de Google
 * Play Services — decisión explícita para no bloquear el mapa en
 * dispositivos sin GMS completo (CLAUDE.md sección 4.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onOpenQrScanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { configureOsmdroid(context) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasLocationPermission = granted }

    var mapCenter by remember { mutableStateOf(MADRID_LATITUDE to MADRID_LONGITUDE) }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            requestCurrentLocation(context) { latitude, longitude ->
                mapCenter = latitude to longitude
                viewModel.loadNearby(latitude, longitude)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text("AutoRecargas Pegaso") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenQrScanner) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear cargador")
            }
        },
    ) { padding ->
        when (val current = state) {
            MapUiState.Loading -> LoadingContent(padding)
            is MapUiState.Error -> ErrorContent(current.message, padding)
            is MapUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    FilterRow(filters = current.filters, onToggleConnector = viewModel::toggleConnectorType, onToggleMinPower = viewModel::toggleMinPower)
                    OsmMapView(
                        chargers = current.visibleChargers,
                        center = mapCenter,
                        onChargerClick = viewModel::selectCharger,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                current.selected?.let { selected ->
                    val sheetState = rememberModalBottomSheetState()
                    ModalBottomSheet(onDismissRequest = viewModel::dismissSelection, sheetState = sheetState) {
                        ChargerDetailContent(
                            charger = selected,
                            onOpenApp = { viewModel.onOpenApp(selected) },
                            onGetDirections = { viewModel.onGetDirections(selected) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    filters: ChargerFilters,
    onToggleConnector: (ConnectorType) -> Unit,
    onToggleMinPower: (Double) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = ConnectorType.TYPE_2 in filters.connectorTypes,
                onClick = { onToggleConnector(ConnectorType.TYPE_2) },
                label = { Text("Tipo 2") },
            )
        }
        item {
            FilterChip(
                selected = ConnectorType.CCS in filters.connectorTypes,
                onClick = { onToggleConnector(ConnectorType.CCS) },
                label = { Text("CCS") },
            )
        }
        item {
            FilterChip(
                selected = filters.minPowerKw == 50.0,
                onClick = { onToggleMinPower(50.0) },
                label = { Text("≥ 50 kW") },
            )
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("No se han podido cargar los cargadores: $message")
    }
}

@Composable
private fun OsmMapView(
    chargers: List<Charger>,
    center: Pair<Double, Double>,
    onChargerClick: (Charger) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(center) {
        mapView.controller.setCenter(GeoPoint(center.first, center.second))
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.overlays.clear()
            chargers.forEach { charger ->
                val marker = Marker(view).apply {
                    position = GeoPoint(charger.latitude, charger.longitude)
                    title = charger.name
                    snippet = charger.address
                    setOnMarkerClickListener { _, _ -> onChargerClick(charger); true }
                }
                view.overlays.add(marker)
            }
            view.invalidate()
        },
    )
}

private const val MADRID_LATITUDE = 40.4168
private const val MADRID_LONGITUDE = -3.7038

private fun configureOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
        userAgentValue = context.packageName
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(osmdroidBasePath, "tiles")
    }
}

/**
 * GPS sin Google Play Services (`android.location.LocationManager`, no
 * `FusedLocationProviderClient`) — decisión deliberada para no depender de
 * GMS, igual que la elección de osmdroid para el mapa. Usa la última
 * posición conocida si existe (rápido); si no, pide una actualización
 * puntual y se desregistra tras la primera lectura.
 */
private fun requestCurrentLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }

    val lastKnown = providers
        .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull(Location::getTime)

    if (lastKnown != null) {
        onLocation(lastKnown.latitude, lastKnown.longitude)
        return
    }

    val provider = providers.firstOrNull() ?: return
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocation(location.latitude, location.longitude)
            locationManager.removeUpdates(this)
        }
    }
    runCatching {
        locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
    }
}
