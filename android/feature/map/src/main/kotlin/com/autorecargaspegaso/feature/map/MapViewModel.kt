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

/** Filtros del mapa (CLAUDE.md sección 2, fase 2 adelantada a petición del usuario). */
data class ChargerFilters(
    val connectorTypes: Set<ConnectorType> = emptySet(), // vacío = sin filtrar por conector
    val minPowerKw: Double? = null,
) {
    fun matches(charger: Charger): Boolean {
        val connectorOk = connectorTypes.isEmpty() || charger.connectors.any { it.type in connectorTypes }
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** Eventos puntuales de "mueve la cámara aquí y marca este sitio" — el mapa (Composable) los consume una vez, no son estado persistente. */
    private val _searchFocusEvents = MutableSharedFlow<SearchFocus>(extraBufferCapacity = 1)
    val searchFocusEvents: SharedFlow<SearchFocus> = _searchFocusEvents.asSharedFlow()

    private var lastQueriedLocation: Pair<Double, Double>? = null

    init {
        loadNearbyDefault(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
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
        result.onSuccess { chargers -> _uiState.value = MapUiState.Success(chargers) }
            .onFailure { error ->
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
     * consultado antes — para el botón "Buscar en esta zona" al mover el
     * mapa (a diferencia de [loadNearby], que ignora repeticiones exactas).
     */
    fun refreshArea(latitude: Double, longitude: Double, distanceKm: Double) {
        lastQueriedLocation = null
        loadNearby(latitude, longitude, distanceKm)
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

    fun toggleConnectorType(type: ConnectorType) = updateFilters { filters ->
        filters.copy(
            connectorTypes = if (type in filters.connectorTypes) filters.connectorTypes - type else filters.connectorTypes + type,
        )
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
