# Zunder

Operador líder de carga ultra-rápida en España, con presencia creciente en
Europa. Legalmente operado por Grupo EasyCharger, S.L. (de ahí el package
name/bundle id, que no contiene "zunder").

## Android (`researcher-android`)

- **Package name:** `es.easycharger.app`
  ([Google Play](https://play.google.com/store/apps/details?id=es.easycharger.app)).
- **App Link (https) confirmado:** ❌ no comprobado. Se comprobó
  `https://www.zunder.com/.well-known/assetlinks.json` → 404.

## iOS (`researcher-ios`)

- **Bundle id:** `es.easycharger.app` (verificado vía iTunes Lookup API,
  `https://itunes.apple.com/lookup?id=1382581406`) — coincide exactamente
  con el package name de Android.
- **App Store id:** `1382581406`
  ([App Store](https://apps.apple.com/es/app/zunder-red-de-carga/id1382581406)),
  desarrollador: Grupo EasyCharger, S.L.
- **Universal Link confirmado:** ❌ no comprobado.
- Nota: Zunder tiene integración con Android Auto y CarPlay
  ([anuncio oficial](https://www.zunder.com/zunder-car-play-zunder-android-auto/)),
  irrelevante para el lanzador pero relevante si en el futuro se investiga
  paridad de funcionalidades.

## QR físico

No se ha encontrado documentación pública del formato de QR de los
cargadores Zunder. **Pendiente de verificación de campo.**

## Roaming

Ya documentado en `docs/providers/roaming-agreements.md`: Zunder es
accesible también desde **Repsol Waylet** (>1.200 puntos operables) y da
acceso, desde su propia app, a la red de **Eranovum** (Zunder es la app de
roaming en ese caso, no el operador nativo). No se ha encontrado ningún
acuerdo adicional en esta pasada.
