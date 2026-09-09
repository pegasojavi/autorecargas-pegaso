# Shell Recharge

**Resuelto (2026-09-09): dentro de Europa es una única app, no varias.**
Decisión del product owner: como el alcance es mercado y cargadores
europeos, solo cuenta la fragmentación de apps *dentro* de Europa — la
existencia de apps separadas para Norteamérica/India/Asia no es relevante
y se descarta sin más análisis.

La antigua app independiente "Shell Recharge" (Android:
`com.thenewmotion.thenewmotion`; iOS: id `617977159` NA / `1574497484`
"Connect by Shell Recharge" UK) **ha sido descontinuada** — Shell consolidó
toda la función de carga EV dentro de su app general. Confirmado también
que la ficha antes atribuida solo a "Alemania" está en realidad publicada
en la store de Reino Unido con la misma cobertura paneuropea, así que no
hay fragmentación real dentro de Europa.

## Android (`researcher-android`)

- **Package name:** `com.shell.sitibv.retail`
  ([Google Play](https://play.google.com/store/apps/details?id=com.shell.sitibv.retail&hl=en_GB)),
  desarrollado por Shell Information Technology International B.V.
- **App Link (https) confirmado:** ❌ no comprobado, pendiente.
- **No usar** `com.shellrecharge.mobileapp` ni `com.thenewmotion.thenewmotion`
  — son apps legacy/discontinuadas.

## iOS (`researcher-ios`)

- **Bundle id:** `com.shell.sitibv.retail` (idéntico al package Android —
  caso poco común, verificado vía
  `https://itunes.apple.com/lookup?id=1464649113&country=gb`).
- **App Store id:** `1464649113` — app **"Shell: Fuel, EV & Rewards"**
  ([App Store, storefront UK](https://apps.apple.com/gb/app/shell-fuel-ev-rewards/id1464649113)).
  Su propia descripción cita acceso a "600.000 puntos de carga públicos en
  33 países europeos" vía Shell Recharge — coherente con una única app
  paneuropea.
- **No usar** `1410234033` ("Shell: Fuel, Charge & More") — es la variante
  de EE. UU., fuera de alcance.
- **Universal Link confirmado:** ❌ no comprobado, pendiente.

## QR físico

No investigado — pendiente de verificación de campo, igual que el resto de
redes (sección 5 del CLAUDE.md).

## Nota para `planner`

Caso cerrado: no hace falta tratar Shell Recharge como una excepción
arquitectónica (no hace falta un `ProviderAppInfo` por país). Una única
entrada en `ProviderDirectory` para ambas plataformas basta para todo el
mercado europeo.
