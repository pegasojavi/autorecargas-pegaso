# Allego (ex Smoov)

Operador independiente de carga pública con más de 35.000 puntos (AC/DC) en
16 países europeos. **Caso especial:** Allego ha sustituido su app anterior
(Smoov) por una nueva app unificada "Allego". Documentar esto tal cual, sin
asumir que solo hay una app en circulación.

## Estado de la transición Smoov → Allego (verificado)

- La app **Smoov** (Android `nl.smoov`, iOS App Store id `1073557177`,
  desarrollada por Allego B.V.) sigue apareciendo listada en las tiendas,
  pero las fichas oficiales indican que **Allego reemplaza a Smoov** como
  app principal.
  ([Play Store — Smoov](https://play.google.com/store/apps/details?id=nl.smoov),
  [App Store — Smoov](https://apps.apple.com/us/app/smoov/id1073557177),
  [Allego — página de descarga de Smoov](https://www.allego.eu/download-smoov/)).
- La app nueva se llama **"Allego (ex Smoov): EV Charging"**.
- **Existe además una tercera app,** `com.allego.android.app` en Android
  (["Allego" — Google Play](https://play.google.com/store/apps/details?id=com.allego.android.app)),
  cuyo propósito exacto (¿legacy, versión de flotas, versión regional
  distinta?) no se ha podido determinar por búsqueda web. **No usar esta
  hasta confirmar cuál es la vigente para consumidor final.**

## Android (`researcher-android`)

- **Package name (app actual, recomendado):** `eu.allego.app`
  ([Google Play](https://play.google.com/store/apps/details?id=eu.allego.app)).
- **Package name legacy a NO usar sin confirmar:** `com.allego.android.app`
  (propósito sin determinar) y `nl.smoov` (app antigua, en transición).
- **App Link (https) confirmado:** ❌ no confirmado. Se comprobó
  `https://www.allego.eu/.well-known/assetlinks.json` → 404.

## iOS (`researcher-ios`)

- **Bundle id:** `eu.allego.app` (verificado vía iTunes Lookup API,
  `https://itunes.apple.com/lookup?id=6752953747&country=be`) — **coincide
  exactamente con el package name de Android**, caso favorable para
  `ProviderDirectory`.
- **App Store id:** `6752953747`
  ([App Store](https://apps.apple.com/be/app/allego-ex-smoov-ev-charging/id6752953747)).
- **Universal Link confirmado:** ❌ no confirmado (mismo motivo).

## QR físico

No se ha encontrado documentación pública del formato de QR de los
cargadores Allego. **Pendiente de verificación de campo.**
