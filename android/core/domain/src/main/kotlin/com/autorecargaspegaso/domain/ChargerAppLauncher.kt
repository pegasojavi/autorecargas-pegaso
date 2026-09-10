package com.autorecargaspegaso.domain

/**
 * Resultado de resolver qué hacer con un [Charger] (CLAUDE.md secciones 0/3/5,
 * revisión 2026-09-10 de la regla de resolución multi-app).
 *
 * Una única app candidata instalada gana sin preguntar ([OpenedApp]); si
 * ninguna está instalada, gana el operador nativo del cargador sin
 * preguntar ([OpenedApp]/[OpenedStore] según esté o no instalada); **si hay
 * dos o más candidatas instaladas a la vez**, ya no gana el operador nativo
 * en silencio — se devuelve [NeedsDisambiguation] con esas apps instaladas
 * para que el llamador muestre un selector y el usuario elija cuál abrir.
 */
sealed interface LaunchResult {
    data class OpenedApp(val provider: ProviderAppInfo) : LaunchResult
    data class OpenedStore(val provider: ProviderAppInfo) : LaunchResult
    data object NoProviderInfo : LaunchResult

    /**
     * Dos o más apps candidatas instaladas a la vez (CLAUDE.md sección 0,
     * revisión 2026-09-10). [candidates] son solo las instaladas, nunca
     * incluye apps candidatas no instaladas. El llamador debe mostrar un
     * selector y luego invocar `launch(LaunchResult.OpenedApp(elegida))`
     * con la que el usuario elija.
     */
    data class NeedsDisambiguation(val candidates: List<ProviderAppInfo>) : LaunchResult
}

/**
 * Lanzador de apps de proveedor. La implementación real (Android:
 * `feature/applauncher`, usando `PackageManager`) vive fuera de este
 * módulo, que solo define el contrato — así `feature/map` y
 * `feature/chargerdetail` dependen de una abstracción, no de Android
 * Framework directamente.
 */
interface ChargerAppLauncher {
    /**
     * Resuelve qué hacer con este cargador sin preguntar al usuario, salvo
     * cuando hay dos o más apps candidatas instaladas a la vez, en cuyo
     * caso devuelve [LaunchResult.NeedsDisambiguation] y es el llamador
     * quien debe preguntar (CLAUDE.md sección 0, revisión 2026-09-10).
     */
    fun resolve(charger: Charger): LaunchResult

    /** Ejecuta la apertura de la app (o de su ficha en tienda) ya resuelta. */
    fun launch(result: LaunchResult)
}
