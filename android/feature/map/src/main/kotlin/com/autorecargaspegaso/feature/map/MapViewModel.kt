package com.autorecargaspegaso.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autorecargaspegaso.domain.Charger
import com.autorecargaspegaso.network.ocm.ChargerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MapUiState {
    data object Loading : MapUiState
    data class Success(val chargers: List<Charger>, val selected: Charger? = null) : MapUiState
    data class Error(val message: String) : MapUiState
}

/**
 * TODO fast-follow: sustituir la coordenada fija por geolocalización real
 * (`FusedLocationProviderClient` + permiso `ACCESS_FINE_LOCATION`, CLAUDE.md
 * sección 10) y por el buscador de dirección/ciudad (sección 2). Se deja
 * fijo en Madrid para que este primer scaffold sea funcional sin pedir
 * permisos de ubicación todavía.
 */
private const val DEFAULT_LATITUDE = 40.4168
private const val DEFAULT_LONGITUDE = -3.7038

class MapViewModel(
    private val repository: ChargerRepository,
    private val openProviderApp: (Charger) -> Unit,
    private val getDirections: (Charger) -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadNearby(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    }

    fun loadNearby(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            repository.nearbyChargers(latitude, longitude)
                .onSuccess { chargers -> _uiState.value = MapUiState.Success(chargers) }
                .onFailure { error -> _uiState.value = MapUiState.Error(error.message ?: "Error desconocido") }
        }
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
