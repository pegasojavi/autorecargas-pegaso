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
  revisada (sustituye la versión anterior "siempre preguntar"):**
  - Una única app candidata → se abre directamente (deep link / Universal
    Link / App Link, según plataforma); si no está instalada, se lleva al
    usuario a su ficha en la tienda.
  - **Más de una app candidata** (operador nativo del cargador + apps de
    roaming que también dan acceso, p. ej. Waylet/Electromaps sobre
    Endesa/Zunder/Eranovum): **se resuelve sin preguntar**, con esta
    prioridad:
    1. Si el usuario tiene **instalada exactamente una** de las apps
       candidatas → se abre esa, sea o no la del operador nativo.
    2. En cualquier otro caso (ninguna instalada, o varias instaladas a la
       vez) → se abre (o se lleva a la tienda de) la app del **operador
       nativo** del cargador, no una de roaming.
  - Ya no hay un selector obligatorio en el flujo por defecto (ver sección
    3 para el detalle técnico y sección 5 para cómo se compila qué
    cargadores son multi-app).
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
- **Lanzador de app de proveedor** (resuelve sin preguntar, ver sección 0
  para la regla completa):
  - Una sola app candidata → la abre directamente; si no está instalada,
    lleva a su ficha en la tienda.
  - Más de una app candidata (operador nativo + apps de roaming) →
    si exactamente una está instalada, se abre esa; si no, se usa la app
    del operador nativo (instalada o, si no, su ficha en la tienda).
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
- Filtro por tipo de conector / potencia, si el dataset lo soporta.
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
}

interface ChargerAppLauncher {
    // Resuelve sin preguntar al usuario (sección 0): una única app instalada
    // gana; si no, gana el operador nativo. Nunca bloquea con un selector.
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
}

protocol ChargerAppLauncher {
    // Resuelve sin preguntar al usuario (sección 0): una única app instalada
    // gana; si no, gana el operador nativo. Nunca bloquea con un selector.
    func resolve(charger: Charger) -> LaunchResult
    func launch(provider: ProviderAppInfo)
}
```

### Principios comunes a ambas plataformas

- **`ProviderDirectory`** (Android) / su equivalente en iOS es una **tabla
  estática mantenida por el equipo** (JSON o recurso embebido), **no** una
  API remota — cada plataforma mantiene la suya, poblada desde el mismo
  `docs/providers/<red>.md`.
- **Regla de resolución multi-app (idéntica en ambas plataformas, revisada):**
  cuando un cargador tiene más de una app candidata (`nativeProviderId` +
  `roamingProviderIds`), `resolve` decide **sin preguntar al usuario**: si
  hay exactamente una candidata instalada, se abre esa; en cualquier otro
  caso (ninguna o varias instaladas) se usa la del operador nativo. No hay
  selector en el flujo por defecto — ver sección 5 para cómo se compila la
  lista de `roamingProviderIds` de cada cargador.
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
- **Mapas (renderizado interactivo dentro de la app):** Google Maps SDK for
  Android — pines, clustering, pan/zoom.
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

**Regla de resolución (sección 0/3, sin selector por defecto):** una única
app instalada gana; si no, gana el operador nativo. Esta tabla de roaming
es la que alimenta esa decisión — mantenerla actualizada es tan importante
como `ProviderDirectory` en sí.

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
- **Android:** `context.resources.assets.locales` (`AssetManager`) devuelve
  exactamente los locales para los que hay recursos empaquetados en el APK.
  Mostrar cada idioma con su autónimo (`locale.getDisplayName(locale)` —
  p. ej. "Español", "Français" — no traducido al idioma actual de la UI).
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

**Fast-follows explícitamente diferidos en este scaffold (no son pendientes
de decisión, son trabajo pendiente de `builder-android`):**
- Hilt: se usa composición manual (`AppContainer`) porque no se pudo fijar
  con confianza una versión de KSP compatible con AGP 9.0.1/Kotlin 2.3.20
  sin poder probarlo — migrar cuando se confirme la combinación correcta.
- Mapa interactivo real (Google Maps Compose + pines): `feature:map` hoy
  muestra una lista, no un mapa — hace falta una API key de Maps que no se
  ha dado de alta todavía.
- Geolocalización real: coordenada fija (Madrid) en vez de
  `FusedLocationProviderClient` + permiso en runtime.
- Selector de idioma real: los 24 `.properties` de `docs/i18n/` no se han
  convertido todavía a `values-<lang>/strings.xml` — los textos de la UI
  actual están embebidos en Kotlin (violación puntual de la sección 9,
  pendiente de corregir junto con la conversión).
- Interpretación del contenido del QR escaneado (hoy solo cierra la
  pantalla al leer cualquier código).
- Icono de app real (se mantiene el genérico de la plantilla).

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
5. ✅ **Fase 3 adelantada por completo:** los 24 idiomas oficiales de la UE
   ya tienen fichero en `docs/i18n/*.properties` (43 claves cada uno, mismo
   formato), no solo los 4 del MVP — formato de staging neutro, pendiente
   de convertir a `values-<lang>/strings.xml` /
   `<lang>.lproj/Localizable.strings` cuando exista el scaffold real.
   **Antes de publicar cualquiera**, pasada de QA por hablante nativo — en
   particular irlandés (`ga`) y maltés (`mt`), señalados como los de menor
   confianza por ser los idiomas de menor recurso del lote; el resto
   también sin revisión nativa, solo de calidad razonable. Verificar
   además que el selector de idioma de cada plataforma los detecta
   dinámicamente (sección 6) y decidir si el MVP lanza con los 24 desde el
   día 1 o se mantiene el lanzamiento escalonado (4 al inicio) ya
   planteado en la sección 0.
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
