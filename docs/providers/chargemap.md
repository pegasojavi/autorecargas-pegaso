# Chargemap

**Caso especial — no es un operador nativo de cargadores, es un agregador
de roaming.** Chargemap no opera su propia red física de cargadores; da
acceso (localizar + a veces cargar/pagar) a cargadores de muchos operadores
distintos a través de su propia tarjeta/app. Esto es conceptualmente
distinto al resto de la lista (Ionity, Tesla, Fastned, etc., que sí operan
sus propios cargadores).

**Implicación para `planner` / arquitectura (CLAUDE.md sección 3/5):**
Chargemap encaja de forma más natural como **entrada en
`roamingProviderIds`** de múltiples operadores nativos (vía
`RoamingPartnerships`) que como un "operador nativo" propio con sus
cargadores. Aparece en la lista de 13 redes del MVP como una app a mapear
en `ProviderDirectory` igualmente (para poder abrirla cuando sea la app de
roaming elegida), pero **no debería tener cargadores propios en OCM con
`nativeProviderId: "chargemap"`** salvo que se confirme que Chargemap opera
puntos físicos propios (no detectado en esta investigación).

## Android (`researcher-android`)

- **Package name:** `com.chargemap_beta.android`
  ([Google Play](https://play.google.com/store/apps/details?id=com.chargemap_beta.android)).
  El sufijo `_beta` es parte del nombre de paquete de producción (no indica
  que sea una versión de pruebas) — confirmado por ser el único resultado
  oficial de Chargemap en Play Store.
- **App Link (https):** ❌ no confirmado. Se comprobó
  `https://chargemap.com/.well-known/assetlinks.json` → HTTP 402
  (respuesta inusual/no estándar, no concluyente — repetir comprobación).

## iOS (`researcher-ios`)

- **Bundle id:** `com.chargemap` (verificado vía iTunes Lookup API,
  `https://itunes.apple.com/lookup?id=438176982`) — **no coincide** con el
  package name de Android (`com.chargemap_beta.android`).
- **App Store id:** `438176982`
  ([App Store](https://apps.apple.com/us/app/chargemap-charging-stations/id438176982)).
- **Universal Link confirmado:** ❌ no comprobado.

## QR físico

No aplica del mismo modo que a un operador nativo — Chargemap se abriría
como app de roaming, no identificada directamente por un QR propio en un
cargador físico (salvo que el propio cargador muestre el logo/QR de
Chargemap como una de sus apps compatibles, no confirmado).
