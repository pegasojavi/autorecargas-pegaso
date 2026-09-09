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
        // Verificados por uso/documentación directa de la app (confianza alta).
        "IONITY" to "ionity",
        "Tesla" to "tesla",
        "Tesla Supercharger" to "tesla",
        "Fastned" to "fastned",
        "Shell Recharge" to "shell-recharge",

        // ⚠️ NO VERIFICADOS contra la API real de OCM (2026-09-09): la API
        // empezó a exigir API key (`x-api-key`/`key`) para cualquier
        // consulta, incluida `referencedata`, y no se ha podido consultar
        // la lista real de operadores para confirmar el `Title` exacto que
        // usa OCM para cada uno. Son el nombre comercial más probable —
        // `researcher-android` debe confirmarlos (y corregirlos si hace
        // falta) en cuanto se dé de alta una clave (CLAUDE.md sección 0/5).
        "Allego" to "allego",
        "EnBW" to "enbw",
        "Iberdrola" to "iberdrola",
        "Endesa" to "endesa-x",
        "Wenea" to "wenea",
        "Zunder" to "zunder",
        "TotalEnergies" to "total-energies",

        // Chargemap y Plugsurfing NO se añaden aquí a propósito: son
        // agregadores de roaming, no operadores nativos de cargadores
        // propios (CLAUDE.md sección 0, docs/providers/chargemap.md y
        // plugsurfing.md) — no deben aparecer como nativeProviderId de
        // ningún Charger de OCM.
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
