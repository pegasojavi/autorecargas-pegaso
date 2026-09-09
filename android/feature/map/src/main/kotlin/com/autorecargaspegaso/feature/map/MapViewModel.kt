package com.autorecargaspegaso.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autorecargaspegaso.domain.Charger
import com.autorecargaspegaso.domain.ConnectorType
import com.autorecargaspegaso.network.ocm.ChargerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    data object Loading : MapUiState
    data class Success(
        val allChargers: List<Charger>,
        val filters: ChargerFilters = ChargerFilters(),
        val selected: Charger? = null,
    ) : MapUiState {
        val visibleChargers: List<Charger> get() = allChargers.filter(filters::matches)
    }
    data class Error(val message: String) : MapUiState
}

private const val DEFAULT_LATITUDE = 40.4168
private const val DEFAULT_LONGITUDE = -3.7038

class MapViewModel(
    private val repository: ChargerRepository,
    private val openProviderApp: (Charger) -> Unit,
    private val getDirections: (Charger) -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** Última posición usada, para no repetir la búsqueda si el GPS manda la misma. */
    private var lastQueriedLocation: Pair<Double, Double>? = null

    init {
        loadNearby(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    }

    /**
     * Llamar desde la UI en cuanto haya una posición real del GPS
     * (CLAUDE.md sección 2/10) — sustituye la búsqueda por defecto en
     * Madrid. `MapScreen` es quien pide el permiso y obtiene la posición;
     * este ViewModel no depende de Android Location Framework.
     */
    fun loadNearby(latitude: Double, longitude: Double) {
        if (lastQueriedLocation == latitude to longitude) return
        lastQueriedLocation = latitude to longitude
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            repository.nearbyChargers(latitude, longitude)
                .onSuccess { chargers -> _uiState.value = MapUiState.Success(chargers) }
                .onFailure { error -> _uiState.value = MapUiState.Error(error.message ?: "Error desconocido") }
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
