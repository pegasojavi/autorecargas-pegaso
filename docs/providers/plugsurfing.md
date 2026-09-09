# Plugsurfing

**Caso especial — igual que Chargemap, es un agregador de roaming, no un
operador nativo de cargadores propios.** Ver la misma nota arquitectónica
en `docs/providers/chargemap.md`: Plugsurfing encaja mejor como entrada en
`roamingProviderIds` de otros operadores (vía `RoamingPartnerships`) que
como "operador nativo" con cargadores propios en OCM.

**Hallazgo relevante para `roaming-agreements.md` (ya incorporado):** la
propia ficha oficial de Plugsurfing en el App Store declara explícitamente:
> "Charge with popular networks like IONITY, Fastned, EnBW, Instavolt,
> Osprey and many more"
([App Store — Plugsurfing](https://apps.apple.com/us/app/plugsurfing-ev-charging/id793188906)).

Esto confirma con fuente oficial (no inferencia por datos de OCM) que
Plugsurfing es candidata de roaming para **dos de las tres redes ya
confirmadas del MVP: Ionity y Fastned**, además de EnBW (una de las 10
pendientes). Instavolt y Osprey no están en la lista actual de 13 redes —
no se han documentado por estar fuera del alcance de esta pasada, pero
`planner` debería valorar si merece la pena añadirlas dado que ya se sabe
que existe la relación de roaming.

## Android (`researcher-android`)

- **Package name:** `com.xitaso.plugsurfing`
  ([Google Play](https://play.google.com/store/apps/details?id=com.xitaso.plugsurfing)).
  "Xitaso" es la agencia de desarrollo — no coincide con el bundle id de
  iOS (ver abajo), otro caso de identificadores distintos entre
  plataformas para la misma app.
- **App Link (https):** ❌ no confirmado. Se comprobó
  `https://plugsurfing.com/.well-known/assetlinks.json` → 404.

## iOS (`researcher-ios`)

- **Bundle id:** `com.PlugSurfing.PlugSurfing` (verificado vía iTunes
  Lookup API, `https://itunes.apple.com/lookup?id=793188906`).
- **App Store id:** `793188906`
  ([App Store](https://apps.apple.com/us/app/plugsurfing-ev-charging/id793188906)),
  desarrollador: Plugsurfing GmbH.
- **Universal Link confirmado:** ❌ no comprobado.

## QR físico

No aplica del mismo modo que a un operador nativo (ver nota de Chargemap).
