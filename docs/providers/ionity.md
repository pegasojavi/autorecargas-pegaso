# IONITY

Red paneuropea de carga ultra-rápida (HPC, hasta 400 kW), operada por IONITY
GmbH. Confirmada para el MVP (CLAUDE.md sección 0).

## Android (`researcher-android`)

- **Package name:** `com.cleevio.ionity.android.app`
  ([Google Play](https://play.google.com/store/apps/details?id=com.cleevio.ionity.android.app))
- **Desarrollador publicado:** IONITY GmbH (app construida por Cleevio, una
  agencia de desarrollo — el nombre de paquete lo refleja).
- **App Link (https) confirmado:** ❌ no confirmado. Se comprobó
  `https://ionity.eu/.well-known/assetlinks.json` → 404. Puede que el
  dominio real usado para Digital Asset Links sea otro (p. ej. un subdominio
  de la propia app) — pendiente de verificar con la
  [Statement List Generator/Tester de Google](https://developers.google.com/digital-asset-links/tools/generator)
  o inspeccionando el APK.
- **Deep link scheme propio:** no encontrado públicamente. Asumir por ahora
  que solo se puede abrir la app (sin deep link a una pantalla concreta),
  usando el package name para `Intent` genérico o `market://details`.

## iOS (`researcher-ios`)

- **Bundle id:** `cz.Ionity.app` (verificado vía iTunes Lookup API,
  `https://itunes.apple.com/lookup?id=1551448692`).
- **App Store id:** `1551448692`
  ([App Store](https://apps.apple.com/us/app/ionity/id1551448692)).
- **Universal Link confirmado:** ❌ no confirmado (mismo motivo que Android;
  no se pudo verificar `apple-app-site-association` sin conocer el dominio
  exacto). Pendiente de `researcher-ios`.
- **Esquema `LSApplicationQueriesSchemes`:** no encontrado públicamente,
  pendiente de verificar.

## QR físico

No se ha encontrado documentación pública del formato de QR de los
cargadores IONITY. Recordar que, por AFIR, todo cargador IONITY (≥50 kW)
debe ofrecer igualmente un medio de pago ad-hoc (CLAUDE.md sección 5), pero
eso no equivale a un enlace universal a la app. **Pendiente de verificación
de campo** (foto/lectura de un QR real en un cargador IONITY).
