# Política de privacidad — AutoRecargas Pegaso

> **Fuente de verdad (CLAUDE.md sección 10):** este fichero vive versionado
> en el propio repositorio, no como página externa mantenida aparte. Se
> empaqueta como asset dentro de ambas apps (Android: `res/raw` o similar;
> iOS: recurso del bundle) y se muestra en una pantalla "Acerca de/Privacidad"
> accesible desde ambas. Para cumplir el campo de URL obligatorio de Play
> Console y App Store Connect, publicar este mismo contenido en una página
> mínima (p. ej. GitHub Pages) — sin que eso cambie dónde vive el original.
>
> ⚠️ **Campos pendientes de rellenar antes de publicar** (marcados como
> `[PENDIENTE: ...]`): no se puede publicar la app con estos huecos sin
> rellenar — no son un ejemplo, son huecos reales.

**Última actualización:** [PENDIENTE: fecha de publicación real]

## 1. Quiénes somos

AutoRecargas Pegaso es una app (Android e iOS) que ayuda a localizar
cargadores de vehículo eléctrico y abrir la app oficial del operador
correspondiente. Responsable del tratamiento:
[PENDIENTE: nombre legal de la empresa/autónomo, NIF, dirección de
contacto]. Contacto para cuestiones de privacidad:
[PENDIENTE: email de contacto].

## 2. Qué datos tratamos

- **Ubicación (mientras usas la app, en primer plano):** para centrar el
  mapa y mostrarte cargadores cercanos. No se envía a nuestros servidores
  porque no tenemos servidor propio (CLAUDE.md sección 3) — se usa
  localmente en el dispositivo y para consultar la API pública de Open
  Charge Map. No guardamos un historial de tus ubicaciones.
- **Cámara:** solo mientras tienes abierto el escáner QR, para decodificar
  el código del cargador. No se graba ni almacena ninguna imagen ni vídeo.
- **Datos de suscripción:** la compra y renovación de la suscripción
  (0,99 €/año) la gestiona directamente Google Play / App Store con tu
  cuenta de Google/Apple — nosotros no vemos ni almacenamos tu método de
  pago. Aplica también la política de privacidad de Google/Apple para ese
  proceso.
- **Datos técnicos de estabilidad (Firebase Crashlytics/Analytics):**
  información de fallos y uso agregado del dispositivo (modelo, versión de
  SO, identificador de instalación) para detectar errores y mejorar la app.
- **Favoritos y preferencias (idioma, entorno visual):** se guardan
  únicamente en tu dispositivo (DataStore/UserDefaults), no en un servidor
  nuestro.
- **No recogemos:** ni cuentas de usuario propias, ni historial de
  recargas, ni datos de pago de la carga en sí (todo eso ocurre en la app
  del operador, fuera de nuestro control — revisa su propia política de
  privacidad).

## 3. Con quién compartimos datos

- **Open Charge Map:** consultamos su API pública para obtener ubicaciones
  de cargadores (licencia ODbL). Solo enviamos coordenadas aproximadas para
  la búsqueda, sin identificarte.
- **Google / Apple:** gestión de la suscripción y, si aplica, de la cuenta
  usada para instalar la app.
- **Firebase (Google):** telemetría de estabilidad y uso agregado, según lo
  descrito arriba.
- **La app del operador que abras:** en cuanto abrimos su app o su ficha en
  la tienda, deja de aplicar esta política — aplica la suya.
- No vendemos datos personales a terceros con fines publicitarios.

## 4. Tus derechos (RGPD)

Al no mantener una cuenta de usuario ni un histórico identificable en
servidores propios, la mayoría de tus datos personales viven únicamente en
tu dispositivo y los controlas tú (desinstalar la app los elimina). Para
cualquier solicitud de acceso, rectificación o supresión sobre lo poco que
sí tratamos (soporte técnico, telemetría de fallos), contacta con
[PENDIENTE: email de contacto]. Tienes derecho a reclamar ante tu autoridad
de protección de datos (en España, la AEPD).

## 5. Menores de edad

La app no está dirigida a menores de edad sin supervisión de un adulto,
dado que gestiona el acceso a servicios de pago de terceros (la propia
carga del vehículo).

## 6. Cambios en esta política

Si cambiamos esta política de forma relevante, se actualizará este mismo
fichero (con fecha de la sección 1) y se avisará dentro de la app.
