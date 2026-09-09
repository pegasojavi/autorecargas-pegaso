# Fastned

Red de carga rápida con presencia en varios países europeos (NL, DE, UK,
FR, BE...). Confirmada para el MVP (CLAUDE.md sección 0).

## Android (`researcher-android`)

- **Package name:** `nl.fastned.my`
  ([Google Play](https://play.google.com/store/apps/details?id=nl.fastned.my)).
  Ojo: el package name (`nl.fastned.my`) no coincide con el bundle id de iOS
  (`nl.fastned.app`, ver abajo) — no asumir que son iguales entre
  plataformas al construir `ProviderDirectory`.
- **App Link (https) confirmado:** ❌ no confirmado. Se comprobó
  `https://www.fastnedcharging.com/.well-known/assetlinks.json` → 404 — es
  posible que el dominio real sea `fastned.nl` o `fastned.com` en vez de
  `fastnedcharging.com`. Pendiente de repetir la comprobación con el dominio
  correcto.
- **Deep link scheme propio:** no encontrado públicamente, pendiente de
  verificar.

## iOS (`researcher-ios`)

- **Bundle id:** `nl.fastned.app` (verificado vía iTunes Lookup API,
  `https://itunes.apple.com/lookup?id=1485702761`).
- **App Store id:** `1485702761`
  ([App Store](https://apps.apple.com/us/app/fastned-ev-charging-app/id1485702761)).
- **Universal Link confirmado:** ❌ no confirmado, mismo motivo que Android.
- **Nota relevante:** la propia ficha de la app indica que también muestra
  cargadores de otros operadores dentro de su propia app (agregador
  parcial) — no confundir esto con que Fastned dé "acceso" a cargadores de
  terceros vía su app a efectos de `RoamingPartnerships` (sección 5); son
  cosas distintas y hay que verificarlo con cuidado antes de dar por hecho
  un acuerdo de roaming real.

## QR físico

No se ha encontrado documentación pública del formato de QR de los
cargadores Fastned. Fastned ofrece "Autocharge" (carga sin necesidad de QR
ni tarjeta, vía Plug & Charge) en parte de su red, lo cual es un dato
relevante para la UX pero no sustituye la necesidad de identificar el
cargador desde nuestra app. **Pendiente de verificación de campo.**
