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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
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
    onOpenSettings: () -> Unit,
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
    var myLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var searchedPlace by remember { mutableStateOf<SearchFocus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showListView by remember { mutableStateOf(false) }

    // Recarga automática al mover el mapa (pan/zoom) — con un pequeño
    // debounce para no disparar una petición por cada fotograma del gesto,
    // solo cuando el usuario deja de mover el mapa.
    var pendingAreaQuery by remember { mutableStateOf<Triple<Double, Double, Double>?>(null) }
    LaunchedEffect(pendingAreaQuery) {
        val query = pendingAreaQuery ?: return@LaunchedEffect
        kotlinx.coroutines.delay(700)
        viewModel.refreshArea(query.first, query.second, query.third)
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            requestCurrentLocation(context) { latitude, longitude ->
                mapCenter = latitude to longitude
                myLocation = latitude to longitude
                pendingAreaQuery = null
                viewModel.loadNearbyDefault(latitude, longitude)
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.searchFocusEvents.collect { focus ->
            mapCenter = focus.latitude to focus.longitude
            searchedPlace = focus
            pendingAreaQuery = null
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.map_title)) },
                actions = {
                    IconButton(onClick = { showListView = !showListView }) {
                        Icon(
                            if (showListView) Icons.Filled.Map else Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.map_toggle_list_action),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenQrScanner) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.map_qr_scan_action))
            }
        },
    ) { padding ->
        when (val current = state) {
            MapUiState.Loading -> LoadingContent(padding)
            is MapUiState.Error -> ErrorContent(current.message, padding)
            is MapUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (current.isRefreshing) {
                        androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { viewModel.search(searchQuery) },
                        searching = current.searching,
                    )
                    current.searchError?.let { error ->
                        Snackbar(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            action = { Text(stringResource(R.string.map_dismiss_action), modifier = Modifier.padding(4.dp)) },
                        ) { Text(error) }
                    }
                    FilterRow(filters = current.filters, onToggleConnector = viewModel::toggleConnectorType, onToggleMinPower = viewModel::toggleMinPower)

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (showListView) {
                            ChargerListByDistance(
                                chargers = current.visibleChargers,
                                referenceLatitude = mapCenter.first,
                                referenceLongitude = mapCenter.second,
                                onChargerClick = viewModel::selectCharger,
                            )
                        } else {
                            OsmMapView(
                                chargers = current.visibleChargers,
                                center = mapCenter,
                                myLocation = myLocation,
                                searchedPlace = searchedPlace,
                                onChargerClick = viewModel::selectCharger,
                                onMapMoved = { latitude, longitude, distanceKm ->
                                    pendingAreaQuery = Triple(latitude, longitude, distanceKm)
                                },
                                // .weight(1f) en el Box contenedor, no fillMaxSize() aquí:
                                // dentro de una Column sin peso, la vista nativa del mapa
                                // reclamaba toda la altura disponible y se dibujaba encima
                                // de los chips de filtro (bug real, detectado en dispositivo).
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searching: Boolean,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.map_search_placeholder)) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (searching) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp))
            } else {
                IconButton(onClick = { /* centrar en mi ubicación de nuevo */ }) {
                    Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_locate_me_action))
                }
            }
        },
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/** Los 9 tipos de conector relevantes en Europa (CLAUDE.md/domain.ConnectorType) — UNKNOWN queda fuera, es un cajón de sastre interno, no una categoría que el usuario elija. */
private val FILTERABLE_CONNECTOR_TYPES = listOf(
    ConnectorType.TYPE_1,
    ConnectorType.TYPE_2,
    ConnectorType.TYPE_3,
    ConnectorType.CCS1,
    ConnectorType.CCS2,
    ConnectorType.CHADEMO,
    ConnectorType.TESLA,
    ConnectorType.DOMESTIC,
    ConnectorType.WIRELESS,
)

@Composable
private fun FilterRow(
    filters: ChargerFilters,
    onToggleConnector: (ConnectorType) -> Unit,
    onToggleMinPower: (Double) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(FILTERABLE_CONNECTOR_TYPES) { type ->
            FilterChip(
                selected = type in filters.connectorTypes,
                onClick = { onToggleConnector(type) },
                label = { Text(connectorTypeLabel(type)) },
            )
        }
        item {
            FilterChip(
                selected = filters.minPowerKw == 50.0,
                onClick = { onToggleMinPower(50.0) },
                label = { Text(stringResource(R.string.map_filter_min_power)) },
            )
        }
    }
}

@Composable
private fun connectorTypeLabel(type: ConnectorType): String = when (type) {
    ConnectorType.TYPE_1 -> stringResource(R.string.map_filter_connector_type1)
    ConnectorType.TYPE_2 -> stringResource(R.string.map_filter_connector_type2)
    ConnectorType.TYPE_3 -> stringResource(R.string.map_filter_connector_type3)
    ConnectorType.CCS1 -> stringResource(R.string.map_filter_connector_ccs1)
    ConnectorType.CCS2 -> stringResource(R.string.map_filter_connector_ccs2)
    ConnectorType.CHADEMO -> stringResource(R.string.map_filter_connector_chademo)
    ConnectorType.TESLA -> stringResource(R.string.map_filter_connector_tesla)
    ConnectorType.DOMESTIC -> stringResource(R.string.map_filter_connector_domestic)
    ConnectorType.WIRELESS -> stringResource(R.string.map_filter_connector_wireless)
    ConnectorType.UNKNOWN -> stringResource(R.string.map_filter_connector_unknown)
}

/** Lista de cargadores ordenada por distancia al centro actual del mapa (a petición del usuario). */
@Composable
private fun ChargerListByDistance(
    chargers: List<Charger>,
    referenceLatitude: Double,
    referenceLongitude: Double,
    onChargerClick: (Charger) -> Unit,
) {
    val sorted = remember(chargers, referenceLatitude, referenceLongitude) {
        chargers
            .map { it to distanceMeters(referenceLatitude, referenceLongitude, it.latitude, it.longitude) }
            .sortedBy { it.second }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sorted, key = { it.first.id }) { (charger, distance) ->
            Card(onClick = { onChargerClick(charger) }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = charger.name, style = MaterialTheme.typography.titleLarge)
                    charger.operatorDisplayName?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    charger.address?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
                    Text(text = formatDistance(distance), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private fun formatDistance(meters: Double): String = if (meters < 1000) {
    "${meters.toInt()} m"
} else {
    "%.1f km".format(meters / 1000.0)
}

/** Fórmula de Haversine — distancia en metros entre dos puntos geográficos. */
private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusMeters = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadiusMeters * c
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
        Text(stringResource(R.string.map_error_loading, message))
    }
}

private const val DEFAULT_ZOOM = 13.0
private const val MY_LOCATION_ZOOM = 17.0

@Composable
private fun OsmMapView(
    chargers: List<Charger>,
    center: Pair<Double, Double>,
    myLocation: Pair<Double, Double>?,
    searchedPlace: SearchFocus?,
    onChargerClick: (Charger) -> Unit,
    onMapMoved: (Double, Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val myLocationLabel = stringResource(R.string.map_my_location_label)
    val myLocationIcon = remember { createDotDrawable(context, fillColor = 0xFF2E7DFF.toInt()) }
    val searchedPlaceIcon = remember { createDotDrawable(context, fillColor = 0xFF22B573.toInt()) }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
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

    // Movimiento de cámara en curso por programación nuestra (no gesto del
    // usuario) — mientras esté activo, se ignoran los scroll/zoom que
    // genera la propia animación, para no disparar recargas automáticas
    // sobre nuestro propio movimiento (bucle real detectado: la animación
    // de "centrar en mi ubicación" generaba eventos de scroll que a su vez
    // programaban un "buscar en esta zona" automático).
    var isProgrammaticCameraMove by remember { mutableStateOf(false) }

    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                if (!isProgrammaticCameraMove) reportMapMoved(mapView, onMapMoved)
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                if (!isProgrammaticCameraMove) reportMapMoved(mapView, onMapMoved)
                return false
            }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    // Zoom de acercamiento (CLAUDE.md sección 2) cuando el centro es una
    // ubicación real (GPS o resultado de búsqueda), no el respaldo fijo de
    // Madrid.
    //
    // `mapView.post { ... }`: si se posiciona la cámara antes de que la
    // vista tenga un tamaño real asignado (layout todavía no ejecutado),
    // osmdroid calcula la proyección con dimensiones 0x0 y el resultado es
    // un zoom incorrecto (se veía media España en vez de la calle) o un
    // parpadeo al recalcular más tarde — bug real detectado en dispositivo.
    // `post` garantiza que el layout ya se ha completado.
    LaunchedEffect(center, myLocation != null, searchedPlace) {
        val zoom = if (myLocation != null || searchedPlace != null) MY_LOCATION_ZOOM else DEFAULT_ZOOM
        isProgrammaticCameraMove = true
        mapView.post {
            mapView.controller.setZoom(zoom)
            mapView.controller.animateTo(GeoPoint(center.first, center.second))
        }
        kotlinx.coroutines.delay(600)
        isProgrammaticCameraMove = false
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.overlays.clear()

            myLocation?.let { (latitude, longitude) ->
                val meMarker = Marker(view).apply {
                    position = GeoPoint(latitude, longitude)
                    title = myLocationLabel
                    icon = myLocationIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                view.overlays.add(meMarker)
            }

            searchedPlace?.let { place ->
                val searchMarker = Marker(view).apply {
                    position = GeoPoint(place.latitude, place.longitude)
                    title = place.label
                    icon = searchedPlaceIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                view.overlays.add(searchMarker)
            }

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

private fun reportMapMoved(mapView: MapView, onMapMoved: (Double, Double, Double) -> Unit) {
    val center = mapView.mapCenter
    val distanceKm = runCatching {
        (mapView.boundingBox.diagonalLengthInMeters / 2.0 / 1000.0).coerceIn(1.0, 100.0)
    }.getOrDefault(25.0)
    onMapMoved(center.latitude, center.longitude, distanceKm)
}

/** Pin circular relleno de [fillColor] con borde blanco, para distinguir "Yo" de los cargadores (pin rojo por defecto de osmdroid) sin necesitar un recurso drawable aparte. */
private fun createDotDrawable(context: Context, fillColor: Int): android.graphics.drawable.Drawable {
    val sizePx = (28 * context.resources.displayMetrics.density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val radius = sizePx / 2f

    val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = android.graphics.Paint.Style.FILL
    }
    val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f * context.resources.displayMetrics.density
    }

    canvas.drawCircle(radius, radius, radius - strokePaint.strokeWidth / 2f, fillPaint)
    canvas.drawCircle(radius, radius, radius - strokePaint.strokeWidth / 2f, strokePaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
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
 * posición conocida si existe (rápido); si no, escucha a **todos** los
 * proveedores disponibles a la vez (no solo GPS) y se queda con el primero
 * que responda — en interior, o en dispositivos sin `NETWORK_PROVIDER`
 * (algunos sin Google Play Services completo), un GPS en solitario puede
 * tardar mucho o no dar señal; con varios proveedores en paralelo se evita
 * depender de que uno concreto funcione.
 */
private fun requestCurrentLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }

    if (providers.isEmpty()) return // sin ningún proveedor activo, se mantiene el centro por defecto

    val lastKnown = providers
        .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull(Location::getTime)

    if (lastKnown != null) {
        onLocation(lastKnown.latitude, lastKnown.longitude)
        return
    }

    var reported = false
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (reported) return
            reported = true
            onLocation(location.latitude, location.longitude)
            locationManager.removeUpdates(this)
        }
    }
    providers.forEach { provider ->
        runCatching {
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }
    }
}
