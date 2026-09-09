package com.autorecargaspegaso.domain

/**
 * Tabla estática de acuerdos de roaming (CLAUDE.md sección 5,
 * docs/providers/roaming-agreements.md): para un operador nativo dado,
 * qué apps de terceros también dan acceso a sus cargadores.
 *
 * Independiente de [ProviderDirectory] (esa solo mapea app → datos de
 * lanzamiento; esta decide qué apps son candidatas para un cargador dado).
 */
interface RoamingPartnerships {
    fun rolesFor(nativeProviderId: String): List<String>
}
