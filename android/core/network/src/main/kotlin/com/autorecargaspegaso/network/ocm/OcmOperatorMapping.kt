package com.autorecargaspegaso.network.ocm

/**
 * OCM identifica al operador de un punto por su propio `OperatorInfo.Title`
 * (texto libre) o `OperatorID` (numérico, interno de OCM) — ninguno de los
 * dos coincide con nuestros `providerId` internos (los slugs usados en
 * `docs/providers/` y en `ProviderDirectory`, p. ej. "ionity", "tesla").
 *
 * Esta tabla traduce de uno a otro. **Verificada en vivo el 2026-09-09**
 * contra `GET /v3/referencedata` con clave real — varios títulos NO
 * coinciden con el nombre comercial obvio (p. ej. "FastNed" con N
 * mayúscula, "Ionity" no en mayúsculas, Tesla sin entrada "Tesla" a secas).
 * Varios operadores tienen varias entradas de OCM por país/marca — se
 * mapean todas al mismo `providerId`.
 */
object OcmOperatorMapping {
    private val titleToProviderId: Map<String, String> = mapOf(
        "Ionity" to "ionity",
        "Tesla (including non-tesla)" to "tesla",
        "Tesla (Tesla-only charging)" to "tesla",
        "FastNed" to "fastned",
        "PlugSurfing" to "plugsurfing",
        "Allego BV" to "allego",
        "EnBW (D)" to "enbw",
        "Iberdrola | BP Pulse (ES)" to "iberdrola",
        "Endesa" to "endesa-x",
        "Enel X" to "endesa-x", // nombre previo al rebranding, algunos puntos antiguos pueden seguir así
        "Wenea" to "wenea",
        "Zunder" to "zunder",
        "Mercadona" to "iberdrola",
        "Repsol - Ibil (ES)" to "waylet",
        "EDP" to "edp",
        "Eranovum (ES)" to "eranovum",

        // Shell Recharge: no existe una entrada genérica "Shell Recharge"
        // en OCM — solo variantes por país/marca. Solo se listan las
        // europeas (excluidas explícitamente las de fuera de Europa: AR,
        // IN, Malaysia, PH, TR, Indonesia, US).
        "Shell Recharge Solutions (BE)" to "shell-recharge",
        "Shell Recharge Solutions (DE)" to "shell-recharge",
        "Shell Recharge Solutions (NL)" to "shell-recharge",
        "Shell Recharge Solutions (UK)" to "shell-recharge",
        "Shell EV Charging Solutions France" to "shell-recharge",
        "Shell Recharge (ES) (Cable Energia)" to "shell-recharge",

        // TotalEnergies: igual que Shell, sin entrada genérica europea.
        "TotalEnergies (ES)" to "total-energies",
        "TotalEnergies (FR)" to "total-energies",
        "Total Energies (UK)" to "total-energies",
        "TOTAL Be PlugToDrive" to "total-energies",
        "TOTAL Nl PlugToDrive" to "total-energies",

        // Chargemap NO aparece en referencedata como operador nativo — confirma
        // que es agregador de roaming puro (CLAUDE.md sección 0/5), no tiene
        // cargadores propios en OCM. No añadir entrada aquí.
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
