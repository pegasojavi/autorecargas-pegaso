package com.autorecargaspegaso.domain

/**
 * Resultado de resolver qué hacer con un [Charger] (CLAUDE.md secciones 0/3/5).
 *
 * Importante: ya NO existe un caso "NeedsDisambiguation". La regla de
 * negocio vigente resuelve sola, sin selector: una única app candidata
 * instalada gana; en cualquier otro caso (ninguna o varias instaladas) gana
 * el operador nativo del cargador.
 */
sealed interface LaunchResult {
    data class OpenedApp(val provider: ProviderAppInfo) : LaunchResult
    data class OpenedStore(val provider: ProviderAppInfo) : LaunchResult
    data object NoProviderInfo : LaunchResult
}

/**
 * Lanzador de apps de proveedor. La implementación real (Android:
 * `feature/applauncher`, usando `PackageManager`) vive fuera de este
 * módulo, que solo define el contrato — así `feature/map` y
 * `feature/chargerdetail` dependen de una abstracción, no de Android
 * Framework directamente.
 */
interface ChargerAppLauncher {
    /** Decide sin preguntar al usuario qué app abrir para este cargador. */
    fun resolve(charger: Charger): LaunchResult

    /** Ejecuta la apertura de la app (o de su ficha en tienda) ya resuelta. */
    fun launch(result: LaunchResult)
}
