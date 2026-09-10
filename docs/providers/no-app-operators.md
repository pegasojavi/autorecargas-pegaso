# Operadores sin app propia — candidatos a "solo operador nativo, sin roaming propio conocido"

Investigación específica (2026-09-10) para responder: ¿qué operadores/redes
de carga habituales en el dataset de Open Charge Map en Europa **no tienen
app propia de consumidor** (localizar/iniciar/pagar), y qué usa el
conductor en su lugar? Solo se listan aquí los que dieron **No** o
**Unclear** tras búsqueda real — cualquier red donde se confirmó app propia
queda fuera de esta tabla (ver nota al final sobre los casos "célebres por
no tener app" que sí la tienen ya).

**Hallazgo principal:** la mayoría de operadores de tamaño medio/grande que
aparecen habitualmente en OCM (Freshmile, IZIVIA, Bump, Driveco, PowerDot,
GreenWay, Recharge/Recharge.no, Virta, Orlen Charge, Q8, Circle K, Be
Charge, Enel X Way, A2A E-Moving, Duferco D-Mobility, E.ON Drive, Aral
pulse, Compleo, Wien Energie, Eneco eMobility, ESB ecars, EasyGo/EZO,
e-Totem) **sí tienen app propia confirmada en 2026** y por tanto no
aparecen en la tabla de abajo. Los casos genuinos de "sin app" son más
escasos de lo que sugiere la fama histórica del farolas-charging del Reino
Unido, y se concentran en: (a) redes absorbidas por una marca mayor que
retiró la app propia, (b) esquemas municipales que solo ofrecen pago ad hoc
(QR/NFC) sin ninguna app ni de operador ni de terceros obligatoria, (c)
"backbones" de interoperabilidad que no son comercializadores en sí
mismos, y (d) plataformas backend white-label que no exponen app de marca
propia.

## Tabla

| País | Operador/red (tal como aparecería en OCM) | ¿App propia? | Cómo carga el usuario en la práctica | Fuente |
|---|---|---|---|---|
| Reino Unido | **Ubitricity** (rebrandeado "ubitricity Shell Recharge") | No — Shell retiró la app propia de Ubitricity | App **Shell Recharge** (ya documentada en `docs/providers/shell-recharge.md`), o pago ad hoc escaneando el QR del punto → página web de pago (tarjeta/Apple Pay/Google Pay), sin necesidad de ninguna app | [ubitricity — rebranding Shell Recharge](https://ubitricity.com/en/driver/shell-recharge-rebranding/), [Open Charge Map — foro comunidad](https://community.openchargemap.org/t/ubitricity-rebrand-to-shell-recharge/1226) |
| Reino Unido (Escocia) | **ChargePlace Scotland (CPS)** | Unclear — red en transición/fragmentación durante 2026, la app y tarjeta RFID propias (operadas por Swarco) están quedando obsoletas según cada punto se transfiere a un nuevo operador regional (p. ej. ScottishPower en algunas islas/consejos) | Mientras dura la transición: apps de roaming como **Octopus Electroverse**, **Zapmap**, **Paua** o **AllStar**, o la app del nuevo operador que herede cada punto concreto | [ChargePlace Scotland — aviso de transición](https://chargeplacescotland.org/network-news/new-payment-provider/), [Paua — "ChargePlace Scotland transitions and the network fragments"](https://www.paua.com/article/paua-in-scotland-what-happens-as-chargeplace-scotland-transitions-and-the-network-fragments) |
| Alemania | **Berliner Stadtwerke** (red pública de carga de Berlín) | No — confirmado explícitamente por el propio operador: no ofrecen tarjeta ni app de carga para sus puntos públicos | Pago ad hoc directo escaneando el QR/chip NFC del punto (tarjeta de crédito o PayPal), sin contrato ni registro — cumple el mandato de pago ad hoc de AFIR (UE 2023/1804) mencionado en `CLAUDE.md` sección 5; su propia `berlinLadestrom-App` es solo para grupos privados en puntos privados, no da acceso a la red pública | [Berliner Stadtwerke — FAQ "¿Hay tarjeta o app de carga?"](https://berlinerstadtwerke.de/faq-beitrag/gibt-es-eine-ladekarte-oder-lade-app-der-berliner-stadtwerke/), [Berliner Stadtwerke — "¿Puedo cargar sin tarjeta o app?"](https://berlinerstadtwerke.de/faq-beitrag/ich-habe-keine-ladekarte-oder-lade-app-wie-kann-ich-trotzdem-laden/) |
| Portugal | **MOBI.E** | No, por diseño — MOBI.E no es un comercializador (CEME), es la red/backbone nacional de interoperabilidad que interconecta puntos y comercializadores; no tiene app de consumidor propia | Cualquier app o tarjeta de un **CEME** (comercializador de electricidad para movilidad eléctrica) conectado a la red, p. ej. **EVIO** (carga ad hoc sin tarjeta ni contrato), Prio, Galp, Repsol Portugal, etc. | [MOBI.E — "Nova app EVIO permite carregamento sem cartão"](https://www.mobie.pt/en/w/nova-app-miio-permite-carregamento-veiculos-sem-cartao), [Visão — app Miio/EVIO en la red Mobi.E](https://visao.pt/volt/2021-01-28-app-miio-permite-carregar-na-rede-mobi-e-sem-cartao/) |
| Países Bajos | **GreenFlux** | Unclear/No propiamente — GreenFlux es una plataforma backend (CPMS) para CPOs/eMSPs, no un operador de cara al público; su "app" es white-label, personalizable por cada cliente | La app de marca propia del CPO/eMSP que use GreenFlux como backend (p. ej. Eneco eMobility, que sí tiene app propia confirmada — `docs/providers` no la incluye aún como red separada); no hay una "app GreenFlux" que el usuario final instale | [GreenFlux — "EV Charging Software"](https://www.greenflux.com/ev-charging-software/), [GreenFlux — "Charge Point Operators"](https://www.greenflux.com/charge-point-operator/) |
| Polonia | **PGE** (PGE Nowa Energia, red de carga) | Unclear — el acceso descrito exige registro/login en el portal web `PGEdoladujauto.pl`; no se ha encontrado confirmación clara de una app móvil de consumidor propia y activamente mantenida (a diferencia de GreenWay u Orlen Charge, que sí tienen app confirmada) | Login web en `PGEdoladujauto.pl`; posible app propia sin confirmar — **pendiente de verificación de campo** antes de asumir nada en `ProviderDirectory` | [elektrowoz.pl — "PGE Nowa Energia wprowadza opłaty... konieczne logowanie na portalu"](https://elektrowoz.pl/ladowarki/pge-nowa-energia-wprowadza-oplaty-za-ladowanie-od-31-lipca-konieczne-logowanie-na-portalu-pgedoladujauto-pl/) |
| Francia | **Sindicatos departamentales de energía** (p. ej. SyME05, varios "Syndicats Départementaux d'Énergies", redes tipo Mobive/Modulo/Révéo que despliegan puntos rurales) | No, típicamente — son entidades públicas/semi-públicas que instalan infraestructura pero no desarrollan una app de consumidor propia | **Chargemap Pass** (tarjeta/app universal, ya documentada en `docs/providers/chargemap.md`), o las apps de otros operadores con acuerdo de roaming sobre esos puntos (Freshmile, IZIVIA) | [automobile-propre.com — "ChargeMap Pass: badge único para el acceso a las bornes"](https://automobile-propre.com/chargemap-pass-badge-unique-acces-bornes-de-recharge/amp), [Mobive](https://mobive.fr/), [SEY78 — "Les Bornes de Recharge SEY Ma Borne"](https://www.sey78.fr/activites-du-sey/les-bornes-de-recharge) |

## Metodología y limitaciones

- Búsqueda real (WebSearch) por cada operador candidato, priorizando fuentes
  primarias (web oficial del operador, ficha de tienda de apps, comunicados
  de prensa) sobre agregadores de terceros. Cuando la evidencia era
  contradictoria o insuficiente se marcó **Unclear** con el motivo, nunca
  se asumió una respuesta.
- **No es exhaustivo.** OCM tiene cientos de operadores locales pequeños en
  Europa (sindicatos de energía, ayuntamientos, gasolineras independientes,
  aparcamientos privados) — esta tabla cubre los casos que la tarea pidió
  investigar explícitamente (redes de farolas del Reino Unido, esquemas
  municipales, y los operadores medianos/grandes listados en el encargo),
  no el universo completo del dataset.
- **Confirmado que SÍ tienen app propia hoy (por eso NO están en la tabla),
  a pesar de la fama histórica de "solo RFID/contactless" en el Reino
  Unido:** Char.gy ([Google Play](https://play.google.com/store/apps/details?id=com.chargy_limited.driverapp), [App Store](https://apps.apple.com/gb/app/char-gy/id1636840750)), Connected Kerb ([Google Play](https://play.google.com/store/apps/details?id=com.connectedkerb.cp.app)), GeniePoint ([Google Play](https://play.google.com/store/apps/details?id=com.geniecpms.GeniePointMobile)), Believ/Liberty Charge — mismo paquete `com.driivz.liberty`, aparente rebrand — ([Google Play](https://play.google.com/store/apps/details?id=com.driivz.liberty)), Source London ([App Store](https://apps.apple.com/gy/app/source-london-charging-network/id1585122990)), ESB ecars Irlanda (`ecar connect`), y EasyGo Irlanda (rebrandeado a **EZO** en 2025). Ubitricity es la excepción real: perdió su app al integrarse en Shell Recharge.
- **Municipios investigados que result quedar excluidos** (sí tienen app
  propia, contrario a la intuición de "ayuntamiento sin app"): Madrid
  (app **Electro EMT**) y Barcelona (app/servicio **Smou**, red Endolla).
  Ámsterdam no tiene una app municipal propia, pero tampoco constituye un
  "operador" identificable en OCM — el ayuntamiento subcontrata la gestión
  a CPOs con app propia (TotalEnergies, Vattenfall...) más pago ad hoc por
  QR como red de seguridad, en línea con el mandato de AFIR ya descrito en
  `CLAUDE.md` sección 5.
- **Pendiente de verificación de campo:** PGE (Polonia) y el alcance exacto
  de qué operadores concretos usan GreenFlux como backend en Países Bajos
  (para saber si su app blanca-marca debería documentarse aparte como red
  propia). No añadir ninguno de los dos a `ProviderDirectory` sin
  confirmar antes con una fuente más directa.
