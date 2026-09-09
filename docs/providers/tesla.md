# Tesla (Supercharger)

Red de carga propia de Tesla, progresivamente abierta a otros fabricantes en
parte de Europa. Confirmada para el MVP (CLAUDE.md sección 0).

## Android (`researcher-android`)

- **Package name:** `com.teslamotors.tesla`
  ([Google Play](https://play.google.com/store/apps/details?id=com.teslamotors.tesla))
- **App Link (https) confirmado:** ❌ no confirmado. Se comprobó
  `https://www.tesla.com/.well-known/assetlinks.json` → HTTP 403 (bloqueado,
  no concluyente — puede existir pero estar protegido contra bots; no
  equivale a "no existe"). Pendiente de verificar con herramienta oficial de
  Google o desde un dispositivo real.
- **Deep link scheme propio:** existe un esquema `tesla://` documentado de
  forma no oficial en listados de terceros (p. ej. el repositorio
  [bhagyas/app-urls](https://github.com/bhagyas/app-urls)), pero sin
  especificación pública de qué rutas/parámetros soporta (p. ej. si acepta
  abrir directamente la pantalla de un Supercharger). **No usar sin
  verificarlo primero contra la app real.**

## iOS (`researcher-ios`)

- **Bundle id:** `com.teslamotors.TeslaApp` (verificado vía iTunes Lookup
  API, `https://itunes.apple.com/lookup?id=582007913`).
- **App Store id:** `582007913`
  ([App Store](https://apps.apple.com/us/app/tesla/id582007913)).
- **Universal Link confirmado:** ❌ no confirmado, mismo motivo que Android.
- Ojo: existen apps de terceros no oficiales para Tesla (T4U, TezLab,
  "Auth app for Tesla") — no confundir con la app oficial al mapear
  `ProviderDirectory`.

## QR físico

No se ha encontrado documentación pública del formato de QR de los
Superchargers Tesla. **Pendiente de verificación de campo.**
