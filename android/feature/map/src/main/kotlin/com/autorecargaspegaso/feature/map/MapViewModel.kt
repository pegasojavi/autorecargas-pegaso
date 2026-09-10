package com.autorecargaspegaso.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autorecargaspegaso.domain.Charger
import com.autorecargaspegaso.domain.ConnectorType
import com.autorecargaspegaso.network.geocoding.GeocodingRepository
import com.autorecargaspegaso.network.ocm.ChargerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Filtros del mapa (CLAUDE.md sección 2, fase 2 adelantada a petición del
 * usuario).
 *
 * [excludedConnectorTypes] es un modelo de EXCLUSIÓN, no de inclusión (bug
 * real reportado: "están al revés, si el icono se ve en color está
 * activado... por defecto todos activados"). Antes era al revés (un
 * conjunto de tipos "permitidos", vacío = sin filtrar) y con `FilterChip`
 * usando `selected = type in connectorTypes`, eso significaba que en el
 * estado por defecto (todo visible, sin filtrar) NINGÚN chip aparecía
 * seleccionado/coloreado — justo lo contrario de lo que el usuario espera:
 * si todos los tipos se están mostrando, todos los chips deberían verse
 * "activados" (en color) por defecto, y pulsar uno debería apagarlo
 * (excluirlo), no encenderlo.
 */
data class ChargerFilters(
    val excludedConnectorTypes: Set<ConnectorType> = emptySet(), // vacío = nada excluido, todos los tipos visibles (todos los chips activados por defecto)
    val minPowerKw: Double? = null,
) {
    fun matches(charger: Charger): Boolean {
        val connectorOk = excludedConnectorTypes.isEmpty() || charger.connectors.any { it.type !in excludedConnectorTypes }
        val powerOk = minPowerKw == null || charger.connectors.any { (it.powerKw ?: 0.0) >= minPowerKw }
        return connectorOk && powerOk
    }
}

sealed interface MapUiState {
    /** Solo se da ANTES de tener ningún dato — el mapa aún no se ha montado. */
    data object Loading : MapUiState
    data class Success(
        val allChargers: List<Charger>,
        val filters: ChargerFilters = ChargerFilters(),
        val selected: Charger? = null,
        val searching: Boolean = false,
        val searchError: String? = null,
        /**
         * Recarga en curso (GPS, búsqueda, "buscar en esta zona") con datos
         * ya existentes en pantalla. A propósito NO se vuelve a `Loading`
         * en este caso: hacerlo desmontaba y volvía a montar el `MapView`
         * nativo entero (perdiendo posición/zoom y provocando un parpadeo
         * visible, detectado en dispositivo) cada vez que llegaba una
         * ubicación nueva del GPS tras la carga inicial en Madrid.
         */
        val isRefreshing: Boolean = false,
    ) : MapUiState {
        val visibleChargers: List<Charger> get() = allChargers.filter(filters::matches)
    }
    data class Error(val message: String) : MapUiState
}

/** Resultado de una búsqueda por dirección/ciudad — el mapa mueve la cámara y pinta un marcador ahí. */
data class SearchFocus(val latitude: Double, val longitude: Double, val label: String)

private const val DEFAULT_LATITUDE = 40.4168
private const val DEFAULT_LONGITUDE = -3.7038
private const val DEFAULT_DISTANCE_KM = 25.0

/**
 * Radios de búsqueda para la vista por defecto (CLAUDE.md sección 2, a
 * petición del usuario): empieza en 5 km y va ampliando hasta encontrar al
 * menos un cargador. Solo aplica a la carga inicial (Madrid o primer fix
 * de GPS) — una búsqueda manual por dirección o un "buscar en esta zona"
 * al mover el mapa respeta el radio que pide el usuario, sin ampliarlo.
 */
private val EXPANDING_RADII_KM = listOf(5.0, 10.0, 25.0, 50.0, 100.0, 200.0)

class MapViewModel(
    private val repository: ChargerRepository,
    private val geocodingRepository: GeocodingRepository,
    private val openProviderApp: (Charger) -> Unit,
    private val getDirections: (Charger) -> Unit,
    /**
     * Persistencia local del filtro de conector (CLAUDE.md sección 4.1,
     * petición del usuario 2026-09-10) — interfaz aparte para poder
     * mockearla en tests. `minPowerKw` sigue siendo solo de sesión, no se
     * persiste (ver [ChargerFiltersRepository]).
     */
    private val filtersRepository: ChargerFiltersRepository,
) : ViewModel() {

    // Arranca ya en Success (vacío, isRefreshing = true) en vez de Loading:
    // así el primer frame ya monta buscador/filtros/mapa (aunque vacíos) en
    // vez de un spinner sin chrome que luego se sustituye de golpe por todo
    // el árbol de Compose — ese remontaje completo era el "parpadeo que
    // afecta a todo el formato y ajuste de menús" reportado en dispositivo.
    // MapUiState.Loading queda reservado para un fallo real de red sin
    // ningún dato previo (ver applyChargersResult/beginLoadOrRefresh).
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Success(allChargers = emptyList(), isRefreshing = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** Eventos puntuales de "mueve la cámara aquí y marca este sitio" — el mapa (Composable) los consume una vez, no son estado persistente. */
    private val _searchFocusEvents = MutableSharedFlow<SearchFocus>(extraBufferCapacity = 1)
    val searchFocusEvents: SharedFlow<SearchFocus> = _searchFocusEvents.asSharedFlow()

    private var lastQueriedLocation: Pair<Double, Double>? = null

    /**
     * Condición de carrera real detectada por `researcher-android`: la UI ya
     * es interactiva desde el primer frame (arranca en `Success` vacío), así
     * que el usuario puede pulsar un chip de filtro (`toggleConnectorType`,
     * síncrono sobre `_uiState`) mientras la corrutina de `init` todavía está
     * esperando la lectura de DataStore (sobre todo en arranque en frío, sin
     * caché en memoria todavía). Si esa corrutina aplicara el valor
     * persistido sin comprobar nada, pisaría el toggle reciente del usuario
     * con el valor viejo — "el chip se desactiva solo" justo después de
     * tocarlo. Este flag se marca en cuanto el usuario interactúa, y se
     * comprueba DESPUÉS de que la lectura asíncrona de DataStore complete
     * (nunca antes de lanzar la corrutina) para capturar cualquier toggle
     * ocurrido durante la espera: si ya es `true`, el valor persistido no se
     * aplica — gana la decisión más reciente del usuario.
     */
    private var userHasToggledFilters = false

    init {
        loadNearbyDefault(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        // Filtro de conector persistido entre sesiones (CLAUDE.md sección
        // 4.1): se aplica en cuanto esté disponible, sin bloquear el primer
        // frame (que ya arranca en Success vacío, ver comentario de arriba).
        // Independiente de la carga de cargadores: usa updateFilters, que
        // solo exige que el estado ya sea Success (lo es desde el arranque),
        // así que no importa el orden relativo en que terminen ambas
        // corrutinas.
        viewModelScope.launch {
            val persisted = filtersRepository.loadExcludedConnectorTypes()
            // Comprobación DESPUÉS del suspend de arriba (ver comentario en
            // la declaración del flag): si el usuario ya tocó un chip
            // mientras se leía DataStore, su decisión gana y el valor
            // persistido (más viejo) se descarta en vez de sobrescribirla.
            if (persisted.isNotEmpty() && !userHasToggledFilters) {
                updateFilters { it.copy(excludedConnectorTypes = persisted) }
            }
        }
    }

    /**
     * Vista por defecto (Madrid al arrancar, o primer fix real de GPS): en
     * vez de un radio fijo, empieza en 5 km y va ampliando (10, 25, 50...)
     * hasta encontrar al menos un cargador. Al encontrarlo, repite la
     * consulta con +1 km de margen sobre el radio que dio resultado, para
     * que ese primer cargador no quede justo en el borde del área
     * consultada (a petición del usuario). No usar esto para búsquedas
     * manuales ni para "buscar en esta zona" — ahí el radio lo decide el
     * usuario, no se amplía solo.
     */
    fun loadNearbyDefault(latitude: Double, longitude: Double) {
        if (lastQueriedLocation == latitude to longitude) return
        lastQueriedLocation = latitude to longitude
        viewModelScope.launch {
            beginLoadOrRefresh()
            var result: Result<List<Charger>> = Result.success(emptyList())
            for (radius in EXPANDING_RADII_KM) {
                result = repository.nearbyChargers(latitude, longitude, radius)
                if (result.isFailure) break
                if (result.getOrNull().orEmpty().isNotEmpty()) {
                    // Margen de 1 km extra para que el primer cargador encontrado no quede en el borde.
                    result = repository.nearbyChargers(latitude, longitude, radius + 1.0)
                    break
                }
            }
            applyChargersResult(result)
        }
    }

    /**
     * Llamar con un radio explícito: búsqueda manual por dirección o
     * "buscar en esta zona" al mover el mapa (sección 2) — a diferencia de
     * [loadNearbyDefault], respeta el radio pedido sin ampliarlo solo.
     */
    fun loadNearby(latitude: Double, longitude: Double, distanceKm: Double = DEFAULT_DISTANCE_KM) {
        if (lastQueriedLocation == latitude to longitude) return
        lastQueriedLocation = latitude to longitude
        viewModelScope.launch {
            beginLoadOrRefresh()
            applyChargersResult(repository.nearbyChargers(latitude, longitude, distanceKm))
        }
    }

    private fun beginLoadOrRefresh() {
        val before = _uiState.value
        _uiState.value = if (before is MapUiState.Success) {
            before.copy(isRefreshing = true) // mantiene el mapa montado con los datos anteriores
        } else {
            MapUiState.Loading // primera carga: todavía no hay mapa que preservar
        }
    }

    private fun applyChargersResult(result: Result<List<Charger>>) {
        result.onSuccess { chargers ->
            // Bug real reportado: cada recarga (GPS, búsqueda, "buscar en
            // esta zona" al mover/zoom el mapa, o cualquier refresco
            // disparado por un cambio de tamaño de la ventana) construía un
            // `MapUiState.Success` nuevo SIN pasar los `filters` actuales,
            // así que usaba el valor por defecto (`ChargerFilters()`, sin
            // exclusiones) y el usuario veía sus filtros de conector
            // "desaparecer" en cada recarga. Los filtros son preferencia de
            // sesión del usuario, no datos del área consultada — deben
            // sobrevivir a cualquier recarga, no solo a la primera.
            val previousFilters = (_uiState.value as? MapUiState.Success)?.filters ?: ChargerFilters()
            _uiState.value = MapUiState.Success(chargers, filters = previousFilters)
        }.onFailure { error ->
                val afterFailure = _uiState.value
                _uiState.value = if (afterFailure is MapUiState.Success) {
                    afterFailure.copy(isRefreshing = false, searchError = error.message)
                } else {
                    MapUiState.Error(error.message ?: "Error desconocido")
                }
            }
    }

    /**
     * Recarga forzada centrada en [latitude]/[longitude] aunque ya se haya
     * consultado antes — para el resultado de un buscador de dirección/
     * ciudad (círculo de radio fijo alrededor del lugar encontrado, sección
     * 2). NO usar esto para "el usuario movió/hizo zoom en el mapa": para
     * eso está [refreshVisibleArea], que consulta el rectángulo visible en
     * vez de un círculo.
     */
    fun refreshArea(latitude: Double, longitude: Double, distanceKm: Double) {
        lastQueriedLocation = null
        loadNearby(latitude, longitude, distanceKm)
    }

    /**
     * Recarga forzada al rectángulo visible del mapa — para el pan/zoom del
     * usuario (auto-refresco "buscar en esta zona"). Antes se aproximaba con
     * un círculo (radio = mitad de la diagonal del rectángulo, con un tope
     * de 100 km y luego 500 km) y ese tope era precisamente la causa de un
     * bug real en dispositivo: al hacer zoom out más allá del tope, la
     * consulta se quedaba fija en un círculo más pequeño que la pantalla,
     * así que se veía un corte circular neto en vez de todos los cargadores
     * del área visible. Un rectángulo no puede producir ese artefacto en
     * ningún nivel de zoom, así que ya no hace falta ningún tope de área.
     */
    fun refreshVisibleArea(north: Double, south: Double, east: Double, west: Double) {
        lastQueriedLocation = null
        viewModelScope.launch {
            beginLoadOrRefresh()
            applyChargersResult(repository.chargersInBoundingBox(north, south, east, west))
        }
    }

    /** Buscador por dirección/ciudad (CLAUDE.md sección 2), vía Nominatim/OSM. */
    fun search(query: String) {
        if (query.isBlank()) return
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(searching = true, searchError = null)
        }
        viewModelScope.launch {
            geocodingRepository.search(query)
                .onSuccess { place ->
                    _searchFocusEvents.emit(SearchFocus(place.latitude, place.longitude, place.label))
                    refreshArea(place.latitude, place.longitude, DEFAULT_DISTANCE_KM)
                }
                .onFailure { error ->
                    val stateAfter = _uiState.value
                    if (stateAfter is MapUiState.Success) {
                        _uiState.value = stateAfter.copy(searching = false, searchError = error.message ?: "Búsqueda sin resultados")
                    }
                }
        }
    }

    fun dismissSearchError() {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(searchError = null)
        }
    }

    fun updateFilters(transform: (ChargerFilters) -> ChargerFilters) {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(filters = transform(current.filters))
        }
    }

    /** Pulsar un chip ya activado (en color) lo excluye; pulsar uno excluido lo vuelve a activar. */
    fun toggleConnectorType(type: ConnectorType) {
        userHasToggledFilters = true
        updateFilters { filters ->
            filters.copy(
                excludedConnectorTypes = if (type in filters.excludedConnectorTypes) {
                    filters.excludedConnectorTypes - type
                } else {
                    filters.excludedConnectorTypes + type
                },
            )
        }
        persistExcludedConnectorTypes()
    }

    /**
     * Guarda el filtro de conector actual en DataStore (asíncrono, no
     * bloquea la UI — CLAUDE.md sección 4.1). `updateFilters` ya actualizó
     * `_uiState.value` de forma síncrona antes de llegar aquí, así que este
     * método siempre persiste el valor recién aplicado.
     */
    private fun persistExcludedConnectorTypes() {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            viewModelScope.launch {
                filtersRepository.saveExcludedConnectorTypes(current.filters.excludedConnectorTypes)
            }
        }
    }

    fun toggleMinPower(minPowerKw: Double) = updateFilters { filters ->
        filters.copy(minPowerKw = if (filters.minPowerKw == minPowerKw) null else minPowerKw)
    }

    fun selectCharger(charger: Charger) {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(selected = charger)
        }
    }

    fun dismissSelection() {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(selected = null)
        }
    }

    fun onOpenApp(charger: Charger) = openProviderApp(charger)

    fun onGetDirections(charger: Charger) = getDirections(charger)
}
