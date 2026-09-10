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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Outlet
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.osmdroid.views.CustomZoomButtonsController
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
    // solo cuando el usuario deja de mover el mapa. Rectángulo visible real
    // (no un círculo aproximado — ver MapViewModel.refreshVisibleArea).
    var pendingAreaQuery by remember { mutableStateOf<MapBounds?>(null) }
    LaunchedEffect(pendingAreaQuery) {
        val query = pendingAreaQuery ?: return@LaunchedEffect
        kotlinx.coroutines.delay(700)
        viewModel.refreshVisibleArea(query.north, query.south, query.east, query.west)
    }

    // Recentra en la ubicación actual y recarga cargadores cercanos —
    // factorizado para reutilizarlo también desde el botón de "punto de
    // mira" del buscador (bug real reportado: ese botón no hacía nada).
    val fetchCurrentLocation = {
        requestCurrentLocation(context) { latitude, longitude ->
            mapCenter = latitude to longitude
            myLocation = latitude to longitude
            pendingAreaQuery = null
            viewModel.loadNearbyDefault(latitude, longitude)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) fetchCurrentLocation()
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
        // Abajo-izquierda (a petición del usuario): el control de zoom
        // propio de OsmMapView vive abajo-derecha (ver MapZoomControls), así
        // que fijar el FAB en el lado opuesto evita cualquier solape entre
        // ambos sin necesidad de coordinar paddings entre dos composables
        // distintos.
        floatingActionButtonPosition = FabPosition.Start,
        floatingActionButton = {
            // Extended (icono + texto visible), a petición del usuario. OJO:
            // el overload `ExtendedFloatingActionButton(icon=, text=, ...)`
            // de material3 1.4.0 aplica `clearAndSetSemantics` sobre `text`
            // (verificado con javap sobre el .aar real) — el texto visible
            // queda excluido del árbol de accesibilidad a propósito, así que
            // el nombre accesible del botón depende ÚNICAMENTE del
            // `contentDescription` del icono. No dejarlo en null.
            ExtendedFloatingActionButton(
                onClick = onOpenQrScanner,
                icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.map_qr_scan_action)) },
                text = { Text(stringResource(R.string.map_qr_scan_action)) },
            )
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
                        onLocateMe = { if (hasLocationPermission) fetchCurrentLocation() else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
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
                                onMapMoved = { bounds -> pendingAreaQuery = bounds },
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
    onLocateMe: () -> Unit,
    searching: Boolean,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.map_search_placeholder)) },
        singleLine = true,
        leadingIcon = {
            // Bug real reportado: este icono era puramente decorativo (sin
            // onClick), asimétrico con el trailingIcon de "centrar en mi
            // ubicación" que sí funciona. Ahora dispara onSearch() igual
            // que la tecla de acción del teclado (imeAction = Search), sin
            // depender de que el teclado esté abierto.
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.map_search_action))
            }
        },
        trailingIcon = {
            if (searching) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp))
            } else {
                // Bug real reportado: este botón no hacía nada (onClick
                // vacío, solo un comentario). Ahora sí recentra el mapa en
                // la ubicación actual y recarga los cargadores cercanos.
                IconButton(onClick = onLocateMe) {
                    Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_locate_me_action))
                }
            }
        },
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/**
 * Conectores AC ("carga lenta"): monofásicos/trifásicos de baja-media
 * potencia. Agrupados en su propia fila (petición del usuario) para
 * distinguirlos de un vistazo de los conectores DC de la fila de abajo.
 */
private val AC_CONNECTOR_TYPES = listOf(
    ConnectorType.TYPE_1,
    ConnectorType.TYPE_2,
    ConnectorType.TYPE_3,
    ConnectorType.DOMESTIC,
    ConnectorType.WIRELESS,
)

/**
 * Conectores DC ("carga rápida"). TESLA/NACS se trata aquí como DC —aunque
 * el conector físico NACS también soporta AC— porque en esta app se usa en
 * la práctica para carga rápida, que es lo relevante para agrupar con
 * CCS1/CCS2/CHAdeMO de un vistazo.
 */
private val DC_CONNECTOR_TYPES = listOf(
    ConnectorType.CCS1,
    ConnectorType.CCS2,
    ConnectorType.CHADEMO,
    ConnectorType.TESLA,
)

/** Los 9 tipos de conector relevantes en Europa (CLAUDE.md/domain.ConnectorType) — UNKNOWN queda fuera, es un cajón de sastre interno, no una categoría que el usuario elija. */
private val FILTERABLE_CONNECTOR_TYPES = AC_CONNECTOR_TYPES + DC_CONNECTOR_TYPES

/**
 * Dos filas apiladas (AC arriba, DC abajo) en vez de una única fila con
 * scroll horizontal — a petición del usuario, para separar visualmente
 * "carga lenta" de "carga rápida". El chip de potencia (`≥ 50 kW`) se deja
 * al final de la fila DC: la potencia alta es sobre todo relevante para
 * filtrar carga rápida, así que queda junto a esos chips en vez de en una
 * tercera fila aparte. `selected`/`onToggleConnector`/accesibilidad de cada
 * chip no cambian, solo el agrupamiento visual.
 */
@Composable
private fun FilterRow(
    filters: ChargerFilters,
    onToggleConnector: (ConnectorType) -> Unit,
    onToggleMinPower: (Double) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(AC_CONNECTOR_TYPES) { type ->
                ConnectorFilterChip(type = type, filters = filters, onToggleConnector = onToggleConnector)
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(DC_CONNECTOR_TYPES) { type ->
                ConnectorFilterChip(type = type, filters = filters, onToggleConnector = onToggleConnector)
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
}

@Composable
private fun ConnectorFilterChip(
    type: ConnectorType,
    filters: ChargerFilters,
    onToggleConnector: (ConnectorType) -> Unit,
) {
    FilterChip(
        // Activado (en color) = tipo visible; pulsar lo excluye. Ver ChargerFilters.excludedConnectorTypes.
        selected = type !in filters.excludedConnectorTypes,
        onClick = { onToggleConnector(type) },
        label = { Text(connectorTypeLabel(type)) },
        leadingIcon = {
            // Decorativo (sin contentDescription/semántica propia): el
            // texto completo y localizado sigue visible en `label`, que
            // ya es lo que TalkBack anuncia — el icono solo agiliza el
            // escaneo visual del chip (petición del usuario), no
            // sustituye el texto accesible (CLAUDE.md sección 9).
            ConnectorLeadingIcon(type)
        },
    )
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

/**
 * Icono genérico de catálogo (Material Icons Extended 1.7.8) para los tipos
 * que no tienen una silueta física de "conector" que dibujar: enchufe
 * doméstico (toma de pared) e inalámbrico (no hay conector físico). Ver
 * [ConnectorGlyph] para TYPE_1/2/3, CCS1/CCS2, CHAdeMO y TESLA, que sí se
 * dibujan a mano con la disposición de pines real de cada estándar.
 */
private fun connectorTypeIcon(type: ConnectorType): ImageVector = when (type) {
    ConnectorType.DOMESTIC -> Icons.Filled.Outlet
    ConnectorType.WIRELESS -> Icons.Filled.Wifi
    else -> Icons.Filled.Outlet // TYPE_1/2/3, CCS1/2, CHAdeMO, TESLA y UNKNOWN nunca llegan aquí, ver ConnectorLeadingIcon
}

/**
 * Icono del chip de filtro: para los conectores con forma física real
 * (TYPE_1/2/3, CCS1/2, CHAdeMO, TESLA) dibuja un glifo esquemático propio
 * ([ConnectorGlyph]) con la disposición de pines real de cada estándar;
 * para el resto (doméstico/inalámbrico), un icono genérico de catálogo.
 *
 * La disposición de pines se verificó contra una lámina de referencia que
 * el usuario aportó (`esquemas cargadores.jpeg`, "Global EV Charging
 * Standards" — ella misma marcada "© EV Charging Schematics 2024"). Esa
 * lámina NO se ha incrustado ni calcado en la app — solo se usó como
 * referencia técnica para redibujar cada silueta con trazos propios; la
 * disposición de pines de un estándar (IEC 62196/SAE J1772/CHAdeMO/NACS) es
 * un hecho técnico público, no la expresión artística de esa lámina
 * concreta. TESLA usa ahora la silueta real del conector NACS (una cápsula
 * compacta con 5 pines en línea, bien distinta de los conectores circulares
 * del resto) en vez de un icono de coche genérico — sigue sin usarse ningún
 * logo de la marca, solo la forma física y funcional del propio conector.
 */
@Composable
private fun ConnectorLeadingIcon(type: ConnectorType) {
    when (type) {
        ConnectorType.TYPE_1, ConnectorType.TYPE_2, ConnectorType.TYPE_3,
        ConnectorType.CCS1, ConnectorType.CCS2, ConnectorType.CHADEMO,
        ConnectorType.TESLA,
        -> ConnectorGlyph(type)
        else -> Icon(connectorTypeIcon(type), contentDescription = null)
    }
}

@Composable
private fun ConnectorGlyph(type: ConnectorType, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier = modifier.size(18.dp)) {
        when (type) {
            // Type 1 (SAE J1772/Yazaki) y Type 3 (Scame): conectores AC
            // "ovalados" de la misma familia, con 5 pines asimétricos.
            ConnectorType.TYPE_1, ConnectorType.TYPE_3 -> drawType1Glyph(color)
            // Type 2 (Mennekes), estándar AC en Europa: cuerpo circular, 7 pines.
            ConnectorType.TYPE_2 -> drawType2Glyph(color)
            // CCS1/CCS2 (Combo): silueta "orejas de ratón" — nub AC arriba +
            // dos pines DC grandes abajo.
            ConnectorType.CCS1, ConnectorType.CCS2 -> drawCcsGlyph(color)
            // CHAdeMO: cuerpo circular más grande, pestaña de orientación y
            // 2 pines DC grandes + señal.
            ConnectorType.CHADEMO -> drawChademoGlyph(color)
            // Tesla/NACS: cápsula compacta con 5 pines en línea — silueta
            // real del conector, sin logo de marca.
            ConnectorType.TESLA -> drawTeslaGlyph(color)
            else -> Unit // no se alcanza: ConnectorLeadingIcon solo llama aquí para estos tipos
        }
    }
}

/** Conector AC ovalado con 5 pines (familia Type 1/SAE J1772 y Type 3/Scame). */
private fun DrawScope.drawType1Glyph(color: Color) {
    val stroke = size.minDimension * 0.09f
    drawOval(
        color = color,
        topLeft = Offset(size.width * 0.05f, size.height * 0.12f),
        size = Size(size.width * 0.9f, size.height * 0.76f),
        style = Stroke(width = stroke),
    )
    val pinRadius = size.minDimension * 0.075f
    listOf(
        Offset(size.width * 0.33f, size.height * 0.34f),
        Offset(size.width * 0.67f, size.height * 0.34f),
        Offset(size.width * 0.5f, size.height * 0.5f),
        Offset(size.width * 0.36f, size.height * 0.68f),
        Offset(size.width * 0.64f, size.height * 0.68f),
    ).forEach { drawCircle(color = color, radius = pinRadius, center = it) }
}

/** Conector AC circular con 7 pines (Type 2/Mennekes, estándar en Europa). */
private fun DrawScope.drawType2Glyph(color: Color) {
    val stroke = size.minDimension * 0.09f
    val center = Offset(size.width * 0.5f, size.height * 0.5f)
    drawCircle(color = color, radius = size.minDimension * 0.46f, center = center, style = Stroke(width = stroke))
    val pinRadius = size.minDimension * 0.065f
    val ringRadius = size.minDimension * 0.28f
    for (i in 0 until 6) {
        val angle = Math.toRadians((i * 60).toDouble())
        val x = center.x + ringRadius * kotlin.math.cos(angle).toFloat()
        val y = center.y + ringRadius * kotlin.math.sin(angle).toFloat()
        drawCircle(color = color, radius = pinRadius, center = Offset(x, y))
    }
    drawCircle(color = color, radius = pinRadius, center = center)
}

/** Combo CCS1/CCS2: nub AC arriba (contorno) + dos pines DC grandes abajo (rellenos). */
private fun DrawScope.drawCcsGlyph(color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.3f, size.height * 0.06f),
        size = Size(size.width * 0.4f, size.height * 0.34f),
        cornerRadius = CornerRadius(size.width * 0.12f),
        style = Stroke(width = size.minDimension * 0.09f),
    )
    val dcRadius = size.minDimension * 0.22f
    drawCircle(color = color, radius = dcRadius, center = Offset(size.width * 0.3f, size.height * 0.7f))
    drawCircle(color = color, radius = dcRadius, center = Offset(size.width * 0.7f, size.height * 0.7f))
}

/** CHAdeMO: cuerpo circular grande, pestaña de orientación arriba, 2 pines DC + 2 de señal. */
private fun DrawScope.drawChademoGlyph(color: Color) {
    val stroke = size.minDimension * 0.09f
    drawCircle(
        color = color,
        radius = size.minDimension * 0.46f,
        center = Offset(size.width * 0.5f, size.height * 0.54f),
        style = Stroke(width = stroke),
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.42f, 0f),
        size = Size(size.width * 0.16f, size.height * 0.12f),
        cornerRadius = CornerRadius(size.width * 0.03f),
    )
    val bigRadius = size.minDimension * 0.14f
    drawCircle(color = color, radius = bigRadius, center = Offset(size.width * 0.36f, size.height * 0.6f))
    drawCircle(color = color, radius = bigRadius, center = Offset(size.width * 0.64f, size.height * 0.6f))
    val smallRadius = size.minDimension * 0.06f
    drawCircle(color = color, radius = smallRadius, center = Offset(size.width * 0.5f, size.height * 0.38f))
}

/**
 * Tesla/NACS: cápsula compacta (rounded rect ancho) con 5 pines en línea —
 * la silueta real del conector NACS es mucho más pequeña y "achatada" que
 * los conectores circulares CCS/CHAdeMO, así que esa cápsula por sí sola ya
 * lo distingue de un vistazo, sin necesitar ningún logo.
 */
private fun DrawScope.drawTeslaGlyph(color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.08f, size.height * 0.28f),
        size = Size(size.width * 0.84f, size.height * 0.44f),
        cornerRadius = CornerRadius(size.height * 0.22f),
        style = Stroke(width = size.minDimension * 0.09f),
    )
    val pinRadius = size.minDimension * 0.06f
    val y = size.height * 0.5f
    listOf(0.22f, 0.38f, 0.5f, 0.62f, 0.78f).forEach { fractionX ->
        drawCircle(color = color, radius = pinRadius, center = Offset(size.width * fractionX, y))
    }
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
    onMapMoved: (MapBounds) -> Unit,
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
            // Desactiva el control nativo de zoom de osmdroid: por defecto
            // pinta un "+"/"–" propio (fuera del árbol de Compose, con su
            // propio fade-out tras cualquier gesto) en una posición fija que
            // no se coordina con el resto de la UI — se solapaba con el FAB
            // de "Escanear QR" (bug real reportado). Sustituido por
            // MapZoomControls, un control propio en Compose anclado
            // abajo-derecha (ver el Box más abajo).
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
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

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            // El mapa se pintaba por encima de menús/chips Material3 en
            // dispositivo real (bug reportado): los componentes Material3
            // (SearchBar, FilterChip, CenterAlignedTopAppBar, FAB) usan `Surface`
            // internamente, que aplica compositing offscreen para su sombra/
            // elevación tonal; la `View` clásica de osmdroid embebida vía este
            // `AndroidView` no participaba de ese mismo modo de compositing, así
            // que el orden de pintado declarado por Compose dejaba de
            // respetarse. Forzar aquí el mismo modo de compositing resuelve el
            // conflicto sin tocar el resto de la jerarquía.
            modifier = Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
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

        // Control de zoom propio, abajo-derecha (fix combinado del zoom
        // nativo de osmdroid tapando el FAB de QR, ver el
        // `zoomController.setVisibility` de arriba): el FAB de "Escanear QR"
        // vive en `Scaffold.floatingActionButton` con
        // `FabPosition.Start` (abajo-izquierda), así que ambos quedan en
        // esquinas opuestas y no se solapan.
        MapZoomControls(
            onZoomIn = { mapView.controller.zoomIn() },
            onZoomOut = { mapView.controller.zoomOut() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/**
 * Botones propios de zoom in/out, en vez del control nativo de osmdroid
 * (desactivado en [OsmMapView] con `zoomController.setVisibility(NEVER)`).
 * `IMapController.zoomIn()`/`zoomOut()` (osmdroid 6.1.20, expuestos vía
 * `MapView.getController()`/`mapView.controller`) son los métodos reales —
 * los equivalentes de `MapView` (`MapView.zoomIn()`/`zoomOut()`) son de
 * visibilidad de paquete y no serían accesibles desde aquí.
 */
@Composable
private fun MapZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledIconButton(onClick = onZoomIn) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.map_zoom_in_action))
        }
        FilledIconButton(onClick = onZoomOut) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.map_zoom_out_action))
        }
    }
}

/** Rectángulo visible del mapa, en grados — ver [MapViewModel.refreshVisibleArea]. */
data class MapBounds(val north: Double, val south: Double, val east: Double, val west: Double)

/**
 * Reporta el rectángulo visible real del mapa, no un círculo aproximado.
 *
 * Antes se aproximaba el área visible con un círculo (radio = mitad de la
 * diagonal del rectángulo), con un tope que subió de 100 a 500 km en una
 * ronda anterior — pero ese tope era la causa real del bug ("veo un círculo
 * de cargadores, no todo lo que hay en pantalla"): en cuanto el usuario
 * hacía zoom out más allá del tope vigente, la consulta se quedaba fija en
 * un círculo más pequeño que la pantalla, y las esquinas del rectángulo
 * visible (fuera del círculo) se veían vacías aunque hubiera cargadores
 * reales ahí. Subir el tope otra vez solo movería el punto en el que
 * reaparece, no lo arregla. La causa raíz era el círculo en sí, no el
 * tamaño del tope — OCM soporta consultar directamente por rectángulo
 * (`boundingbox`, ver `OpenChargeMapApi`), que por construcción nunca puede
 * producir ese artefacto, a ningún nivel de zoom.
 */
/**
 * Zoom out más allá de esto (diagonal de la pantalla visible) ya no
 * dispara recarga automática — a petición del usuario: a esa escala
 * (varias regiones/un país) buscar y pintar cada punto individual deja de
 * tener sentido (miles de pines, consulta enorme) y no aporta nada sobre
 * lo que ya había cargado. El mapa se sigue pudiendo mover libremente,
 * simplemente no dispara una nueva búsqueda hasta volver a acercarse.
 */
private const val MAX_AUTO_REFRESH_DIAGONAL_KM = 300.0

private fun reportMapMoved(mapView: MapView, onMapMoved: (MapBounds) -> Unit) {
    val box = mapView.boundingBox
    val diagonalKm = runCatching {
        distanceMeters(box.latNorth, box.lonWest, box.latSouth, box.lonEast) / 1000.0
    }.getOrDefault(0.0)
    if (diagonalKm > MAX_AUTO_REFRESH_DIAGONAL_KM) return
    onMapMoved(MapBounds(north = box.latNorth, south = box.latSouth, east = box.lonEast, west = box.lonWest))
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
