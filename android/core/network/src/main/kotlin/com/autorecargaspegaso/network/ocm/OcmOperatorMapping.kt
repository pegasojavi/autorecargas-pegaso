package com.autorecargaspegaso.network.ocm

/**
 * OCM identifica al operador de un punto por su propio `OperatorInfo.Title`
 * (texto libre) o `OperatorID` (numérico, interno de OCM) — ninguno de los
 * dos coincide con nuestros `providerId` internos (los slugs usados en
 * `docs/providers/` y en `ProviderDirectory`, p. ej. "ionity", "tesla").
 *
 * Esta tabla traduce de uno a otro. Solo cubre las redes ya confirmadas
 * para el MVP (CLAUDE.md sección 0) — ampliarla es tarea de
 * `researcher-android` según se activen más redes, verificando el
 * `OperatorInfo.Title` real que devuelve OCM para cada una (puede no
 * coincidir textualmente con el nombre comercial).
 */
object OcmOperatorMapping {
    private val titleToProviderId: Map<String, String> = mapOf(
        "IONITY" to "ionity",
        "Tesla" to "tesla",
        "Tesla Supercharger" to "tesla",
        "Fastned" to "fastned",
    )

    /**
     * Devuelve el `providerId` interno para un operador de OCM, o `null`
     * si no está mapeado — en cuyo caso el cargador no debe asociarse a
     * ninguna app de lanzamiento hasta documentarlo (nunca inventar un
     * slug a partir del título por defecto).
     */
    fun providerIdFor(ocmOperatorTitle: String?): String? =
        ocmOperatorTitle?.let { titleToProviderId[it.trim()] }
}
