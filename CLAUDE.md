# CLAUDE.md — AutoRecargas Pegaso (localizador de cargadores)

> Este documento es el contexto persistente del proyecto para Claude Code y para
> los subagentes del pipeline (`handshake`, `researcher-android`,
> `researcher-ios`, `planner`, `builder-android`, `builder-ios`). Debe leerse
> ANTES de planificar o tocar código. Mantenerlo actualizado es
> responsabilidad de `planner`.

---

## ⚠️ Decisiones de alcance

### 🔄 Pivotes de alcance recientes (2026-09-08)
1. A partir de información localizada por el product owner (Javi), el
   proyecto pasa de ser un agregador transaccional (con webservice propio,
   pagos y sesiones de carga) a un **directorio + lanzador**: localiza el
   cargador (por mapa o QR) y abre la app oficial del proveedor para que el
   usuario cargue y pague allí. Desaparecen backend propio, pasarela de
   pago, sesiones de carga gestionadas por la app, cuentas de usuario y el
   recargo de "Gastos de gestión".
2. **El proyecto se hace multiplataforma:** ya no es solo Android — se
   construyen **dos apps nativas independientes, Android e iOS**, con el
   mismo comportamiento funcional (mapa, ficha de cargador, escáner QR,
   lanzador con desambiguación multi-app). El pipeline de agentes vuelve a
   tener dos streams, esta vez **por plataforma** en vez de por capa
   UI/backend como en versiones anteriores de este documento.
3. **El ámbito geográfico se amplía a Europa, no solo España.** El mapa, el
   dataset abierto de cargadores y el `ProviderDirectory` deben cubrir
   operadores de toda Europa (redes multinacionales como Ionity, Fastned,
   Allego, Shell Recharge, Tesla, además de las redes españolas ya
   mencionadas), no solo el mercado español.

### ✅ Confirmadas por el product owner (Javi)
- **Qué es la app:** un directorio + lanzador, no un agregador
  transaccional. Ayuda al usuario a localizar un cargador e identifica el
  proveedor, y abre la app oficial de ese proveedor para que el usuario
  cargue y pague **allí**. La app no interviene en la carga ni en el pago.
- **Plataformas objetivo:** apps nativas independientes para **Android**
  (Kotlin + Jetpack Compose) e **iOS** (Swift + SwiftUI) — dos codebases
  separadas que comparten comportamiento y la misma documentación de
  proveedores (`docs/providers/`), pero no comparten código entre sí.
- **Dos formas de identificar un cargador, misma resolución final:**
  1. Mapa/buscador → el usuario pincha un cargador.
  2. Escáner QR → el usuario escanea el código físico del cargador con la
     cámara desde dentro de la app.
- **Lógica de apertura (lanzador), igual en ambas plataformas — regla
  revisada dos veces, versión vigente 2026-09-10:**
  - Una única app candidata → se abre directamente (deep link / Universal
    Link / App Link, según plataforma); si no está instalada, se lleva al
    usuario a su ficha en la tienda.
  - **Más de una app candidata** (operador nativo del cargador + apps de
    roaming que también dan acceso, p. ej. Waylet/Electromaps sobre
    Endesa/Zunder/Eranovum): tres casos, solo el tercero muestra selector:
    1. Si el usuario tiene **instalada exactamente una** de las apps
       candidatas → se abre esa directamente, sea o no la del operador
       nativo. Sin preguntar.
    2. Si el usuario **no tiene ninguna instalada** → se abre (o se lleva a
       la ficha en la tienda de) la app del **operador nativo** del
       cargador, no una de roaming. Sin preguntar.
    3. **Si el usuario tiene dos o más instaladas a la vez** → **se muestra
       un selector** con esas apps instaladas (solo las instaladas, no
       todas las candidatas) para que el usuario elija cuál abrir. Esto
       revierte, solo para este caso concreto, la decisión anterior de "no
       selector nunca" — los casos 1 y 2 siguen resolviendo sin preguntar.
  - Historial de esta regla, para que quede claro qué se revirtió y qué no:
    versión original (fase de aggregator transaccional) = "siempre
    preguntar"; primera revisión (2026-09-08) = "nunca preguntar, gana la
    instalada única o si no el operador nativo"; revisión actual
    (2026-09-10, la vigente) = igual que la anterior salvo que el caso
    "varias instaladas a la vez" ahora sí pregunta. Ver sección 3 para el
    detalle técnico (`LaunchResult.NeedsDisambiguation`) y sección 5 para
    cómo se compila qué cargadores son multi-app.
- **Sin cuentas de usuario, sin backend propio:** no hace falta login, no
  hay integración con APIs de operadores, no hay servidor propio ni
  pasarela de pago propia.
- **Dataset del mapa: Open Charge Map (OCM).** Consumo gratuito filtrando
  `opendata=true` en la API — licencia ODbL, solo exige atribución visible,
  no bloquea monetizar (compatible con la suscripción de sección 7). **Ojo:**
  no todos los puntos de OCM son de datos abiertos — hay ubicaciones con
  licencia restringida por el propio operador, que hay que excluir o tratar
  aparte si no cumplen `opendata=true`. Mismo dataset para ambas apps.
- **Redes/operadores del MVP:** confirmadas para el lanzamiento **Ionity,
  Tesla y Fastned**; las 10 restantes (Allego/Smoov, EnBW, Shell Recharge,
  TotalEnergies, Iberdrola, Endesa X, Wenea, Zunder, Chargemap, Plugsurfing)
  ya están documentadas en `docs/providers/` (sección 5). **Matiz
  arquitectónico importante:** Chargemap y Plugsurfing **no son operadores
  nativos** de cargadores propios, sino agregadores de roaming — no deben
  tratarse como `nativeProviderId` de ningún `Charger`, sino como entradas
  candidatas en `roamingProviderIds` de cargadores de otros operadores
  (`docs/providers/roaming-agreements.md`). `planner` debe tenerlo en
  cuenta al repartir trabajo entre builders.
- **Ámbito geográfico:** Europa. Esto multiplica el número de operadores a
  soportar en el `ProviderDirectory` de ambas plataformas (redes
  multinacionales y locales por país) y hace más relevante el límite de
  `LSApplicationQueriesSchemes` en iOS (sección 4.2) — cuantos más
  proveedores solo-esquema-propio, antes se llega a ese límite práctico.
- **Idiomas del MVP:** español, francés, inglés y alemán. Un fichero de
  traducción por idioma, y el selector de idioma de la app se genera a
  partir de los ficheros que existan (nunca una lista mantenida a mano) —
  ver sección 6 para la arquitectura completa, incluida la ampliación
  futura a los 24 idiomas oficiales de la UE.
- **Idioma por defecto = el del teléfono**, no uno fijo en el código; el
  selector solo entra en juego cuando el usuario elige explícitamente
  cambiarlo. Fallback a inglés si el idioma del sistema no está soportado
  (asunción, sección 6).
- **Buscador por dirección/ciudad:** el mapa no se limita a "cargadores
  cerca de mí" — el usuario puede buscar una dirección, ciudad o lugar
  (p. ej. para planificar antes de salir) y el mapa se centra ahí. Ver
  sección 2.
- **Modelo de monetización: suscripción de pago, 0,99 €/año, con 3 días de
  prueba gratuita.** Se gestiona íntegramente con la facturación nativa de
  cada tienda (Google Play Billing / StoreKit) — no hace falta backend
  propio para esto tampoco. **Sin Family Sharing / Google Play Family:** la
  suscripción es individual, un usuario = una suscripción, sin compartir
  entre miembros de una familia. Ver sección 7 para el detalle completo.
- **Accesibilidad:** con ámbito europeo, la *European Accessibility Act*
  (exigible desde junio de 2025 para muchos productos digitales) aplica.
  Soporte de lector de pantalla (TalkBack/VoiceOver), texto escalable y
  contraste adecuado son requisito del MVP, no un "nice to have" de fase 2.
  Ver sección 9.
- **Política de privacidad:** el contenido vive **como fichero dentro del
  código de la app** (fuente de verdad versionada junto al proyecto), no
  como página externa mantenida aparte. *Matiz técnico a resolver por
  `planner`:* tanto Play Console como App Store Connect piden igualmente
  una **URL pública** en el formulario de publicación — habrá que publicar
  ese mismo contenido en una página mínima (p. ej. GitHub Pages) que
  refleje el fichero, no reabrir la decisión de dónde vive el contenido en
  sí.
- **Equipo:** equipos **separados de Android e iOS trabajando en paralelo
  desde el día 1** — ya no es un riesgo de secuenciación para el plazo de 2
  meses, aunque `planner` debe seguir validando que la lista de redes
  confirmada (arriba) cabe en ese plazo por plataforma.
- **Plazo:** se mantiene en 2 meses (2026-09-08 → objetivo ≈ 2026-11-08),
  ahora con más certeza al haber equipos paralelos por plataforma desde el
  inicio. `planner` valida el detalle fino de alcance (las 10 redes
  pendientes de investigar) contra este plazo.

### 🔶 Pendientes de cerrar
No quedan pendientes que requieran una decisión del product owner. Lo que
queda es trabajo de investigación mecánico para `researcher-android` /
`researcher-ios` (sección 5): completar package name/bundle id/Universal
Link/App Store id de las 10 redes aún no confirmadas, compilar la tabla de
acuerdos de roaming (qué apps de terceros dan acceso a qué operadores), y
comprobar el formato de QR físico de cada red.

---

## 1. Visión general

**AutoRecargas Pegaso** son dos apps nativas (Android e iOS) que ayudan a un
conductor de vehículo eléctrico a **encontrar** el cargador más cercano y a
**abrir la app correcta** para usarlo — por mapa o escaneando el QR físico
del cargador — sin gestionar ella misma la sesión de carga ni el pago.

El valor diferencial frente a usar el mapa genérico o la app de un único
operador (Iberdrola, Endesa X, Wenea, Zunder, o multinacionales como Ionity,
Fastned, Allego, Shell Recharge, Tesla...) es la **agregación de
descubrimiento**: un solo punto de entrada para localizar cargadores de
cualquier red **en toda Europa**, con un lanzador que sabe qué app abrir —
y que avisa y deja elegir cuando un cargador es accesible desde más de un
operador a la vez, en vez de adivinar por el usuario. Este comportamiento es
idéntico en Android y en iOS.

---

## 2. Alcance funcional

Aplica por igual a la app Android y a la app iOS.

### MVP
- **Buscador por dirección/ciudad:** campo de texto que centra el mapa en
  un lugar buscado (geocodificación), además de la vista por geolocalización
  actual — necesario para planificar antes de salir, no solo para
  "cargadores cerca de mí ahora".
- **Mapa interactivo** con cargadores cercanos (geolocalización, dataset
  abierto): pan/zoom libre, un pin por cargador (agrupado en clusters si hay
  muchos en poca área), pulsar un pin abre su ficha rápida. Datos por pin:
  ubicación, proveedor(es) asociados, y los metadatos que el dataset ofrezca
  (tipo de conector, potencia) — sin estado de disponibilidad en tiempo real
  salvo que el dataset lo incluya. Los filtros (sección "Fase 2" más abajo
  para filtros avanzados) actúan sobre qué pines se muestran en este mismo
  mapa, no sobre una lista aparte.
- **Ficha rápida de cargador** (al pinchar un pin): proveedor(es), dirección,
  y dos acciones independientes:
  1. Abrir la app del proveedor correspondiente (lanzador, ver más abajo).
  2. **"Cómo llegar"** → abre la app de navegación nativa de la plataforma
     (Google Maps en Android, Maps en iOS) con las coordenadas del cargador
     como destino, para guiar al usuario hasta allí. Es independiente del
     lanzador de apps de proveedor: no necesita `ProviderDirectory` ni
     desambiguación propia, delega en el mecanismo estándar del sistema
     operativo (que ya resuelve él solo si hay varias apps de navegación
     instaladas).
- **Lanzador de app de proveedor** (ver sección 0 para la regla completa):
  - Una sola app candidata → la abre directamente; si no está instalada,
    lleva a su ficha en la tienda.
  - Más de una app candidata (operador nativo + apps de roaming): si
    exactamente una está instalada, se abre esa sin preguntar; si ninguna
    está instalada, se usa la app del operador nativo (o su ficha en la
    tienda) sin preguntar; **si hay dos o más instaladas a la vez, se
    muestra un selector** con las apps instaladas para que el usuario
    elija.
- **Escáner QR:** abre la cámara, decodifica el QR físico de un cargador,
  identifica proveedor(es) y aplica la misma lógica de lanzador de arriba.
- **Selector de idioma:** ajuste accesible desde la app que lista los
  idiomas disponibles (español, francés, inglés, alemán en el MVP) y deja
  al usuario fijar el idioma de la interfaz, independientemente del idioma
  del sistema. Ver sección 6.
- **Suscripción y paywall:** 3 días de prueba gratuita y después 0,99 €/año
  si no se cancela (facturación nativa de la tienda). Incluye pantalla de
  paywall (con los términos de precio/duración/cancelación visibles antes
  de aceptar, exigido por ambas tiendas), aviso antes de que termine la
  prueba, y "restaurar compras" (obligatorio en ambas tiendas). Ver
  sección 7.
- Sin login propio (la suscripción se valida contra la cuenta de la tienda,
  no contra una cuenta nuestra), sin pagos de recarga propios (siguen en la
  app del operador), sin seguimiento de sesión de carga, sin historial
  propio de recargas.

### Fase 2 (post-MVP)
- Favoritos guardados localmente en el dispositivo (sin cuenta).
- ~~Filtro por tipo de conector / potencia~~ — adelantado al MVP el
  2026-09-09 (ver sección 4.1), ya implementado.
- Recordar qué apps de proveedor están instaladas para agilizar el selector
  multi-app en visitas repetidas al mismo cargador.
- Caché offline del mapa.

### Fuera de alcance
- Backend propio, pasarela de pago, sesiones de carga gestionadas por la
  app, cuentas de usuario, historial de recargas propio.
- Panel de operador/CPO, gestión de flotas corporativas, firmware/telemetría
  directa del cargador.

---

## 3. Arquitectura

Dos codebases nativas independientes, sin backend propio y sin código
compartido entre ellas — solo comparten comportamiento funcional y la
documentación de proveedores:

```
autorecargas-pegaso/
├── android/    # app nativa Android (Kotlin + Compose) — researcher-android / builder-android
├── ios/        # app nativa iOS (Swift + SwiftUI) — researcher-ios / builder-ios
└── docs/
    └── providers/<red>.md   # una ficha por operador, con los datos de AMBAS plataformas
```

### 3.1 Android (`builder-android` / `researcher-android`)

```
android/app/
├── core/
│   ├── core-network      # cliente HTTP de solo lectura hacia el dataset abierto
│   ├── core-ui           # design system Compose (tema, componentes, tipografía)
│   ├── core-domain       # modelo de Charger y ProviderDirectory (ver abajo)
│   └── core-common       # utilidades, extensiones, Result wrappers
├── feature/
│   ├── map               # búsqueda y mapa de cargadores
│   ├── charger-detail     # ficha rápida + acción de abrir app
│   ├── qr-scanner         # cámara + decodificación de QR
│   └── app-launcher       # abrir app / Play Store / selector multi-app
└── app                    # navigation graph, DI wiring, MainActivity
```

```kotlin
data class ProviderAppInfo(
    val providerId: String,
    val displayName: String,
    val androidPackage: String,
    val deepLinkScheme: String?,   // null si el proveedor solo tiene App Link https
    val playStoreId: String,
)

data class Charger(
    val id: String,
    // ...ubicación, conectores, etc.
    val nativeProviderId: String,        // operador real del punto (dueño del cargador)
    val roamingProviderIds: List<String>, // apps de terceros que también dan acceso (sección 5)
)

sealed interface LaunchResult {
    data class OpenedApp(val provider: ProviderAppInfo) : LaunchResult
    data class OpenedStore(val provider: ProviderAppInfo) : LaunchResult
    // Solo cuando hay 2+ apps candidatas instaladas a la vez (sección 0,
    // revisión 2026-09-10) — `candidates` son solo las instaladas, nunca
    // incluye apps no instaladas. El llamador debe mostrar un selector y
    // luego invocar `launch(provider)` con la elegida.
    data class NeedsDisambiguation(val candidates: List<ProviderAppInfo>) : LaunchResult
}

interface ChargerAppLauncher {
    // Sección 0 (revisión 2026-09-10): una única app instalada gana sin
    // preguntar; si ninguna está instalada, gana el operador nativo sin
    // preguntar; si hay 2+ instaladas a la vez, devuelve
    // NeedsDisambiguation en vez de decidir sola.
    fun resolve(charger: Charger): LaunchResult
    fun launch(provider: ProviderAppInfo)
}
```

### 3.2 iOS (`builder-ios` / `researcher-ios`)

```
ios/AutoRecargasPegaso/
├── Core/
│   ├── Networking        # cliente de solo lectura hacia el dataset abierto
│   ├── DesignSystem       # SwiftUI: colores, tipografía, componentes
│   ├── Domain             # modelo de Charger y ProviderDirectory (ver abajo)
│   └── Common             # utilidades compartidas
├── Features/
│   ├── Map                # búsqueda y mapa de cargadores
│   ├── ChargerDetail       # ficha rápida + acción de abrir app
│   ├── QRScanner           # cámara + decodificación de QR
│   └── AppLauncher         # abrir app / App Store / selector multi-app
└── App                     # App entry point, navegación, inyección de dependencias
```

```swift
struct ProviderAppInfo {
    let providerId: String
    let displayName: String
    let urlScheme: String?          // esquema propio, si el proveedor lo tiene
    let universalLinkHost: String?  // dominio de Universal Link, si lo tiene
    let appStoreId: String
}

struct Charger {
    let id: String
    // ...ubicación, conectores, etc.
    let nativeProviderId: String         // operador real del punto (dueño del cargador)
    let roamingProviderIds: [String]     // apps de terceros que también dan acceso (sección 5)
}

enum LaunchResult {
    case openedApp(ProviderAppInfo)
    case openedStore(ProviderAppInfo)
    // Solo cuando hay 2+ apps candidatas instaladas a la vez (sección 0,
    // revisión 2026-09-10) — `candidates` son solo las instaladas.
    case needsDisambiguation(candidates: [ProviderAppInfo])
}

protocol ChargerAppLauncher {
    // Sección 0 (revisión 2026-09-10): una única app instalada gana sin
    // preguntar; si ninguna está instalada, gana el operador nativo sin
    // preguntar; si hay 2+ instaladas a la vez, devuelve
    // needsDisambiguation en vez de decidir sola.
    func resolve(charger: Charger) -> LaunchResult
    func launch(provider: ProviderAppInfo)
}
```

### Principios comunes a ambas plataformas

- **`ProviderDirectory`** (Android) / su equivalente en iOS es una **tabla
  estática mantenida por el equipo** (JSON o recurso embebido), **no** una
  API remota — cada plataforma mantiene la suya, poblada desde el mismo
  `docs/providers/<red>.md`.
- **Regla de resolución multi-app (idéntica en ambas plataformas, revisión
  2026-09-10):** cuando un cargador tiene más de una app candidata
  (`nativeProviderId` + `roamingProviderIds`), `resolve` decide así: si hay
  exactamente una candidata instalada, se abre esa sin preguntar; si no hay
  ninguna instalada, se usa la del operador nativo sin preguntar; **si hay
  dos o más instaladas a la vez, `resolve` devuelve `NeedsDisambiguation`**
  y el llamador muestra un selector con esas apps instaladas para que el
  usuario elija. Ver sección 5 para cómo se compila la lista de
  `roamingProviderIds` de cada cargador.
- **"Cómo llegar" no es parte de `ChargerAppLauncher`:** es una acción
  aparte y más simple — abrir la app de navegación nativa de la plataforma
  con las coordenadas del cargador — que no depende de `ProviderDirectory`
  ni tiene lógica de desambiguación propia (si el usuario tiene varias apps
  de navegación instaladas, es el sistema operativo quien resuelve, no
  nosotros). No mezclar ambos conceptos: uno abre la app del *operador* del
  cargador, el otro abre *cualquier* app de mapas para llegar hasta él.

---

## 4. Stack tecnológico

### 4.1 Android

- **Lenguaje:** Kotlin. **UI:** Jetpack Compose + Material 3,
  navigation-compose. **DI:** Hilt.
- **Red:** Retrofit/Ktor client de solo lectura contra la API de Open
  Charge Map, siempre filtrando `opendata=true`.
- **Mapas (renderizado interactivo dentro de la app):** **osmdroid**
  (OpenStreetMap) — decisión confirmada 2026-09-09, sustituye a Google Maps
  SDK. Sin API key, sin depender de Google Play Services (mejor
  compatibilidad con dispositivos sin GMS completo). Pines, pan/zoom.
- **Ubicación:** `android.location.LocationManager` (framework, no
  `FusedLocationProviderClient`) — misma razón que el mapa, cero
  dependencia de Play Services en toda la app.
- **QR:** ML Kit Barcode Scanning (o ZXing) + CameraX.
- **Deep linking a apps de proveedor:** `PackageManager` + `Intent
  ACTION_VIEW` (esquema propio o App Link https), fallback a
  `market://details?id=` / `https://play.google.com/store/apps/details?id=`.
- **"Cómo llegar" (navegación externa, distinto de lo anterior):**
  `Intent ACTION_VIEW` con URI `geo:<lat>,<lon>?q=<lat>,<lon>(<nombre>)` —
  el propio Android resuelve el chooser si hay varias apps de navegación
  instaladas (Google Maps, Waze...); no requiere `ProviderDirectory`.
- **Suscripción:** Google Play Billing Library (suscripción auto-renovable
  con periodo de prueba de 3 días configurado en Play Console).
- **Persistencia local:** DataStore (favoritos, preferencias, estado de
  suscripción en caché).
- **Testing:** JUnit5, MockK, Compose UI testing, Espresso.
- **Calidad:** ktlint/detekt, CI con lint + test + build en cada PR.
- **Observabilidad:** Firebase Crashlytics/Analytics (o alternativa
  self-hosted si hay requisito de privacidad, sección 10).

### 4.2 iOS

- **Lenguaje:** Swift. **UI:** SwiftUI. **Arquitectura:** MVVM
  (`ObservableObject` + `@Published`, análogo al `StateFlow<UiState>` de
  Android). **DI:** inyección manual por inicializador — sin framework
  adicional salvo que el equipo prefiera uno.
- **Red:** `URLSession` (o Alamofire si el equipo lo prefiere) de solo
  lectura contra la misma API de Open Charge Map que usa Android, siempre
  filtrando `opendata=true`.
- **Mapas (renderizado interactivo dentro de la app):** MapKit (nativo de
  Apple) por defecto — evita dependencia externa; revisar con
  `researcher-ios` si conviene Google Maps SDK for iOS por paridad visual
  con Android (🔶 no decidido, no bloqueante). Pines, clustering
  (`MKClusterAnnotation`), pan/zoom.
- **QR:** `AVFoundation` (`AVCaptureMetadataOutput`) o `VisionKit`
  (`DataScannerViewController`, iOS 16+).
- **Deep linking a apps de proveedor:** Universal Links (Associated
  Domains) como vía preferente; `UIApplication.canOpenURL` +
  `open(_:options:)` para esquemas propios. **Ojo:** cada esquema
  consultado con `canOpenURL` debe declararse en
  `LSApplicationQueriesSchemes` (Info.plist), con un límite práctico de 50
  entradas — si hay muchos proveedores con solo esquema propio (sin
  Universal Link), puede ser un cuello de botella real; `researcher-ios`
  debe vigilarlo. Fallback a tienda vía
  `https://apps.apple.com/app/id<AppStoreID>`.
- **"Cómo llegar" (navegación externa, distinto de lo anterior):** abrir
  `https://maps.apple.com/?daddr=<lat>,<lon>` (o el esquema `maps://`) para
  lanzar la app Maps nativa con el cargador como destino; no requiere
  `ProviderDirectory` ni declarar nada en `LSApplicationQueriesSchemes`.
- **Suscripción:** StoreKit 2 (suscripción auto-renovable con periodo de
  prueba de 3 días configurado en App Store Connect).
- **Persistencia local:** `UserDefaults` (favoritos, preferencias, estado de
  suscripción en caché).
- **Testing:** XCTest, XCUITest.
- **Calidad:** SwiftLint/SwiftFormat, CI con `xcodebuild` (GitHub Actions o
  Xcode Cloud).
- **Observabilidad:** Firebase Crashlytics/Analytics (o alternativa
  self-hosted si hay requisito de privacidad, sección 10) — misma
  herramienta que Android para tener datos comparables entre plataformas.

**Ya no aplican en ninguna plataforma:** Stripe SDK, WebSocket/SSE de sesión
en vivo, Room/CoreData de historial o sesiones, cualquier stack de backend.

---

## 5. Cargadores, QR y apps de proveedor

### El dataset: Open Charge Map

El mapa consume la API pública de **Open Charge Map (OCM)**, filtrando
siempre `opendata=true`. Licencia ODbL: solo exige atribución (pantalla
"Acerca de/Créditos", sección 10), no restringe el uso comercial ni la
suscripción de pago (sección 7). **Importante:** OCM incluye también puntos
con licencia restringida por el propio operador (`opendata=false` o
licencias específicas) — esos hay que filtrarlos o tratarlos aparte, nunca
mostrarlos como si fueran datos abiertos.

### Dos formas de identificar un cargador, misma resolución final

1. **Desde el mapa:** OCM da la ubicación y el operador del cargador. Al
   pinchar el pin, el lanzador de cada plataforma resuelve qué hacer
   (sección 3).
2. **Desde el QR físico:** no existe un estándar único de QR en la UE. El
   Reglamento AFIR (UE 2023/1804) solo exige que exista **algún** medio de
   pago puntual (ad hoc) en cargadores ≥50 kW desde 2024 (o 2027 según el
   caso) — un QR estático que lleve a una página web de pago ya cumple eso,
   pero no da acceso a la app del operador ni identifica el cargador para
   nosotros. Sirve como red de seguridad conceptual (siempre hay alguna vía
   de pago en esos puntos, aunque nuestra app no reconozca el QR), pero
   **cada red sigue habiendo que decodificarla por separado** —
   `researcher-android`/`researcher-ios` deben comprobar, por red, si el QR
   físico ya codifica una URL con enlace universal oficial (no haría falta
   mapeo propio) o si hace falta decodificar un identificador propio del
   fabricante vía `ProviderDirectory`.

### Cómo se compila qué cargadores son multi-app (roaming)

OCM no expone una relación fiable de "qué apps de terceros dan acceso a
este cargador" — no hay API única para esto. Se compila a mano, cruzando
acuerdos de roaming anunciados públicamente por los operadores (p. ej.
Endesa, Zunder y Eranovum son accesibles también desde apps agregadoras
como Waylet o Electromaps). Esto se mantiene como una tabla estática
aparte, **`RoamingPartnerships`**, independiente de `ProviderDirectory`:

```
operador_nativo → [apps de roaming que también dan acceso a sus cargadores]
p. ej.: "endesa" → ["waylet", "electromaps"]
        "zunder" → ["waylet", "electromaps"]
```

Al cargar un cargador desde OCM con operador nativo `X`, `roamingProviderIds`
se rellena consultando `RoamingPartnerships[X]` — no hace falta (ni es
posible) que el dataset lo indique cargador a cargador.

**Regla de resolución (sección 0/3, revisión 2026-09-10):** una única app
instalada gana sin preguntar; si ninguna está instalada, gana el operador
nativo sin preguntar; si hay dos o más instaladas a la vez, se muestra un
selector con las instaladas. Esta tabla de roaming es la que alimenta esa
decisión — mantenerla actualizada es tan importante como `ProviderDirectory`
en sí.

### Tarea explícita para `researcher-android` / `researcher-ios`

- Completar en `docs/providers/<red>.md` (una sección por plataforma) las
  10 redes aún pendientes (sección 0): Allego/Smoov, EnBW, Shell Recharge,
  TotalEnergies, Iberdrola, Endesa X, Wenea, Zunder, Chargemap, Plugsurfing
  — package name Android / bundle id iOS, tipo de enlace universal o solo
  esquema propio, id en Google Play / App Store, y formato del QR físico.
  Ionity, Tesla y Fastned ya están confirmadas para el MVP (sección 0) y
  deben documentarse primero.
- Mantener `docs/providers/roaming-agreements.md` con la tabla
  `RoamingPartnerships` de arriba, citando la fuente pública de cada
  acuerdo anunciado (no inventar relaciones sin confirmar).

---

## 6. Internacionalización (i18n)

**Idiomas del MVP (confirmado):** español, francés, inglés y alemán.

**Arquitectura de traducciones (confirmada):** un fichero de traducción por
idioma, en una carpeta — sin fichero maestro ni sistema de traducción
propio. Esto coincide con la convención nativa de cada plataforma, no hace
falta inventar nada:
- Android: `res/values-<lang>/strings.xml` (p. ej. `values-es`, `values-fr`,
  `values-en`, `values-de`).
- iOS: `<lang>.lproj/Localizable.strings` (p. ej. `es.lproj`, `fr.lproj`,
  `en.lproj`, `de.lproj`).

**El selector de idioma se genera solo, no se mantiene a mano (confirmado):**
el desplegable de idiomas de la app debe mostrar exactamente **una línea
por fichero de traducción que exista** — nunca una lista escrita a mano en
el código, que podría desincronizarse de los ficheros reales. Cada
plataforma ya expone una API nativa para esto:
- **Android:** ⚠️ **corregido 2026-09-09** — `context.resources.assets.locales`
  (`AssetManager`) NO sirve para esto en la práctica: devuelve todos los
  locales para los que existe CUALQUIER recurso en el APK final, incluidos
  los que traen las propias librerías (AppCompat/Material traducen sus
  textos internos a 60-90 idiomas), no solo los que traducimos nosotros —
  bug real detectado en dispositivo ("aparece el listado completo de
  idiomas del mundo"). La fuente de verdad real es
  `res/xml/locales_config.xml` (declarado también en el manifest vía
  `android:localeConfig`, que es el mecanismo estándar de Android 13+ para
  "idiomas soportados por esta app") — sigue siendo un fichero declarativo
  aparte del código (añadir un idioma = añadir una línea ahí + su
  `values-<lang>/strings.xml`), pero se lee parseando ese XML directamente
  con un `XmlPullParser`, no con una API de una línea: no existe ningún
  método en `androidx.core` (`core` 1.18.0) que devuelva la lista de
  locales declarados en el `locale-config` (`LocaleManagerCompat` solo
  expone `getSystemLocales`/`getApplicationLocales`, no
  `getApplicationSupportedLocales`). Mostrar cada idioma con su autónimo
  (`locale.getDisplayName(locale)` — p. ej. "Español", "Français" — no
  traducido al idioma actual de la UI).
- **iOS:** `Bundle.main.localizations` devuelve exactamente los locales para
  los que hay una carpeta `.lproj` en el bundle (filtrar el pseudo-locale
  `"Base"` si se usa Base internationalization). Mostrar el autónimo de cada
  idioma de forma equivalente.
- **Consecuencia práctica:** añadir un idioma nuevo en el futuro es solo
  añadir el fichero de traducción — no requiere tocar la lógica del
  selector en ninguna plataforma.

**Selección del idioma activo (asimetría entre plataformas — importante
para `researcher-ios`/`builder-ios`):**
- **Android:** usar el mecanismo oficial de idioma por app,
  `AppCompatDelegate.setApplicationLocales(LocaleListCompat...)` (AndroidX
  Core 1.6+), que persiste la elección y recompone la UI sin reiniciar la
  app.
- **iOS no tiene un equivalente igual de directo:** cambiar de idioma en
  caliente sin reiniciar el proceso requiere un `LocalizationManager` propio
  que cargue las cadenas desde el bundle `.lproj` elegido por el usuario (en
  vez de dejar que el sistema operativo elija automáticamente), y forzar el
  refresco de las vistas SwiftUI activas (p. ej. inyectando el locale
  elegido vía `Environment` y forzando recomposición de la vista raíz). `researcher-ios` debe investigarlo antes de que `builder-ios` lo
  implemente — no asumir que es tan simple como en Android.

**Futuro (fase 3, no MVP):** generar un fichero de traducción por cada
idioma oficial de cada país de la Unión Europea (24 idiomas oficiales).
Gracias a la arquitectura de "un fichero = una línea en el selector", esto
es puramente aditivo — añadir ficheros de traducción — y no debería
requerir cambios de código si el selector ya se implementó leyendo los
locales disponibles en vez de una lista fija.

**Idioma por defecto = el del teléfono (confirmado):** al abrir la app por
primera vez, sin que el usuario haya tocado el selector, el idioma debe ser
el que detecte el sistema operativo entre los soportados — nunca un idioma
fijo en el código. En la práctica esto es el comportamiento **por defecto**
de ambas plataformas si no se interfiere:
- **Android:** mientras no se llame nunca a
  `AppCompatDelegate.setApplicationLocales`, el sistema ya resuelve solo el
  mejor idioma soportado según la configuración del dispositivo. Esa
  llamada solo debe hacerse **después** de que el usuario elija
  explícitamente algo en el selector de idioma — nunca al arrancar la app
  ni con un valor por defecto hardcodeado.
- **iOS:** el `LocalizationManager` propio (necesario para el cambio en
  caliente, más arriba) debe arrancar en un modo "seguir al sistema"
  (delegando en `Bundle.main.preferredLocalizations`/`NSLocalizedString`
  normal) y solo pasar a modo "override manual" cuando el usuario elige
  explícitamente un idioma en el selector — nunca inicializarlo con un
  idioma fijo.
- **Idioma de reserva (fallback) cuando el idioma del sistema no está entre
  los soportados:** inglés — es el bucket sin cualificador
  (`res/values/strings.xml` en Android; `Base.lproj`/desarrollo por defecto
  en iOS) que exige cada plataforma como último recurso. Es una asunción de
  trabajo razonable, no una decisión cerrada por el product owner; si se
  prefiere otro idioma de reserva, cambiar solo este párrafo.

---

## 7. Monetización

**Modelo confirmado: suscripción de pago.** 0,99 €/año con 3 días de prueba
gratuita; pasado ese periodo se cobra automáticamente si el usuario no
cancela. Es el único ingreso propio de la app — el precio de la recarga en
sí sigue siendo íntegro para el operador, cobrado por su propia app (sección
2), sin relación con esta suscripción.

**Sin backend propio para esto tampoco:** la suscripción se implementa con
la facturación nativa de cada plataforma:
- **Android:** Google Play Billing Library. El producto de suscripción
  (precio, periodo de prueba de 3 días, renovación anual) se configura en
  Play Console, no en código.
- **iOS:** StoreKit 2. El producto se configura igual en App Store Connect.

**Sin Family Sharing / Google Play Family (confirmado):** la suscripción es
individual y no debe habilitarse como compartible entre miembros de una
familia al configurar el producto en Play Console / App Store Connect.

**Entitlement ligado a la cuenta de la tienda, no a una cuenta propia:**
como la app no tiene login (sección 2), "restaurar compras" usa la cuenta
de Google/Apple del dispositivo, no una cuenta nuestra. Es obligatorio en
ambas tiendas exponer una acción de "Restaurar compra" visible.

**Validación del recibo:** para el MVP basta con la verificación que ya
ofrecen las propias librerías cliente (`Purchase.PurchaseState` de Play
Billing / `Transaction.currentEntitlements` de StoreKit 2) sin servidor
propio. Si en el futuro se detecta fraude o se necesita analítica de
suscripciones más fiable, valorar verificación server-to-server — eso sí
requeriría un pequeño backend, lo cual rompería la premisa de "sin backend
propio" de este documento y debería pasar explícitamente por `planner`.

**Transparencia exigida por las tiendas y por la normativa de consumo de la
UE:** el paywall debe mostrar, antes de que el usuario acepte, precio,
duración de la suscripción, cuándo termina la prueba gratuita, que se
renueva automáticamente, y cómo cancelar — no es opcional, ambas tiendas
rechazan apps que lo omitan.

---

## 8. Pipeline de subagentes (Claude Code)

Dos streams, uno por plataforma — ya no por capa UI/backend:

| Agente | Modelo | Responsabilidad |
|---|---|---|
| `handshake` | — | Recoge/valida el alcance de la tarea y detecta si afecta a Android, iOS, o ambas |
| `researcher-android` | — | Investiga patrones Compose, el dataset abierto de cargadores, apps de proveedor en Android (package/App Link/Play Store) y formato de QR |
| `researcher-ios` | — | Investiga patrones SwiftUI, el mismo dataset abierto, apps de proveedor en iOS (URL scheme/Universal Link/App Store) y formato de QR |
| `planner` | Opus | Diseña el plan técnico único, coordinando ambos streams de plataforma cuando la tarea los afecta a los dos |
| `builder-android` | Sonnet | Implementa la app Android completa (mapa, ficha, "cómo llegar", QR, lanzador, selector de idioma) |
| `builder-ios` | Sonnet | Implementa la app iOS completa (mapa, ficha, "cómo llegar", QR, lanzador, selector de idioma) |

Reglas para los agentes:
- **La sesión principal de Claude Code no programa.** Leer código, compilar,
  ejecutar tests o diagnosticar un fallo (para entender la causa) sí es
  trabajo de la sesión principal, pero cualquier cambio de código de la app
  — un fix de una línea incluido — lo aplica el builder de la plataforma
  correspondiente (`builder-android` / `builder-ios`), nunca la sesión
  principal directamente. Si hace falta iterar rápido varias rondas sobre
  el mismo bug, se invoca al builder varias veces, no se salta el paso.
- **Todo cambio de un builder pasa por una revisión del researcher de su
  misma plataforma antes de darse por bueno** (`builder-android` →
  `researcher-android`; `builder-ios` → `researcher-ios`). El researcher
  revisa el diff contra el plan/bug reportado y contra lo que ya sabe de la
  plataforma (patrones Compose/SwiftUI, comportamiento real de las APIs
  usadas, datos de `docs/providers/`) — el objetivo es pillar fixes que
  compilan pero son incorrectos o frágiles (p. ej. una causa raíz mal
  diagnosticada, una API usada de forma no soportada, una regresión en algo
  que ya funcionaba) antes de compilar el APK final o hacer commit.
  **Si el veredicto de la revisión es OK (con o sin reservas menores), es el
  propio researcher quien compila** (`assembleDebug`/`testDebugUnitTest` en
  Android, el equivalente de CI en iOS) para confirmar el build en verde —
  no se delega esa compilación de vuelta al builder ni la asume la sesión
  principal. Si el veredicto es "rechazar" o "necesita ajuste", vuelve al
  builder correspondiente con el motivo concreto, y no se compila hasta que
  pase una nueva revisión.
- Cada builder toca **solo su codebase** (`android/` o `ios/`); nunca se
  cruzan.
- `researcher-android` y `researcher-ios` documentan el mismo operador en
  el mismo fichero `docs/providers/<red>.md`, cada uno su sección de
  plataforma — `planner` vigila que no queden duplicados ni huecos entre
  las dos.
- La regla de desambiguación multi-app y el contrato conceptual de
  `ChargerAppLauncher` (sección 3) deben implementarse igual en ambas
  plataformas; cualquier cambio pasa por `planner` y se refleja aquí para
  las dos a la vez.
- Cuando una tarea afecta a ambas plataformas sin dependencia dura entre
  ellas, `builder-android` y `builder-ios` pueden ejecutarse en paralelo. Si
  dependen de que un researcher cierre el dato de un proveedor concreto
  primero, `planner` debe indicar el orden.
- Ningún agente añade backend, pasarela de pago, ni gestión de sesión de
  carga propia — queda fuera de alcance (sección 2) salvo que este
  documento cambie explícitamente.
- Las asunciones marcadas con 🔶 en la sección 0 se cierran en la primera
  ejecución de `handshake`/`planner` y se eliminan de este documento una vez
  confirmadas.
- Los agentes `researcher-ui`, `researcher-api`, `builder-ui` y `builder-api`
  de versiones anteriores quedan retirados; no invocarlos.

---

## 9. Convenciones de código

### Android
- Un `ViewModel` por pantalla, estado expuesto como `StateFlow<UiState>`
  inmutable (data class sellada por pantalla, sin lógica en Composables).
- Paquetes: `com.<org>.autorecargaspegaso.<capa>.<feature>`.
- Manejo de errores: `Result<T>`/`sealed interface`, nunca excepciones sin
  capturar cruzando capas.

### iOS
- Una `ViewModel` (`ObservableObject`) por pantalla, estado expuesto vía
  `@Published`, sin lógica de negocio en las `View` de SwiftUI.
- Nombrado de tipos y ficheros según las Swift API Design Guidelines
  (`UpperCamelCase` para tipos, un fichero por tipo público principal).
- Manejo de errores: `Result<T, Error>` / `enum` de dominio con casos
  explícitos, evitar `try!`/`try?` silencioso fuera de capas de UI.

### Comunes
- Sin lógica de negocio en las vistas (Composables o SwiftUI `View`).
- Strings localizados en `strings.xml` (Android) / `Localizable.strings`
  (iOS) — nunca texto embebido en código. Ver sección 6 para la arquitectura
  completa de idiomas (un fichero por idioma, selector autogenerado).
- Commits: Conventional Commits (`feat:`, `fix:`, `refactor:`...).

### Accesibilidad (requisito de MVP, no de fase 2)
Con ámbito europeo, la *European Accessibility Act* aplica — no es opcional.
- **Android:** todo elemento interactivo con `contentDescription` (pines del
  mapa incluidos), tamaños de texto en `sp` (no `dp`) para respetar el
  escalado del sistema, contraste AA mínimo en el design system de
  `core-ui`, navegación completa con TalkBack en las pantallas del MVP.
- **iOS:** `accessibilityLabel`/`accessibilityHint` en controles e
  interacciones del mapa, soporte de Dynamic Type en todo el texto,
  contraste AA mínimo, navegación completa con VoiceOver.
- El escáner QR necesita una alternativa accesible para quien no pueda usar
  la cámara con precisión (p. ej. confirmación por voz de que el QR se ha
  leído, o permitir introducir el identificador manualmente si el dataset
  lo soporta) — a validar con `researcher-android`/`researcher-ios`.

---

## 10. Seguridad y privacidad

- **Cámara:** el permiso solo se pide al abrir el escáner QR, con
  explicación de para qué se usa (Android: permiso en runtime; iOS:
  `NSCameraUsageDescription` en Info.plist). No se graban ni almacenan
  imágenes ni vídeo — la decodificación es en vivo y se descarta al momento.
- **Ubicación:** solo en primer plano y solo para centrar el mapa (Android:
  `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` en runtime; iOS:
  `NSLocationWhenInUseUsageDescription`), sin seguimiento en segundo plano
  ni historial de ubicaciones del usuario.
- **RGPD:** al no haber cuentas ni historial propio, el tratamiento de
  datos personales queda reducido a ubicación puntual durante el uso —
  minimizar cualquier persistencia que no sea estrictamente necesaria
  (favoritos locales, preferencias).
- **`ProviderDirectory` no contiene secretos:** son datos públicos de cada
  operador (package name/bundle id, ids de tienda, dominios de enlace
  universal) — no requiere gestor de secretos ni cifrado especial, en
  ninguna plataforma.
- No se procesan ni almacenan datos de pago ni de sesión de carga — quedan
  íntegramente en la app del operador correspondiente.
- **Cumplimiento de tiendas (bloqueante para publicar):** por pedir cámara
  y ubicación, ambas tiendas exigen declarar su uso antes de publicar —
  Google Play (formulario de Data Safety) y App Store (etiqueta de App
  Privacy). **Política de privacidad (confirmado):** el contenido vive como
  fichero versionado dentro del código de la app, no como página externa
  mantenida aparte — pero ambos formularios de tienda piden igualmente una
  URL pública, así que hay que publicar ese mismo contenido en una página
  mínima (p. ej. GitHub Pages) solo para satisfacer ese campo, sin que eso
  cambie dónde vive el contenido real.
- **Atribución de Open Charge Map (confirmado, sección 5):** la licencia
  ODbL exige atribución visible — mostrarla en una pantalla de "Acerca
  de/Créditos" accesible desde ambas apps, con el texto de atribución que
  exige OCM en su documentación de licencia.
- **Suscripción (sección 7):** el estado de suscripción cacheado localmente
  no sustituye la fuente de verdad de la tienda (Play Billing / StoreKit) —
  no persistir "está suscrito" de forma que sobreviva a una cancelación sin
  volver a comprobarlo. La política de privacidad debe cubrir también qué
  datos ve la tienda (Google/Apple), no solo los que gestiona la app.

---

## 11. Comandos de build y verificación

### Android — verificable en local (confirmado, 2026-09-09)
Toolchain instalada en la máquina de desarrollo vía la CLI oficial de
Google (`https://dl.google.com/android/cli/latest/<plataforma>/install.cmd`
— dominio `dl.google.com`, script inspeccionado antes de ejecutar):
JDK 17, SDK en `%LOCALAPPDATA%\Android\Sdk` (platform-tools, platforms;35,
build-tools;35.0.0). `ANDROID_HOME`/`ANDROID_SDK_ROOT` configuradas.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest   # requiere emulador/dispositivo, no siempre disponible en CI local
./gradlew ktlintCheck detekt
```

### iOS — **no verificable en local, nunca** (confirmado, no es cuestión de instalar nada)
Xcode y el SDK de iOS solo existen para macOS; no hay VM/Docker/emulación
legítima sobre hardware no-Apple (viola el EULA de macOS, descartado
explícitamente). Verificación real vía **GitHub Actions con runner
`macos-latest`** (hardware Apple real, gratuito en repos públicos) en
cuanto exista repo remoto y código que compilar:

```yaml
# .github/workflows/ios.yml (a crear cuando haya código iOS)
runs-on: macos-latest
steps:
  - uses: actions/checkout@v4
  - run: brew install xcodegen   # genera el .xcodeproj real desde project.yml
  - run: xcodegen generate
  - run: xcodebuild -scheme AutoRecargasPegaso -destination 'platform=iOS Simulator,name=iPhone 16' test
  - run: swiftlint
```

**Por qué XcodeGen y no un `.xcodeproj` escrito a mano:** el formato
`.pbxproj` es frágil y pensado para generarse desde Xcode, no para
escribirse a mano sin poder abrirlo/compilarlo. XcodeGen genera el
`.xcodeproj` real y válido a partir de un `project.yml` en texto plano
(sí versionable, sí revisable en PR) — el CI de macOS es quien valida que
compila de verdad, cerrando el hueco de no poder hacerlo en local.

---

## 12. Próximos pasos inmediatos

**Estado (2026-09-09): scaffold Android real, verificado con `assembleDebug`
y `testDebugUnitTest` en verde** — 9 módulos Gradle (`app`,
`core:{common,domain,network,ui}`, `feature:{map,chargerdetail,qrscanner,
applauncher}`), integración real con Open Charge Map, `ChargerAppLauncher`
con la regla de resolución sin selector cubierta por tests unitarios reales,
`ProviderDirectory`/`RoamingPartnerships` poblados desde `docs/providers/`.
Repo en `https://github.com/pegasojavi/autorecargas-pegaso`.

**⚠️ Hallazgos de integración real con OCM (2026-09-09), corrigen asunciones
de la sección 0/5 — todos ya arreglados en el código:**
- **API key obligatoria:** OCM exige `key`/`x-api-key` en todas sus
  consultas, incluida `/v3/poi` — ya no "funciona sin clave para volumen
  bajo". Clave gratuita dada de alta (app "autorecargas-pegaso" en
  openchargemap.org), guardada en `android/local.properties` (no
  versionado) y expuesta vía `BuildConfig.OCM_API_KEY`.
- **`compact=true` rompía el mapeo de todos los cargadores en silencio:**
  con ese valor (el que se había puesto por defecto) OCM devuelve
  `OperatorInfo: null`, y `ChargerMapper` depende de `OperatorInfo.Title`
  para saber de qué red es cada punto. Corregido a `compact=false`.
- **Varios títulos de operador reales NO coinciden con el nombre
  comercial obvio** — verificado contra `GET /v3/referencedata` con clave
  real: `"Ionity"` (no "IONITY"), `"FastNed"` (N mayúscula), Tesla no tiene
  entrada "Tesla" a secas (son `"Tesla (including non-tesla)"` /
  `"Tesla (Tesla-only charging)"`), y **"Shell Recharge" y "TotalEnergies"
  no existen como entrada genérica europea** — solo variantes por país
  (`Shell Recharge Solutions (BE/DE/NL/UK)`, `TotalEnergies (ES/FR)`, etc.).
  `OcmOperatorMapping` ya está corregido con los títulos reales — esto
  confirma por qué la sección 5 insiste en no asumir nombres sin
  comprobarlos contra la fuente real.
- **Matiz de licencia:** cada registro trae su propio `DataProvider.License`
  (ODbL en unos, "Creative Commons Attribution 4.0" en otros) — no es
  uniformemente ODbL como se simplificó al elegir el dataset. Todos exigen
  atribución igualmente, así que no cambia la conclusión de la sección 5,
  pero el texto de atribución (pantalla "Acerca de/Créditos") debería
  contemplarlo si se quiere ser estrictamente precisos por registro.

**Fast-follows explícitamente diferidos en este scaffold (no son pendientes
de decisión, son trabajo pendiente de `builder-android`):**
- Hilt: se usa composición manual (`AppContainer`) porque no se pudo fijar
  con confianza una versión de KSP compatible con AGP 9.0.1/Kotlin 2.3.20
  sin poder probarlo — migrar cuando se confirme la combinación correcta.
- ✅ **Resuelto (2026-09-09): mapa interactivo real + GPS + filtros.**
  Cambio de decisión respecto al stack (sección 4.1): en vez de Google Maps
  SDK (necesitaba API key) se usa **osmdroid (OpenStreetMap)** — sin clave,
  sin cuenta, y sin depender de Google Play Services, lo cual además da
  mejor compatibilidad con dispositivos sin GMS completo (detectado en
  pruebas reales: un móvil con tienda de apps propia del fabricante en vez
  de Google Play). La ubicación usa `android.location.LocationManager` del
  framework en vez de `FusedLocationProviderClient`, por la misma razón —
  ninguna dependencia de Play Services en toda la app. Filtros de conector
  (Tipo 2/CCS) y potencia (≥50 kW) implementados como chips sobre el mapa,
  adelantando ese punto de la fase 2 a petición del usuario.
- ✅ **Resuelto (2026-09-09): selector de idioma, icono, i18n e
  interpretación de QR.**
  - Pantalla de Ajustes con selector de idioma real, generado desde
    `resources.assets.locales` (CLAUDE.md sección 6) — nunca una lista
    hardcodeada — usando `AppCompatDelegate.setApplicationLocales`.
  - Strings ES/EN externalizados a `strings.xml` en los 4 módulos con UI
    (`app`, `feature:map`, `feature:chargerdetail`, `feature:qrscanner`).
  - Icono de app real (negro + rayo bronce, tema "Eco").
  - QR: si el contenido escaneado es una URL http(s), se abre con el
    mecanismo nativo de Android (resuelve app-instalada-o-navegador solo);
    si no, aviso de "código no reconocido". Sigue sin interpretar contra
    `ProviderDirectory` por dominio — ninguna red tiene todavía un enlace
    universal confirmado (sección 5).
- ✅ **Resuelto (2026-09-09): cargadores ocultos por operador sin mapear.**
  `ChargerMapper` descartaba en silencio cualquier cargador cuyo operador
  de OCM no estuviera en `OcmOperatorMapping` — en la práctica, la mayoría
  de cargadores reales de una zona desaparecían sin ningún error visible
  (detectado en pruebas reales, A Coruña). Ahora se muestran todos, con el
  nombre real del operador (`Charger.operatorDisplayName`) aunque el
  lanzador todavía no sepa abrir su app (aviso al usuario en ese caso, en
  vez de fallar en silencio).
- ✅ **Resuelto (2026-09-09): consistencia y radio de búsqueda del mapa.**
  El límite fijo de 100 resultados de OCM truncaba de forma no determinista
  zonas con muchos cargadores (mismos parámetros, resultados distintos
  cada vez) — ahora escala con el radio consultado. La vista por defecto ya
  no usa un radio fijo: empieza en 5 km y amplía (10/25/50/100/200) hasta
  encontrar el primer cargador, con +1 km de margen para que no quede en el
  borde del mapa.
- ✅ **Resuelto (2026-09-09): vista de lista por distancia.** Alternable con
  el mapa desde la barra superior, ordenada por distancia real (Haversine)
  al centro actual del mapa.
- ✅ **Resuelto (2026-09-09): buscador con recarga automática.** El
  buscador de dirección/ciudad pinta un marcador en el lugar encontrado; al
  mover o hacer zoom en el mapa, el área se recarga sola (debounce de
  700 ms) — ya no hace falta pulsar un botón "buscar en esta zona".
- ✅ **Resuelto (2026-09-09): segunda ronda de bugs reportados en
  dispositivo real, todos corregidos (build verde con `assembleDebug` +
  `testDebugUnitTest`):**
  - **Parpadeo del mapa al renderizar:** `mapView.controller.setZoom()`/
    `.animateTo()` se llamaban antes de que el `MapView` tuviera dimensiones
    de layout reales (clásica carrera de osmdroid embebido en Compose vía
    `AndroidView`), lo que calculaba una proyección incorrecta y producía un
    frame visible en el zoom/posición equivocados antes de corregirse.
    Corregido envolviendo el posicionamiento de cámara en `mapView.post { }`.
    Además, esas animaciones programáticas disparaban ellas mismas eventos
    `onScroll`/`onZoom` en el `MapListener`, realimentando el auto-refresco
    del área — corregido con un flag `isProgrammaticCameraMove` que los
    silencia mientras dura nuestra propia animación.
  - **Filtro de tipo de conector invisible / solo 3 tipos visibles:** dos
    causas distintas. (1) `ChargerMapper` descartaba en silencio cualquier
    cargador cuyo operador de OCM no reconociera (ya corregido en la ronda
    anterior, ver más abajo). (2) El propio `ConnectorType` solo modelaba 3
    valores; ahora modela los **9 tipos reales** que aparecen en
    `GET /v3/referencedata` de OCM (`TYPE_1`, `TYPE_2`, `TYPE_3`, `CCS1`,
    `CCS2`, `CHADEMO`, `TESLA`, `DOMESTIC`, `WIRELESS`, más `UNKNOWN` de
    reserva) y el filtro/ficha/lista los muestra todos.
  - **"Combo 2 de 100 kW" se mostraba como "Tipo 2":** el título real de OCM
    para CCS2 contiene literalmente la subcadena `"Type 2"` (p. ej.
    `"CCS (Type 2)"`), y `mapConnectorType()` comprobaba `contains("Type 2")`
    antes que `contains("CCS")` — corregido reordenando las comprobaciones
    (CCS/Combo/CHAdeMO/Tesla/inalámbrico/doméstico/Type 3 siempre antes que
    los genéricos "Type 2"/"Type 1").
  - **Cargadores de Repsol no abrían ninguna app ("¿no es Waylet?"):**
    Waylet (`com.mdf.repsol`) y Electromaps (`com.enredats.electromaps`)
    estaban en `roaming.json` como apps candidatas de Zunder/Endesa X, pero
    no existían todavía como entradas propias en `providers.json` — el
    lanzador no podía resolverlas. Añadidas ambas (package name verificado).
  - **Selector de idioma no aparecía / mostraba el listado completo de
    idiomas del mundo:** `context.resources.assets.locales` devuelve todos
    los locales para los que hay CUALQUIER recurso en el APK final,
    incluidos los que traen las propias librerías (AppCompat/Material
    traducen sus textos internos — "OK", "Cancelar"... — a 60-90 idiomas).
    Corregido con `res/xml/locales_config.xml` (declara solo los idiomas
    que traducimos de verdad: `en`, `es`) + `android:localeConfig` en el
    manifest, leído directamente con un `XmlPullParser` sobre ese XML — no
    existe ningún método público en `androidx.core` (`androidx.core:core`
    1.18.0) que lea ese `locale-config` por nosotros (se intentó
    `LocaleManagerCompat.getApplicationSupportedLocales`, que no existe en
    esa clase; solo expone `getSystemLocales`/`getApplicationLocales`), así
    que el parseo del XML se hace a mano en `SettingsScreen.kt`.
  - **Elegir un idioma no traducía nada:** `MainActivity` extendía
    `ComponentActivity` en vez de `AppCompatActivity` — el hook automático
    de recreación de actividad que dispara
    `AppCompatDelegate.setApplicationLocales()` solo funciona sobre
    `AppCompatActivity`. Corregido cambiando la clase base.
  - **No se veía el buscador de tipo de cargador (filtros tapados):** ya
    corregido en la ronda anterior (`012963e`); confirmado que sigue
    correcto tras esta ronda de cambios.
- ✅ **Resuelto (2026-09-10): crash al abrir la app.** `AppCompatActivity`
  (appcompat 1.7.0) no delega `setContentView()` en
  `ComponentActivity.setContentView()`, así que nunca queda fijado
  `ViewTreeNavigationEventDispatcherOwner` en el decor view — algo que
  `NavDisplay` (navigation3-ui 1.0.1) exige y sin lo cual lanza
  `IllegalStateException` al arrancar. Causa raíz verificada byte a byte
  con `javap` sobre los `.class` reales. Corregido proveyendo
  `LocalNavigationEventDispatcherOwner` explícitamente en `MainActivity`
  (que ya es `NavigationEventDispatcherOwner` por herencia de
  `ComponentActivity`), sin volver a `ComponentActivity` puro (rompería el
  selector de idioma).
- ✅ **Resuelto (2026-09-10): tercera ronda de bugs reportados en
  dispositivo real, todos corregidos, revisados por `researcher-android` y
  compilados en verde antes de entregar (proceso builder→researcher ya
  aplicado por primera vez de principio a fin, CLAUDE.md sección 8):**
  - **Mercadona y Repsol NO estaban mapeados de verdad** (a pesar de
    parecerlo): el título real de OCM para Mercadona es `"Mercadona"` (no
    aparecía como Iberdrola en el código) y para Repsol es
    `"Repsol - Ibil (ES)"` (no existía ninguna entrada) — ambos caían como
    "operador sin mapear". Añadidos a `OcmOperatorMapping.kt`, con
    `"Mercadona" → iberdrola` y `"Repsol - Ibil (ES)" → waylet`; además
    `"iberdrola"` gana una entrada de roaming hacia `waylet` en
    `roaming.json` (Mercadona instala puntos de ambos operadores).
  - **EDP y Eranovum no tenían app propia dada de alta:** añadidos `edp`
    (`es.edp.edpcharge`, "EDP Charge") y `eranovum`
    (`com.placetoplug.eranovum`) a `providers.json` y `OcmOperatorMapping.kt`
    (`"EDP"` y `"Eranovum (ES)"`); `roaming.json` amplía
    `"eranovum": ["zunder", "waylet"]` (antes solo Zunder).
  - **Regla de resolución multi-app revisada (ver sección 0/3/5):** ahora,
    cuando hay dos o más apps candidatas **instaladas a la vez**, se
    muestra un selector (`LaunchResult.NeedsDisambiguation`,
    `ProviderDisambiguationDialog` en `Navigation.kt`) en vez de resolver
    en silencio hacia el operador nativo. Los casos "una instalada" y
    "ninguna instalada" no cambian.
  - **"El mapa está delante de los menús":** conflicto de compositing entre
    el `AndroidView` que envuelve el `MapView` de osmdroid y los `Surface`
    internos de Material3 (que usan compositing offscreen para su sombra/
    elevación). Corregido forzando el mismo modo de compositing en el
    `AndroidView` (`Modifier.graphicsLayer(compositingStrategy =
    CompositingStrategy.Offscreen)`). Verificado que `ModalBottomSheet` y el
    nuevo `AlertDialog` de desambiguación no se ven afectados (ventana
    propia de Material3).
  - **Iconos en el filtro de tipo de conector** (petición de producto, para
    agilizar el escaneo visual): cada `FilterChip` de `FilterRow` gana un
    `leadingIcon` (Power para AC lento, Bolt para DC rápido, ElectricCar
    para Tesla —sin logo de marca—, Outlet para doméstico, Wifi para
    inalámbrico), manteniendo el texto completo en `label` (TalkBack sigue
    anunciando el nombre completo del conector — confirmado que `FilterChip`
    de Material3 fusiona icono+label+estado en un único nodo de semántica).
  - **Radio de carga limitado a una sub-zona pequeña al hacer zoom out:**
    `reportMapMoved()` calculaba correctamente el radio a partir de la
    diagonal real del área visible del mapa, pero lo limitaba a 100 km — si
    el usuario hacía zoom a una zona más amplia, la consulta a OCM se
    quedaba fija en ese círculo. Subido a 500 km. **Superado por el
    rediseño de 2026-09-10 (ver más abajo): el tope de área en sí era la
    causa, no su tamaño — sustituido por consulta directa al rectángulo
    visible (`boundingbox`), que no tiene ese problema a ningún zoom.**

- ✅ **Resuelto (2026-09-10), cuarta ronda — rediseño de la consulta al mapa
  + inversión del modelo de filtros + mejoras visuales, trabajado
  directamente por el usuario con un agente (fuera del flujo normal de
  handoff builder→researcher de la sesión principal) y revisado a
  posteriori por `researcher-android` antes de compilar/comprometer (OK en
  ambos casos, con reservas menores no bloqueantes anotadas abajo):**
  - **"Círculo de cargadores" al hacer zoom out (causa raíz, no solo el
    síntoma):** subir el tope de radio (100→500 km, ronda anterior) solo
    retrasaba el bug, no lo eliminaba — cualquier círculo con tope deja
    fuera las esquinas de un rectángulo visible más grande. Sustituido por
    consulta directa al **rectángulo visible real** vía el parámetro nativo
    `boundingbox` de OCM (`GET /v3/poi?boundingbox=(lat,lng),(lat2,lng2)`,
    esquina noroeste primero — verificado independientemente contra
    `ocm-openapi-spec.yaml` y `POIManager.cs` del repo oficial
    `openchargemap/ocm-system`: al usar `boundingbox`, el servidor anula
    `latitude`/`longitude`/`distance` sin conflicto). Por construcción, un
    rectángulo no puede producir el artefacto circular a ningún nivel de
    zoom — ya no hace falta ningún tope de área. `maxResultsFor` (200–5000)
    sigue siendo el único límite; riesgo ya documentado y aceptado (no se
    sabe si OCM impone un tope propio por debajo de 5000).
  - **Filtros de conector "se borraban" en cada recarga:** `MapViewModel`
    construía un `MapUiState.Success` nuevo sin arrastrar `filters` en cada
    recarga (GPS/búsqueda/pan-zoom) — corregido preservando los filtros del
    estado anterior.
  - **Chips de filtro "al revés":** `ChargerFilters` pasa de modelo de
    inclusión (`connectorTypes`, vacío = sin filtrar, chip activado = tipo
    incluido) a modelo de **exclusión** (`excludedConnectorTypes`, vacío =
    nada excluido = todo visible = todos los chips activados/en color por
    defecto; pulsar un chip lo excluye). Coincide con la expectativa real
    del usuario ("si el icono se ve en color está activado").
  - **Glifo Tesla/NACS:** el icono genérico de coche eléctrico se sustituye
    por una silueta esquemática propia del conector NACS real (cápsula
    compacta + 5 pines en línea, dibujada a mano con `Canvas`/`DrawScope`,
    igual que Type 1/2/3/CCS/CHAdeMO de la ronda anterior) — verificada
    contra descripciones técnicas públicas del conector (no una imagen con
    copyright), sin usar el logo de la marca.
  - **Icono de la app en 3D:** el rayo bronce plano gana volumen (sombra
    desplazada + degradado diagonal + brillo especular recortado a la
    misma silueta) vía gradientes `<aapt:attr>` dentro del `VectorDrawable`
    — sintaxis oficial de AAPT2, confirmada contra la documentación de
    Android.
  - **Nuevo documento de investigación:** `docs/providers/no-app-operators.md`
    — operadores europeos sin app propia de consumidor confirmada (Ubitricity/
    Shell, ChargePlace Scotland, Berliner Stadtwerke, MOBI.E, GreenFlux, PGE,
    sindicatos departamentales franceses), con fuente citada por fila y
    "Unclear" donde no hay certeza. Reserva menor no bloqueante: la cita de
    Ubitricity es más débil de lo que sugiere el documento (una de las dos
    fuentes no confirma explícitamente la retirada de la app propia).
  - **i18n completa: los 24 idiomas oficiales de la UE + 5 cooficiales,
    convertidos a `strings.xml` real en los 4 módulos con UI (108 ficheros
    nuevos), no solo ES/EN.** 24 oficiales: además de ES/EN/FR/DE del MVP,
    ahora también IT, PT, NL, PL, RO, CS, SK, HU, BG, EL, HR, SL, SV, DA,
    FI, ET, LV, LT, GA, MT. Más 5 cooficiales traducidos **desde cero** (sin
    borrador previo): CA (catalán), EU (euskera), GL (gallego), LB
    (luxemburgués), FY (frisón occidental). Todos dados de alta en
    `locales_config.xml` (29 entradas: EN+ES+27). **Corrección importante
    sobre el propio proceso:** los borradores en `docs/i18n/*.properties`
    (sección 0/6, "ya redactados") resultaron estar **obsoletos** — usaban
    un esquema de claves de una fase de producto anterior
    (`charger_detail_address_label`, `paywall_*`, `map_legend_*`...) que no
    coincide con ninguna clave real que el código actual use. Convertirlos
    mecánicamente habría producido 27 idiomas de strings muertos sin
    traducir la UI real. En su lugar, cada traducción se generó desde el
    inventario real de 39 claves de los 4 módulos actuales — `docs/i18n/*.properties`
    queda como material obsoleto, no como fuente de esta traducción.
    Validado estructuralmente (XML bien formado, mismo conjunto de claves
    que el fichero base por módulo, sin duplicados, marca "AutoRecargas
    Pegaso" y términos técnicos CCS1/CCS2/CHAdeMO/Tesla/"≥ 50 kW" intactos
    sin traducir, placeholder `%1$s` conservado) en los 112 ficheros no-base
    de los 4 módulos — **no** es una revisión de calidad lingüística.
    **Pendiente antes de publicar (ver también sección 0/12 anteriores):**
    QA por hablante nativo, con menor confianza señalada explícitamente en
    irlandés y maltés, y extendida de facto a todo el lote de 27 (catalán,
    euskera y gallego con algo más de confianza por ser lenguas de mayor
    recurso; euskera, luxemburgués y frisón sin precedente previo de
    localización de UI en el proyecto).
  - **Parpadeo que afectaba a todo el formato/menús al abrir el mapa:**
    `MapViewModel` arrancaba en `MapUiState.Loading` (un spinner sin
    buscador/filtros/mapa) y tardaba varias llamadas de red en llegar al
    primer `Success`, montando todo el resto de golpe. Corregido
    arrancando directamente en `MapUiState.Success(emptyList(),
    isRefreshing = true)` — el buscador/filtros/mapa se montan en el primer
    frame, con un `LinearProgressIndicator` mientras `isRefreshing` es
    verdadero.

Ya no hay pendientes 🔶 de decisión del product owner sobre el alcance — lo
que queda de la lista original es ejecución:

1. `researcher-android` y `researcher-ios` documentan primero Ionity,
   Tesla y Fastned (confirmadas para el MVP) en `docs/providers/<red>.md`,
   y después las 10 redes restantes (sección 0/5).
2. Ambos researchers compilan `docs/providers/roaming-agreements.md` (la
   tabla `RoamingPartnerships` de la sección 5) a partir de acuerdos de
   roaming anunciados públicamente.
3. `planner` genera el primer plan técnico cubriendo Android e iOS en
   paralelo (equipos ya confirmados por separado), validando que las 13
   redes de la sección 0 caben en el plazo de 2 meses por plataforma.
4. Integrar la API de Open Charge Map (`opendata=true`) en ambas apps, y
   excluir/tratar aparte cualquier punto con licencia restringida.
5. ✅ **Fase 3 completada en Android** (2026-09-10): los 24 idiomas
   oficiales de la UE + 5 cooficiales (catalán, euskera, gallego,
   luxemburgués, frisón) están convertidos a `values-<lang>/strings.xml`
   real en los 4 módulos Android con UI — ver detalle y matices en la
   entrada de la sección 12 de esta misma fecha (en particular, que
   `docs/i18n/*.properties` quedó obsoleto y no se usó como fuente).
   **Pendiente para iOS:** convertir a `<lang>.lproj/Localizable.strings`
   cuando exista el scaffold real de esa plataforma. **Antes de publicar
   cualquiera**, pasada de QA por hablante nativo — en particular irlandés
   (`ga`) y maltés (`mt`), señalados como los de menor confianza; el resto
   también sin revisión nativa, solo validado estructuralmente (no
   lingüísticamente). Decidir si el lanzamiento es con los 29 desde el día
   1 o escalonado (4 del MVP al inicio) sigue siendo una decisión de
   producto abierta, no técnica.
6. ✅ Primer borrador de la política de privacidad en
   `docs/legal/privacy-policy.md` — **tiene campos `[PENDIENTE: ...]`** (NIF/
   razón social, email de contacto, fecha de publicación) que hay que
   rellenar antes de publicar. Falta también la página mínima con el mismo
   contenido para cubrir el campo de URL obligatorio de Play Console/App
   Store Connect.
7. Validar accesibilidad básica (TalkBack/VoiceOver, texto escalable,
   contraste) en las primeras pantallas que construya cada builder, no
   como revisión final de última hora.
8. Dar de alta el producto de suscripción (0,99 €/año, prueba de 3 días,
   sin Family Sharing/Google Play Family) en Play Console y App Store
   Connect antes de construir el paywall.
9. **Rehacer el mockup visual de navegación:** el mockup anterior (Android,
   pestañas Mapa/Actividad/Pagos/Perfil, sesión de carga en vivo) ya no
   corresponde a este alcance ni cubre iOS — queda pendiente uno nuevo
   centrado en Mapa + Ficha de cargador + Escáner QR + selector de idioma +
   paywall de suscripción, idealmente mostrando ambas plataformas. Ya no
   incluye un selector multi-app bloqueante (sección 0).
