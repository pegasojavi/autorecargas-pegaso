package com.autorecargaspegaso.feature.map

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.autorecargaspegaso.domain.ConnectorType
import kotlinx.coroutines.flow.first

private val EXCLUDED_CONNECTOR_TYPES_KEY = stringSetPreferencesKey("excluded_connector_types")

/**
 * Persistencia local del filtro de tipo de conector del mapa (petición del
 * usuario, 2026-09-10): antes vivía solo en memoria de [MapViewModel] y se
 * perdía al cerrar la app, volviendo siempre a "todos los chips activados".
 * Interfaz aparte (en vez de acoplar [MapViewModel] directamente a
 * DataStore) para poder mockearla en tests sin necesitar un `Context` real.
 *
 * Deliberadamente NO persiste `ChargerFilters.minPowerKw` — se deja fuera de
 * este alcance por ser una decisión de producto menor, no una limitación
 * técnica (ver nota del builder en CLAUDE.md).
 */
interface ChargerFiltersRepository {
    suspend fun loadExcludedConnectorTypes(): Set<ConnectorType>
    suspend fun saveExcludedConnectorTypes(types: Set<ConnectorType>)
}

/**
 * Implementación real sobre Preferences DataStore (CLAUDE.md sección 4.1) —
 * primer uso real de DataStore en el proyecto, hasta ahora solo mencionado
 * en el stack tecnológico. Preferences DataStore no tiene un tipo nativo de
 * "conjunto de enum", así que se guarda como `Set<String>` (el `.name` de
 * cada [ConnectorType]) bajo [EXCLUDED_CONNECTOR_TYPES_KEY], y se reconstruye
 * al leer ignorando con seguridad cualquier nombre que ya no exista en el
 * enum (por ejemplo si en el futuro se renombra o se elimina un valor).
 */
class DataStoreChargerFiltersRepository(
    private val dataStore: DataStore<Preferences>,
) : ChargerFiltersRepository {

    override suspend fun loadExcludedConnectorTypes(): Set<ConnectorType> {
        val storedNames = dataStore.data.first()[EXCLUDED_CONNECTOR_TYPES_KEY] ?: return emptySet()
        return storedNames.mapNotNull { name -> runCatching { ConnectorType.valueOf(name) }.getOrNull() }.toSet()
    }

    override suspend fun saveExcludedConnectorTypes(types: Set<ConnectorType>) {
        dataStore.edit { prefs -> prefs[EXCLUDED_CONNECTOR_TYPES_KEY] = types.mapTo(mutableSetOf()) { it.name } }
    }
}
