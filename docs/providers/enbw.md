# EnBW mobility+

Una de las mayores redes de carga de Alemania, operada por EnBW Energie
Baden-Württemberg AG, con fuerte presencia también en el resto de Europa.

## Android (`researcher-android`)

- **Package name:** `com.enbw.ev`
  ([Google Play](https://play.google.com/store/apps/details?id=com.enbw.ev)).
- **App Link (https):** ❌ **confirmado que NO está configurado.** Se
  comprobó `https://www.enbw.com/.well-known/assetlinks.json` → el fichero
  **existe pero devuelve un array vacío `[]`** — a diferencia de otros
  operadores donde la comprobación fue inconclusa (404/403), aquí hay
  evidencia directa de que no hay ninguna declaración de Digital Asset
  Links para ese dominio. Tratar como "solo esquema propio o ninguno" hasta
  nueva comprobación.
- **Deep link scheme propio:** no encontrado públicamente.

## iOS (`researcher-ios`)

- **Bundle id:** `com.enbw.ev` (verificado vía iTunes Lookup API,
  `https://itunes.apple.com/lookup?id=1232210521&country=de`) — coincide
  exactamente con el package name de Android.
- **App Store id:** `1232210521`
  ([App Store](https://apps.apple.com/de/app/enbw-mobility-e-auto-laden/id1232210521)).
- **Universal Link confirmado:** ❌ no confirmado; dado el resultado vacío
  de `assetlinks.json` en Android, es razonable no esperar Universal Link
  tampoco, pero no se ha comprobado `apple-app-site-association`
  directamente.

## QR físico

No se ha encontrado documentación pública del formato de QR de los
cargadores EnBW. **Pendiente de verificación de campo.**

## Nota de roaming (relevante para `docs/providers/roaming-agreements.md`)

La propia ficha de **Plugsurfing** en el App Store cita explícitamente a
EnBW como una de las redes accesibles desde su app ("Charge with popular
networks like IONITY, Fastned, EnBW, Instavolt, Osprey and many more" —
[App Store, Plugsurfing](https://apps.apple.com/us/app/plugsurfing-ev-charging/id793188906)).
Añadido a `roaming-agreements.md`.
