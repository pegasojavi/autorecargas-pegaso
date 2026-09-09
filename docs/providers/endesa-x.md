# Endesa X

**Nota de nomenclatura importante:** la red se conoce comúnmente como
"Endesa X" (y así aparece en CLAUDE.md y en `roaming-agreements.md`), pero
la app oficial actual **no se llama así**: se llama **"Endesa On Your
Way"** (antes "Enel X Way", renombrada tras el rebranding de Enel X →
Enel On Your Way / Endesa On Your Way). Usar "Endesa On Your Way" como
nombre visible en `ProviderDirectory`, manteniendo "Endesa X"/"endesa" como
identificador interno (`providerId`) por consistencia con el resto de la
documentación del proyecto.

## Android (`researcher-android`)

- **Package name:** `com.enel.mobile.recharge2`
  ([Google Play — Endesa On Your Way](https://play.google.com/store/apps/details?id=com.enel.mobile.recharge2)).
  **Ojo:** termina en "2" — no confundir con otras apps de Endesa (p. ej.
  "Endesa Clientes", `es.awg.movilidadEOL`, que es la app de gestión de
  contratos de luz/gas, sin relación con la carga de VE).
- **App Link (https) confirmado:** ❌ no comprobado.

## iOS (`researcher-ios`)

- **Bundle id:** `com.enel.mobile.recharge` (verificado vía iTunes Lookup
  API, `https://itunes.apple.com/lookup?id=1377291789&country=es`).
  **Sin el "2" final** — a diferencia de Android, donde sí lo lleva. Esto
  es una discrepancia real entre plataformas, no un error de transcripción
  — confirmar de nuevo antes de dar por bueno en `ProviderDirectory`.
- **App Store id:** `1377291789`
  ([App Store — Endesa On Your Way](https://apps.apple.com/gt/app/enel-on-your-way/id1377291789)).
  Desarrollador: ENEL X WAY SRL (grupo Enel, matriz de Endesa en España).
- **Universal Link confirmado:** ❌ no comprobado.

## QR físico

No se ha encontrado documentación pública del formato de QR de los
cargadores Endesa. **Pendiente de verificación de campo.**

## Roaming

Ya documentado en `docs/providers/roaming-agreements.md`: Endesa X es
accesible también desde **Electromaps** (>6.200 puntos incorporados). No se
ha encontrado ningún acuerdo adicional en esta pasada.
