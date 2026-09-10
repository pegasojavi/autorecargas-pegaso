# Candidatos a operador sin mapear (OCM, dominio europeo)

Generado 2026-09-10 desde `GET /v3/referencedata` real (996 operadores en todo el mundo), filtrando a los que tienen `WebsiteURL` con dominio de país europeo (.es/.fr/.de/.it/.pt/.nl/.be/.uk/.se/.no/.dk/.pl/.cz/.ie/.fi/.gr/.hu/.ro/.bg/.hr/.si/.sk/.lt/.lv/.ee/.lu/.mt/.cy/.ch/.eu) y excluyendo los ya mapeados en `OcmOperatorMapping.kt` y los individuos privados (`IsPrivateIndividual`). Quedan **380** — la mayoria son operadores locales/pequenos/desconocidos, no verificados uno a uno (a diferencia de docs/providers/*.md, que si estan investigados). Esto es una lista de candidatos para revisar, no una confirmacion de que tengan o no app.

Uso: si reconoces un nombre de la lista (por haberlo visto en el mapa de la app), dime cual es su app real (nombre + package Android si lo sabes) y lo anado a `OcmOperatorMapping.kt`/`providers.json` igual que se hizo con Atlante/Electromaps/eTecnic.

**Corrección (2026-09-10, tras verificación real a petición del usuario):**
la suposición inicial de este párrafo ("los municipales seguramente no
tienen app") era **incorrecta** para el caso alemán — verificado con
búsquedas reales, ver tabla "Operadores municipales/regionales" más abajo.
Muchas eléctricas municipales alemanas SÍ tienen app propia (EWE Go,
mainova:charge, L-Charge de Leipzig...), y las que no, en su mayoría son
miembro de **ladenetz.de**, que tiene su propia app paraguas
(`de.ladenetz.app`) usada por 260+ eléctricas municipales — cubre a muchas
de golpe en vez de tener que mapear cada una por separado. El caso francés
(SDEG16 y similares sindicatos departamentales) sí parece confirmarse sin
app propia — dependen de tarjeta RFID + el agregador Chargemap. Sigue sin
verificarse uno a uno el resto de municipales; no asumir el patrón sin
comprobar cada bloque (alemán vs. francés vs. otros países ya se ha visto
que se comportan distinto).

## Verificados — ronda 1 (2026-09-10, `researcher-android`, vía búsqueda web real)

Petición explícita del usuario ("quiero verificar los 380 ya que esta app es
para distribución masiva"): primer lote real de verificación, priorizando
marcas paneuropeas/nacionales con más probabilidad de tener app de
consumidor. **No son 380 — son 15 de 380**, el resto sigue pendiente (ver
aviso arriba). Confianza distinta por fila, ver columna Notas — ninguno de
estos se ha volcado todavía a `OcmOperatorMapping.kt`/`providers.json`
(pendiente de que el usuario confirme cuáles ve realmente en su zona antes
de tocar código, mismo criterio que Atlante/Electromaps/eTecnic).

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| Osprey Charging (UK) | Osprey: EV Charging | `uk.co.ospreycharging.mobileapp` | Alta — resultado único y limpio. |
| InstaVolt Ltd | InstaVolt | `com.app.instavolt` | Alta — resultado único y limpio (existe también `com.app.instavolt_fleet` para flotas). |
| Grønn Kontakt | Mer Connect Norway (rebranding: Grønn Kontakt → Mer, propiedad de Statkraft) | `no.giantleap.gronn.kontakt` | Alta, pero ojo al rebranding — el nombre visible en tienda ya no es "Grønn Kontakt". |
| Greenway Polska (PL) / Greenway (SK, no está en la lista de 380 pero es la misma red) | GreenWay Slovakia / GreenWay EV Charging | `sk.greenway.evcharge` (Eslovaquia) / `pl.greenway.evcharge` (Polonia) | Alta — dos apps regionales confirmadas, mismo grupo. |
| CLEVER | Clever | `dk.clever.app` | Alta — no confundir con "CleverCharge" (`com.danlaw.evse`), app de terceros no relacionada. |
| Vattenfall InCharge | InCharge | `com.vattenfall.incharge` | Alta para UK/global; existe variante `nl.nuon.laadpunten` para el mercado neerlandés (marca Nuon, filial de Vattenfall). |
| Mobilize (FR) | Mobilize Pass | `com.renault.mobilize` | Alta — filial de Renault, resultado único y limpio. |
| Plenitude On The Road (EU) | Plenitude On The Road (antes "Be Charge") | `com.bepower.BeCharge` | Alta — coincide con el nombre ya listado en los 380. Ojo: Eni/Plenitude tiene además `com.eni.enigaseluce` (facturas gas/luz, no carga) y `com.eni.charging.app` ("eni.charge", posible app distinta para hubs propios) — no confundir los tres. |
| Blink Charging (Europe) / Blink Charging (UK) | Blink Charging UK & Ireland / Blink Drive | `com.blinknetwork.europe` (UK/Irlanda) / `com.blinkcharging.mobility` (roaming multi-país Europa) | Media-alta — dos apps distintas del mismo grupo, confirmar cuál aplica antes de mapear. |
| ESB Ecars | ecar connect (app histórica) | `com.esb.ecars` | **Baja/dudosa** — un artículo de Irish Times indica que ESB pidió a sus usuarios "ignorar" esta app; puede estar descontinuada a favor de "EV Plug In" (`com.driivz.mobile.android.esb.driver`, plataforma Driivz). No mapear sin confirmar cuál está activa ahora mismo. |
| Blue Corner (Belgium) | — | — | **No confirmada como app propia** — Blue Corner es una red de Blink Charging; probablemente cubierta por `com.blinkcharging.mobility` (Blink Drive) en vez de tener app propia distinta. No mapear como entrada independiente sin confirmar. |
| We Drive Solar (Netherlands) | Posible "Laadpaal" (mencionada en su web) | — | **Sin confirmar** — la búsqueda no dio un package inequívoco; requiere visitar directamente su ficha de Play Store. |
| Weev (Ireland) | My Weev | — | App confirmada por nombre, pero el único package encontrado (`com.plugsurfing.PulzeEV`) parece de una marca anterior (Pulze/Plugsurfing) antes del rebranding a Weev — **no usar sin confirmar en la ficha actual de Play Store**. |
| BP Pulse (UK) | bp pulse | Ambiguo: `com.aml.evapp`, `co.uk.bppulselive.app3` y `com.bp.mobile.bppulse.us` aparecen los tres asociados a "bp pulse" | **Conflicto sin resolver** — tres packages distintos en los resultados; no mapear hasta abrir la ficha real de Play Store desde UK y confirmar cuál es la app vigente. |

## Operadores municipales / regionales — verificación real (2026-09-10)

Petición explícita del usuario ("los operadores municipales también deben
buscarse") — corrige la suposición del aviso de arriba. Muestra verificada,
no las ~75 eléctricas municipales alemanas + sindicatos franceses
completos (sigue pendiente el resto uno a uno):

| Título en lista | App real | Package Android | Notas |
|---|---|---|---|
| ladenetz.de | ladeapp | `de.ladenetz.app` | **Umbrella app de 260+ eléctricas municipales alemanas** — candidata a "app de roaming" compartida para cualquier municipal alemana sin app propia confirmada, en vez de mapear cada una individualmente. Verificar primero si el `OperatorInfo.Title` real de OCM para cada Stadtwerke individual coincide con el nombre del propio Stadtwerke o ya aparece como "ladenetz.de" directamente. |
| EWE | EWE Go - Elektroauto laden | `de.ewe.go.app` | App propia confirmada, específica de carga pública (no confundir con "Mein EWE Energie", que es facturación doméstica). |
| Mainova | mainova:charge | `de.mainladen.b2c` | App propia confirmada, pero Mainova recomienda a sus clientes usar además la app de **Shell Recharge** (ya mapeada) para localizar sus puntos públicos — puede que convenga mapear Mainova hacia `shell-recharge` como alternativa además de su app propia. |
| Stadtwerke Leipzig SWL | L-Charge | `com.energy_app_provider.leipziger` | App propia confirmada, específica de Leipzig. |
| N-ERGIE | Ladeverbund+ | Sin confirmar (no se encontró package explícito) | App regional compartida (norte de Baviera), no exclusiva de N-ERGIE — varios operadores de la zona podrían compartirla. Pendiente confirmar package y qué otros operadores de la lista la comparten. |
| Stadtwerke Münster | — | — | **Sin app propia de localización de carga pública confirmada** — "münster:dynamisch" es de tarificación doméstica dinámica, no de carga pública. Probablemente depende de ladenetz.de o similar; no mapear como app propia. |
| SDEG16 (FR) | — | — | **Sin app propia confirmada** — red "MobiVE", acceso por tarjeta RFID, puntos visibles vía Chargemap (agregador de roaming, ya mapeado). Confirma el patrón francés de "sin app propia" para este caso concreto — no generalizar sin comprobar los demás sindicatos departamentales de la lista. |

**Conclusión práctica:** no existe un patrón único "municipal = sin app" ni
"municipal = con app" — varía por país y por operador concreto, hay que
seguir verificando. El hallazgo de mayor valor es **ladenetz.de** como
umbrella: antes de buscar cada Stadtwerke alemán de la lista una a una,
tiene más sentido (a) confirmar qué título exacto usa OCM para cargadores
servidos por ladenetz.de y (b) verificar cuáles de las ~75 eléctricas
municipales alemanas de la lista son miembro de ladenetz.de vs. tienen app
propia como EWE/Mainova/Leipzig.

## Verificados — ronda 3 (2026-09-10, a partir de un cargador real encontrado por el usuario)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| Plenergy (ES) | Plenergy | `com.plenoil.plenergyapp` | Alta — título OCM verificado en vivo (`Plenergy (ES)`, coincide exacto), app y package confirmados en Google Play (desarrollador PLENERGY GRUPO SL), la propia web de Plenergy (`plenergy.es/carga-electrica/`) confirma que la app cubre carga eléctrica, no solo combustible. |

**Siguiente ronda:** continuar con el resto de marcas reconocibles no
municipales (p. ej. Scottish Power, Naturgy, Moeve, Rompetrol, Kople, Lad
Opp, ZSE Drive, Silverstone Green Energy...), completar la lista de
eléctricas municipales alemanas restantes (~70 más) contrastando membresía
en ladenetz.de antes de buscarlas una a una, y verificar el resto de
sindicatos departamentales franceses (SDET, SDEY, Sigeif, USEDA) para
confirmar si comparten el mismo patrón "sin app propia" de SDEG16. Seguir
priorizando los cargadores reales que el usuario encuentre sobre el mapa
(como Plenergy) frente a la búsqueda ciega del resto de la lista.

## Clasificación por país (petición explícita del usuario: "toda Europa... los 380 no hay discusión y además separados por país")

Clasificación determinista de los 380 títulos por país, hecha a partir del
sufijo entre paréntesis del propio título cuando existe, o si no del TLD de
la URL de referencia — sin necesidad de búsqueda web para esta parte, así
que está completa para los 380 desde ya (no es progresiva como las tablas
de verificación de app/package de arriba). Algunas asignaciones son
inferencia razonable cuando el título no trae país explícito y el TLD es
genérico (`.eu`/`.com`) — marcadas "país inferido" en vez de "confirmado".
Esto NO sustituye la verificación de app real — es el paso previo para
poder atacar la lista país a país como pediste, en vez de en orden
alfabético sin criterio.

| País | Nº de candidatos | Ya verificados (ver tablas arriba) |
|---|---|---|
| Alemania (DE) | ~78 (incluye ~55 Stadtwerke/eléctricas municipales) | ladenetz.de, EWE, Mainova, Stadtwerke Leipzig, N-ERGIE (parcial), Stadtwerke Münster |
| Reino Unido (UK) | ~35 | Osprey, InstaVolt, BP Pulse (ambiguo), Blink Charging UK |
| Francia (FR) | ~28 (incluye sindicatos departamentales) | Mobilize, SDEG16 (sin app) |
| Italia (IT) | ~22 | — |
| España (ES) | ~18 | Plenergy |
| Hungría (HU) | ~17 | — |
| Polonia (PL) | ~11 | GreenWay Polska |
| Países Bajos (NL) | ~11 | We Drive Solar (sin confirmar) |
| Rumanía (RO) | ~10 | — |
| Irlanda (IE) | ~10 | ESB Ecars (dudosa), Weev (sin confirmar) |
| Austria (AT) | ~9 | — |
| República Checa (CZ) | ~8 | — |
| Bélgica (BE) | ~8 | Blue Corner (sin confirmar) |
| Finlandia (FI) | ~8 | VIRTA |
| Eslovaquia (SK) | ~7 | GreenWay Slovakia |
| Portugal (PT) | ~7 | — |
| Suecia (SE) | ~6 | — |
| Dinamarca (DK) | ~5 | CLEVER |
| Noruega (NO) | ~4 | Grønn Kontakt/Mer |
| Eslovenia (SI) | ~4 | — |
| Croacia (HR) | ~2 | — |
| Lituania (LT) | ~4 | — |
| Letonia (LV) | ~2 | — |
| Estonia (EE) | ~3 | — |
| Suiza (CH, no UE pero sí Europa) | ~5 | — |
| Bulgaria (BG) | ~1 | — |
| Chipre (CY) | ~2 | — |
| Luxemburgo (LU) | ~2 | — |
| Malta (MT) | ~1 | — |
| Multi-país / EU genérico (marca paneuropea sin país único) | ~7 | Plenitude On The Road, Vattenfall InCharge |

## Verificados — Polonia, lote 1 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| TAURON Nowe Technologie | eTAURON | `pl.tauron.android.app` | Alta — sustituye a la app anterior "eMap"; no confundir con "Mój TAURON" (`pl.tauron.mtauron`, facturación) ni "TAURON eLicznik" (`tauron.ui`, consumo). |
| Orlencharge | ORLEN Charge (integrado dentro de la app ORLEN VITAY) | `pl.orlen.vitay` (app contenedora, no standalone) | Media — no existe app standalone "ORLEN Charge"; el servicio vive dentro de VITAY, confirmar antes de mapear si conviene apuntar a esa app contenedora. |
| PGE Nowa Energia | Nowa Energia (monitorización de consumo) | `com.nowaEnergia` | **Dudosa** — la app encontrada parece ser de consumo/huella de carbono, no de localización de carga pública; no mapear sin confirmar que es la app de carga real. |

## Verificados — Hungría, lote 1 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| MVM Partner Zrt. | Mobiliti (MVM TöltőPont) | `com.tbdb.toltopont` | Alta — resultado único y limpio. |
| E-Mobi (HU) | — | — | **Búsqueda contaminada** — el resultado obtenido (`com.csdd.emobi`) es la app de E-mobi de **Letonia**, no de Hungría; nombre homónimo, país distinto. Pendiente de repetir búsqueda específica para la app húngara real. |

## Verificados — Chequia, lote 1 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| ČEZ | futurego | `com.driivz.mobile.android.cez.driver` | Alta — app de carga pública del grupo ČEZ (República Checa y resto de Europa vía roaming). Existe también "ČEZ ESCO" (`cz.cez.wallbox.maui`) para wallboxes domésticas — no confundir. |

## Verificados — Austria, lote 1 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| Salzburg AG | Stromladen | `com.beenergised.chargemobile.salzburgag` | Alta — resultado único y limpio. |
| Kelag AG | Kelag-Mobility-App | `at.kelag.autostrom` | Alta — acceso a la red de roaming BEÖ (Bundesverband Elektromobilität Österreich) además de puntos propios de Kelag. |

## Verificados — Italia, lote 1 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| a2a emoving (IT) | A2A Emoving | `eu.a2a.emovingA2A` | Alta — resultado único y limpio, renombrada recientemente. |
| HeraRicarica Pubblica | Hera Ricarica | `com.siemens.hera.mobility` | Alta — resultado único y limpio. |
| Alperia (IT) | Alperia Charge | `eu.alperia.app.charge` | Media-alta — Alperia tiene varias apps (MyHome, app general `com.aew.mobile`); confirmar que `eu.alperia.app.charge` es la de carga pública vigente. |
| Free To X (IT) | — (usa "Muovi", app de Autostrade) | — | **Sin app propia en Google Play** — un foro de usuarios confirma que no existe app dedicada; se accede vía la app "Muovi" de Autostrade o el listado web. No mapear como entrada independiente. |
| eVISO (IT) | — | — | **Sin app de localización de carga clara** — las apps encontradas (`eASY – My eVISO`, `eVISO giro`) son de facturación/gamificación, no de localizar/pagar carga pública; no mapear sin confirmar. |

## Verificados — Países Bajos, lote 1 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| Essent (NL) | Essent Laadpas | `nl.essent.laadpas` | Alta — filial de RWE, resultado único y limpio. |
| Justplugin (NL) | Justplugin | `deftpower.justplugin.app` | Alta — resultado único y limpio, +950.000 puntos en Europa. |
| Greenflux | Charge Assist (nombre de producto, filial de DKV Mobility) | Sin confirmar | App confirmada por existir, package no localizado en la búsqueda. |

## Verificados — Alemania, lote 1 no-municipal (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| Aral pulse | Aral pulse | `com.aml.evapp.aral` | Alta — más de 70.000 puntos en Alemania. Nota: mismo prefijo `com.aml.evapp` que uno de los packages ambiguos de "bp pulse" UK (ronda 1) — bp es matriz de Aral, probablemente mismo proveedor de plataforma blanca. |
| Kaufland eCharge | Kaufland eCharge | `com.htb.kaufland` | **App descatalogada** — según una fuente, se retiró de Google Play el 3 de mayo de 2024; no mapear como app activa sin reconfirmar que sigue disponible. |
| ALDI SÜD (DE) | — | — | **Sin app propia de carga confirmada** — la carga aparece integrada en la web, no hay evidencia de app dedicada distinta del app general de la tienda (`de.apptiv.business.android.aldi_de`, que es de ofertas/compra, no de carga). |
| Tank & Rast | mblty (rebranding reciente) | Sin confirmar | App confirmada por existir (rebranding 2025 de la anterior app de e-movilidad de Tank & Rast), package no localizado en la búsqueda — antes de mapear, confirmar el package exacto en Play Store. |

## Verificados — España, lote 2 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| Moeve (ES) | Moeve (antes Cepsa Gow) | `com.cepsa.xmartplace` | Alta — rebranding de Cepsa a Moeve, +3.000 puntos de carga en la app. |
| Naturgy (ES) | Naturgy Easyrecarga (posible sucesora: "Naturgy Recarga") | `com.gnf.easyrecarga` (antigua) / `com.etecnic.naturgy` (nueva, mismo proveedor blanco "etecnic" que EVcharge/eTecnic) | Media — dos apps candidatas, confirmar cuál está vigente en la ficha real de Play Store antes de mapear (parece migración de plataforma, patrón visto también en Naturgy). |
| Electro-EMT | Electro-EMT | `com.etraid.ecove.ElectroEMT` | Alta — red municipal de Madrid (EMT), resultado único y limpio. |

## Verificados — Reino Unido, lote 3 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| Hubsta | Hubsta | `com.beenergised.chargemobile.hubsta` | Alta — resultado único y limpio. |
| Jersey Electricity Evolve | Evolve (Jersey Electricity) | Sin confirmar | App confirmada por existir, package no localizado en la búsqueda. |
| Jet Charge (UK) | — | — | **No encontrada app UK real** — los resultados solo devuelven apps homónimas de Australia (`au.com.jetcharge.connect`) y Kazajistán (`kz.jetcharge.prod`), sin relación con la entrada UK de la lista; no mapear sin verificación directa. |
| Joju Ltd | Joju Charging | `com.jojucharging` | Alta — parte de la red EVOpencard. |
| ElectRoad (UK) | ElectRoad | `uk.electroad.driverapp` | Alta — resultado único y limpio. |

## Verificados — Francia, lote 1 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| KiWhi Pass | KiWhi Pass (Easytrip) | `fr.easytrip.kiwhipass.android` | Alta — resultado único y limpio. |
| Métropolis | Metropolis Recharge | `fr.etotem.metropolis` | Alta — ojo con `com.teulys.metropolis`, app homónima distinta no relacionada. Cubre el Gran París, no Marsella pese al nombre genérico. |
| e-totem | E-TOTEM: Charging Stations | `fr.etotem.uti` | Alta — red de más de 667 zonas de carga en Francia. |
| Sigeif (FR) | — (usa IZIVIA, ya un operador de roaming conocido) | — | **Sin app propia** — la red Sigeif se opera a través de IZIVIA (app de terceros); aviso de fraude activo sobre webs falsas que ofrecen "su propia app". No mapear como entrada independiente. |
| pass pass électrique | Pass Pass VE | `com.byes.cd.passpass` | Alta — red de Hauts-de-France; no confundir con "Pass Pass Mobilités" (`fr.passpass.application.android`), que es transporte público, no carga EV. |
| Ouest Charge | Ouest Charge | `com.plugsurfing.ouestcharge` | Media-alta — también existe "Ouest Charge Pays de la Loire" (`com.spie.sieml`), red hermana distinta para otra región; confirmar cuál corresponde antes de mapear. |
| Power Dot / PowerDot (Es) | Powerdot (red paneuropea) | Sin confirmar — búsqueda devolvió apps homónimas no relacionadas (dispositivo de electroestimulación muscular) | **Pendiente** — requiere visitar directamente `powerdot.eu`/ficha de Play Store, la búsqueda por palabras clave no distingue bien de otras apps con nombre parecido. |

## Verificados — Reino Unido, lote 2 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| ForEV | FOR EV | `uk.co.forev.driverapp` | Alta — resultado único y limpio. |
| EZ-Charge | EZ Charge | `com.urbanintegrateduk.ezcharge` | Alta — resultado único y limpio. |
| CitiPark (UK) | CitiPark Mobile | `ax.parall.citiparkmobile` | Alta — parking con servicio de carga EV integrado ("CitiCharge"), no es una red EV pura pero sí abre puntos de carga reales. |
| Alfapower (UK) | My Alfa Power (MAP) | `com.paythru.alfapower` | Media — aparece también `com.fortum.alfapower` como alternativa en resultados de terceros; confirmar cuál es la ficha oficial vigente antes de mapear. |
| EB Charging | EB Go! | `com.ov.electricblue` | Alta — resultado único y limpio. |
| evyve | evyve / evyve Charge | `uk.co.evyve.app` o `uk.co.evyve.monta.evyve` | Media — dos apps distintas en Play Store con nombres muy similares; confirmar cuál es la vigente antes de mapear. |
| Silverstone Green Energy | — | — | **Sin app propia** — es una instaladora de paneles solares en Gales, no una red de carga con app de consumidor; no mapear. |
| Qwello | Qwello | `eu.qwello.app` | Alta — resultado único y limpio, red activa en varias ciudades europeas (no solo UK). |

## Verificados — Reino Unido, lote 1 (2026-09-10)

| Título en lista | App real | Package Android | Confianza / notas |
|---|---|---|---|
| Be.EV | Be.EV | `uk.co.be_ev.be_evapp` | Alta — resultado único y limpio. |
| The GeniePoint Network (EQUANS EV Solutions) | GeniePoint | `com.geniecpms.GeniePointMobile` | Alta — resultado único y limpio. |
| Project EV | Project EV Pro | `uk.co.projectev.cp.app` | Media — existe otra app también llamada "Project EV" con package `com.growatt.chargingpile` (fabricante chino de chargers); confirmar cuál usan los puntos reales antes de mapear. |
| Charge Your Car | Charge Your Car (CYC) | `net.corethree.chargeyourcar` | Media — package confirmado, pero la red CYC lleva años integrándose en redes mayores (histórico predecesor de bp pulse); confirmar que sigue operativa de forma independiente antes de mapear. |
| Evolt Network (Swarco E.Connect) | Evolt Network | `com.swarcoeconnect.swarcoeconnectapp` | Alta — resultado único y limpio. |
| Charge My Street (UK) | — (usa "Fuuse", app de terceros de marca blanca) | Sin confirmar | **Sin app propia** — no tiene app distintiva, depende de la plataforma Fuuse de terceros; package no localizado. |
| Scottish Power | ScottishPower Recharge (app de carga separada del app de facturación) | Sin confirmar en Android — solo se localizó en App Store (iOS id 6471003986) | **Sin confirmar en Android** — ojo con confundirla con `uk.co.scottishpower` (app de facturación doméstica, no de carga pública). |
| Plugged In Midlands (UK) | — | — | **Sin app propia** — red histórica ya integrada en POLAR/Chargemaster (predecesor de bp pulse); no mapear como entrada independiente. |
| Smart Charge by Sainsbury's (UK) | — | — | **Sin app propia** — se accede vía apps de terceros (Radius, Octopus Electroverse), no tiene app dedicada en Google Play. |
| SureCharge (FM Conway) (UK) | — | — | **Sin app propia confirmada** — indicios de que la red fue adquirida por Ubitricity (ya documentado como "sin app propia" en `docs/providers/no-app-operators.md`); no mapear sin confirmar. |

**Nota importante sobre el compromiso de verificación completa:** clasificar
por país (tabla de arriba) es determinista y ya está al 100%. Verificar la
app real + package de cada uno de los 380 con el mismo rigor que
Atlante/Electromaps/eTecnic (fuente primaria, no adivinado) es un trabajo
de una búsqueda real por candidato — con 380 candidatos son varios cientos
de búsquedas. Sigo haciéndolo de verdad, país por país empezando por los
grupos más grandes (Alemania, Reino Unido, Francia), en esta misma sesión y
en las siguientes — cada ronda se añade arriba con fuente y confianza real,
nunca inventada. Iré actualizando el recuento de "verificados" en esta
tabla a medida que avance.

| Titulo real en OCM | Web |
|---|---|
| a2a emoving (IT) | https://a2a.it/casa/emoving |
| AAE | https://www.aae.at/ |
| ABC Lataus (FI) | https://www.abcasemat.fi/fi/abc-lataus/sahkoauton-lataus |
| Acea (IT) | https://www.acea.it/e-mobility |
| AENA EV (ES) | https://www.aena.es/ |
| AGSM Electrify Verona (IT) | https://www.agsm.it/I-servizi/Electrify-Verona |
| AIMove (IT) | https://www.aimove.it |
| ALDI SÜD (DE) | https://www.aldi-sued.de/de/nachhaltigkeit/neuigkeiten/e-ladestationen.html |
| Alexela (EE) | https://www.alexela.ee |
| Alfapower (UK) | https://www.alfapower.co.uk/ |
| Alperia (IT) | https://www.alperiagroup.eu |
| AlterBase - Sorégies (FR) | http://www.alterbase86.soregies.fr/portal/#/ |
| Amperio (DE) | https://www.amperio.eu/ |
| Aral pulse | https://www.aral.de/de/global/retail/pulse.html |
| Autolib (Paris) | https://www.autolib.eu/ |
| autoPilDYK | http://autopildyk.lt/ |
| Avacon | https://www.avacon.de/ |
| Badenova (DE) | https://www.badenova.de/web/Privatkunden/E-Mobilit%C3%A4t/%C3%96ffentliches-Laden/index-2.jsp |
| Ballenoil (ES) | https://ballenoil.es |
| BarterGo (ES) | https://www.barterenergy.es/ |
| be emobil | http://www.be-emobil.de/ |
| Be.EV | https://be-ev.co.uk/ |
| BeCharged | http://www.becharged.eu/ |
| BIGGIE Energie | https://www.bigge-energie.de/ |
| Blink Charging (Europe) | https://blinkcharging.gr/en/ |
| Blink Charging (UK) | https://blinkcharging.co.uk/ |
| Blue Corner (Belgium) | http://www.bluecorner.be/ |
| BLUETORINO | https://www.bluetorino.eu/ricarica |
| BP Pulse (UK) | https://www.bppulse.co.uk/ |
| Brite (IE) | https://brite.ie |
| BS Energie | http://www.bs-energy.de/engagement/umwelt/elektromobilitaet/elektrotankstellen/ |
| BürgerLadenetz (DE) | https://buergerwerke.de/e-mobilitaet/buergerladenetz |
| Carrefour (ES) | https://www.carrefour.es/ |
| CEL TODA Navarra (ES) | https://www.comunidadenergeticalocal.eu/proyectos/toda-navarra/ |
| Certa (IE) | https://certaireland.ie |
| ČEZ | http://www.elektromobilita.cz/ |
| Charge & Drive (Fortum - NO) | http://www.fortum.no/hurtigladere |
| Charge My Ride (Malta) | https://chargemyride.mt/ |
| Charge My Street (UK) | https://chargemystreet.co.uk/ |
| Charge Your Car | http://www.chargeyourcar.org.uk/ |
| Chargecloud (DE) | https://www.chargecloud.de/ |
| ChargeLounge | http://www.chargelounge.de/ |
| ChargeNode (SE) | https://chargenode.eu/ |
| Chargy (LU) | https://chargy.lu/ |
| CitiPark (UK) | https://www.citipark.co.uk |
| Citywatt (DE) | https://citywatt.de |
| CLEVER | https://www.clever.dk/ |
| CNR (Compagnie Nationale du Rhône) | https://www.cnr.tm.fr |
| Cogeser Energia (Italia) | https://www.cogeserenergia.it/mosaic/search/it/mobilita-elettrica |
| Comfortcharge | https://www.comfortcharge.de/ |
| Continente Plug&Charge | https://plugcharge.continente.pt |
| CURB (HU) | https://curb.hu/ |
| Curb (Hungary) | https://ev.curb.hu |
| Dream Energy | https://www.dream-energy.fr/ |
| Drehstromnetz | http://www.drehstromnetz.de |
| Driwe | https://www.driwe.eu/ |
| DV Parking (HU) | https://www.dvparking.hu |
| e-Charge (Romania) | http://e-charge.ro/ |
| E-Charge50 (FR) | https://www.e-charge50.fr/ |
| E-Fusion (RO) | https://incarcare.e-fusion.ro |
| e-Laad | http://www.e-laad.nl |
| E-Mobi | https://e-mobi.hu/ |
| e-Mobilita Brno (cz) | https://www.emobilitabrno.cz/en/chargers |
| E-moving (Italy) | http://www.e-moving.it/ |
| e-totem | http://www.e-totem.fr/ |
| e-Vadea | https://www.e-vadea.fr |
| e-VOLTT | https://e-voltt.nl/ |
| E-Wald | http://shop.e-wald.eu/produkt/e-wald-ladekarte-monatskarte/ |
| E.ON (CZ) | http://www.ekobonus.cz/ekologicka-doprava/elektromobilita |
| E.ON (DE) | https://www.eon.de/de/eonde/pk/produkteUndPreise/E.ON_eMobil/index.htm |
| E.ON (DK) | https://www.eon.dk/privat/strom-til-din-elbil.html |
| E.ON (HU) | https://www.eon.hu/ |
| EAC e-charge | https://www.eac.com.cy/En/CustomerService/eCharge/Pages/default.aspx |
| EAM | http://www.eam.de/ueber-uns/unternehmensprofil/standorte/ |
| Easy4You | http://www.easy-4-you.ch |
| Easypark | https://easypark.fi/ |
| EB Charging | https://ebcharging.co.uk/ |
| eborn | https://www.eborn.fr/ |
| EcoCharge77 | https://ecocharge77.fr |
| Ecoinside (PT) | https://www.ecoinside.pt/ |
| Ecoplug | http://www.ecoplug.be/index.html |
| Ecospazio (Italy) | http://www.ecospazio.it/ |
| Ecotap | http://www.ecotap.nl/ |
| ECPoints | https://ecpoints.es/ |
| EDP MOP | http://www.edp.pt/ |
| eDrop | http://www.edrop.ch/ |
| EE-Mobil | http://www.ee-mobil.de/ |
| EEW Duderstadt | http://www.ewb-duderstadt.de/de/Strom/Ausgezeichnet.html |
| eins | https://www.eins.de/privatkunden/elektromobilitaet/ |
| ejoin | https://www.ejoin.eu/ |
| Ekoen | https://ekoen.pl/ |
| Ekomobil | https://www.ekomobil.it |
| Elbud (PL) | https://www.elbud.com.pl/stacje-ladowania |
| Eldrive Lithuania (LT) | https://www.eldrive.eu |
| Electrico.es | https://electrico.es/ |
| Electro-EMT | https://www.emtmadrid.es/ |
| ElectRoad (UK) | https://www.electroad.uk/ |
| ElectroDrive Salzburg | http://www.electrodrive-salzburg.at |
| ElectroDrive/Mark-E (DE) | http://www.mark-e.de/ |
| Elektro Ljublana | http://www.elektro-ljubljana.si/1/Obnovljivi-viri-energije/Polnilna-mesta-za-elektricna-vozila.aspx |
| Elektro Profi Mobility (HU) |  https://epcharger.hu/ |
| Elektrum Drive | https://www.elektrum.lv/en/for-home/elektrum-drive/public-charging/ |
| Elen | http://elen.hep.hr/ELEN-charging-stations.aspx |
| Elevat/MasterCharge (EU) | https://www.elevat.eu/ |
| Ella | http://ella.at/ |
| ELMO | http://www.elmo.ee/ |
| ELMotion | https://www.elmotion.ro/harta |
| ELMŰ | http://www.e-autozas.hu/ |
| ElpeFuture | https://elpefuture.gr/ |
| Emeo (SK) | https://emeo.sk/verejne_nabijanie |
| Emfree | https://emfree.eu/ |
| emma | http://www.friedrichshafen.de/wirtschaft-verkehr/emma |
| Emobitaly (Italy) | http://www.emobitaly.it/come-ricaricare/ |
| Empora | http://www.empora.eu |
| Enefit (LT) | https://www.enefit.lt/ |
| Enercharge |  https://enercharge.at/ |
| EnerCity | https://www.enercity.de/privatkunden/mobilitaet/e-mobilitaet/formular-reg-e-tanken/index.jsx |
| Energie der Eifel | https://www.ene-eifel.de/privatkunden-strom/e-mobilität |
| Energieversorgung Buching-Trauchgau | http://ebt-halblech.de/ |
| Enerhub | http://www.enerhub.it/ricarica/ |
| Enerstock | https://enerstock.fr/ |
| Enertec (SI) | https://enertec.si/ |
| ENEWA | http://www.enewa.de/ |
| Enovos | http://www.enovos.lu/particuliers/ecomobilite |
| Enspirion | http://www.enspirion.pl/?page_id=728 |
| Entega | https://www.entega.de/ |
| eondrive.ro | https://www.eondrive.ro/ |
| eparking (Finland) | https://en.web.eparking.fi/ |
| ePower | https://www.epower.ie/ |
| ESB Ecars | http://www.esb.ie/electric-cars/index.jsp |
| ESB Energy (UK) | https://www.esbenergy.co.uk/ev |
| Essent (NL) | http://www.essent.nl/content/particulier/producten/elektrisch_rijden/index.html |
| eStation (IE) | https://estation.ie/ |
| Estonteca | http://www.estonteco.eu/live/ |
| ETOP | http://etop.sk/ |
| EV Direct | https://evdirect.hu/ |
| EV Expert | https://charge.evexpert.eu |
| EV-Mag | https://ev-mag.ro/stations/ |
| EV-Point | http://www.ev-point.be |
| EVbility (Italy) | http://www.evbility.eu/ |
| EVCE (PT) | https://www.evce.pt/ |
| evconnect.ro (Romania) | https://evconnect.ro/ |
| EVcore (SE) | https://opigo.se/ev-core |
| Evd Dormagen | http://www.evd-dormagen.de/ |
| eVISO | https://www.eviso.it |
| EVite (ch) | http://www.swiss-emobility.ch/home/evite.html |
| EVL (de) | https://www.evl.de/e-mobilitaet/e-tanken-in-limburg/ |
| Evmapa (CZ) | http://www.evmapa.cz |
| EVN (Bulgaria) | https://evn.bg/SpecialPages/e-mobility.aspx |
| EVnetNL | http://www.evnet.nl/ |
| Evolt Network (Swarco E.Connect) | https://evoltnetwork.co.uk/ |
| EVPass (CH) | https://www.evpass.ch/ |
| EVPower (PT) | https://www.evpower.pt |
| EVS Energieversorgung Sylt | https://www.energieversorgung-sylt.de/mobilitaet/e-mobilitaet/ |
| evyve | https://www.evyve.co.uk/ |
| eways | https://www.eways.se/ |
| EWB | http://www.stadtwerke-bruchsal.de/ |
| EWE | http://www.ewe.de/privatkunden/ewe-stromtankstellen.php |
| EWI  Energiewerke Isernhagen | http://www.ewi-isernhagen.de/energieeffizienz/elektromobilitaet.aspx |
| EWP : Energie und Wasser Potsdam GmbH | https://www.swp-potsdam.de/de/energie/elektromobilit%C3%A4t/ |
| EWR gmbh e-mobile | http://www.ewr-e-mobil.de/ |
| EZ-Charge | https://www.ez-charge.co.uk/ |
| EZO (IE) | https://ezo.ie |
| Factor Energia (PT) | https://factorenergia.pt/ |
| Fenie Energía (Spain) | http://recarga.fenieenergia.es/ |
| Flyelectric (IT) | https://www.flyelectric.it/ |
| ForEV | https://forev.co.uk/for-drivers/ |
| FORTISIS | http://www.fortisis.eu |
| Free To X | https://www.freeto-x.it/ |
| Galactico.pl | http://galactico.pl/ |
| GardaUno | http://www.gardauno.it/la-mappa-dei-punti-di-ricarica/ |
| GGEW | http://www.ggew.de/UN/Elektromobilitaet |
| GO+EAuto | https://gopluseauto.pl/ |
| GoCharge (IE) | https://www.gocharge.ie/charge-with-us/ |
| Green Land Mobility (Italy) | http://greenlandmobility.it/ |
| Greenflux | http://www.greenflux.nl/ |
| Greenway | https://greenway.sk |
| Greenway Polska (PL) | https://greenwaypolska.pl/ |
| Gremo na elektriko (SI) | https://www.gremonaelektriko.si/ |
| Grønn Kontakt | http://gronnkontakt.no/ |
| GTIS Charging (SK) | https://www.gtischarging.sk/ |
| Gyorstöltők | https://gyorstoltok.hu/ |
| Harz Energie | http://www.harzenergie.de/index.cfm?fuseaction=portal.showcontent&viewmode=content&num_obj_id=30312&language=de&menu=30305&rootmenu=25325&page=30306&bp=30306 |
| Helexia (PT) | https://helexia.pt/ |
| Helsingin Energia | http://www.helen.fi/ |
| HeraRicarica Pubblica | https://heracomm.gruppohera.it/casa/mobilita-sostenibile/ricarica-pubblica |
| Holtoltsek.hu | https://www.holtoltsek.hu/ |
| Hotoltsek (HU) | https://www.holtoltsek.hu/ |
| Hrvatski Telekom | https://www.hrvatskitelekom.hr/ |
| Hubsta | http://www.hubsta.co.uk/en/home/ |
| Ignitis On | https://ignitison.lt/ |
| Ignitis UAB (LT) | https://ignitison.lt/ |
| inChaNet | http://inchanet.cz/products_en.html#fast_charging_station |
| InstaVolt Ltd | http://instavolt.co.uk/ |
| Interparking (ES) | https://www.interparking.es/ |
| iPlanet (IT) | https://iplanet.eu |
| Jersey Electricity Evolve | https://www.jec.co.uk/your-home/electric-vehicles/charging-your-ev/ |
| Jet Charge (UK) | https://www.jetlocal.co.uk/jet-charge/ |
| Joju Ltd | https://www.jojusolar.co.uk/ |
| Justplugin (NL) | https://justplugin.nl/ |
| K Lataus | https://k-lataus.fi |
| Kaufland eCharge | https://filiale.kaufland.de/service/e-ladestationen.html |
| Kelag AG | https://www.kelag.at/privat/kelag-autostrom-111.htm |
| KiWhi Pass | http://www.kiwhipass.fr/ |
| Kople | https://www.kople.no/veiledning/ladepris |
| KRB Intra (cz) | https://kpb.power2ride.cz/ |
| Lad Opp | https://ladopp.no/ |
| Ladefoxx | https://ladefoxx.de/ |
| ladenetz.de | http://ladenetz.de |
| LAKD (LT) | https://lakd.lt/ |
| Latvenergo (LV) | http://www.latvenergo.lv |
| Leap24 (NL) | https://leap24.eu/ |
| Level2.ee | http://www.level2.ee/ |
| LSW Energie | http://www.elektrofahrzeuge.lsw.de/ |
| Mainova | https://www.mainova.de/privatkunden/mobilitaet/stromtankstellen.html |
| Maxol | https://www.maxol.ie/ |
| MELIB (ES) | http://www.caib.es/sites/energiaicanviclimatic/ca/mobilitat_elactrica_a_les_illes_balears_melib/ |
| Métropolis | https://www.metropolis-recharge.fr/ |
| Migrol (CH) | https://www.migrol.ch |
| Mobib (Belgium) | http://www.mobib.be/index.htm |
| Mobie.pt | http://www.mobie.pt |
| Mobiliti.hu | https://mobiliti.hu/emobilitas |
| MobilityPlus | https://www.mobilityplus.be/en |
| Mobilize (FR) | https://www.mobilize.fr/ |
| MObiVE | https://www.mobive.fr/ |
| Moeve (ES) | https://www.moeve.es/ |
| MOL | https://molplugee.si/en |
| Moon Power | https://www.moon-power.pt/ |
| Motionbox | https://www.motionbox.gr |
| Motolataus | https://www.motonet.fi/fi/sivut/motolataus/ |
| MOVE (CH) | https://www.move.ch |
| München Umland | https://www.bayernwerk.de/cps/rde/xchg/bayernwerk/hs.xsl/278.htm |
| MVM Partner Zrt. | https://www.mvmpartner.hu/ |
| N-ERGIE | https://www.n-ergie.de/header/die-n-ergie/aktiv-fuer-die-umwelt/elektromobilitaet.html |
| Naturenergie (DE) | https://www.naturenergie.de/e-mobil/oeffentliche-ladeinfrastruktur |
| Naturgy (ES) | https://www.naturgy.es/ |
| Neste Lataus | https://neste.fi/ |
| Next Step Mobility (DE) | https://nextstepmobility.de/ |
| Nissan (ES) Dealer Network | https://www.nissan.es/ |
| Nissan DE Freistrom | http://www.nissan.de/DE/de/vehicle/electric-vehicles/leaf/charging-and-battery/freistrom-info.html |
| Nissan UK Dealer Network | http://www.nissan.co.uk |
| NKM mobiliti | http://www.mobiliti.hu/ |
| Nomadpower | http://www.nomadpower.eu |
| NRG4YOU | https://customer.restation.eu/stations |
| NRGincharge | https://www.nrgincharge.gr/en |
| OK (DK) | https://www.ok.dk/privat/produkter/opladning/ude/ladestander-priser |
| OKQ8 (SE) | https://www.okq8.se/ |
| Optimile | https://optimile.eu/ |
| Optimum Way | https://optimumway.hu/ |
| Orlencharge | https://orlencharge.orlen.pl/ |
| Osprey Charging | https://ospreycharging.co.uk/ev-drivers/ |
| Osterholzer Stadtwerke | https://www.osterholzer-stadtwerke.de/service/fahren-mit-strom-erdgas/strom/ |
| Ouest Charge | https://ouestcharge.fr |
| OVAG Energie | http://www.ovag-energie.de/oe/ovag-energie.nsf/c/Umwelt,E-Mobilit%C3%A4t |
| Park & Charge (CH) | http://www.park-charge.ch/ |
| Park & Charge (D) | http://www.park-charge.de/ |
| Parking Energy | https://www.parkkisahko.fi/ |
| pass pass électrique | https://passpasselectrique.fr |
| PCharge | https://www.pcharge.com.cy/ |
| Petite Borne (FR) | https://www.petiteborne.fr/ |
| PETROL | http://www.petrol.eu/road/car/electrical-mobility-petrol |
| Pfalzwerke | http://www.pfalzwerke.de/ |
| PGE Nowa Energia | https://pgene.pl/ |
| Pinergy (IE) | https://pinergy.ie/power-up |
| PKP Mobility | https://pkp.pl/pkpmobility |
| Plenergy (ES) | https://plenergy.es/ |
| Plenitude On The Road (EU) | https://eniplenitude.eu/ |
| Plug Charging |  https://plugcharging.co.uk |
| Plugged In Midlands (UK) | http://www.pluggedinmidlands.co.uk |
| plugpoint.ro | https://www.plugpoint.ro/ |
| Polenergia eMobility | https://polenergia-emobility.pl |
| Polyfazer (CZ) | http://polyfazer.cz/ |
| Polyfazer (RO) | https://polyfazer.ro/map/ |
| Power Dot | https://powerdot.fr/ |
| Power Station (BE) | http://power-station.be/power-card-laadpas/ |
| PowerDot (Es) | https://powerdot.es/ |
| Powered by E.ON Drive & Clever | https://poweredby.dk/ |
| Powerland (BE) | https://www.powerland.be/ |
| PRE (cz) | https://www.pre.cz/cs/profil-spolecnosti/dalsi-aktivity-pre/premobilita/ |
| Project EV | https://www.projectev.co.uk/ |
| Protergia Charge | https://protergiacharge.gr/ |
| Q1 Autostrom | https://www.q1.eu/ |
| Qwello | https://qwello.uk/en |
| R3 | https://www.r3-charge.fr/ |
| Rauman Energia | http://www.raumanenergia.fi/yritys/fi_FI/sahkoautoilu/ |
| Renault | http://www.renault.fr/ |
| Renewing (PT) | https://renewing.pt/ |
| REWAG | https://www.rewag.de/privatkunden/strom/rewariostrommobil.html |
| RhönEnergie | https://re-fd.de/ |
| Rompetrol | https://rompetrol.ro |
| RPSnet | http://rpsnet.cz |
| RWE Mobility/Essent | http://www.essent.nl |
| Sähköinenliikenne (fi) | http://www.sahkoinenliikenne.fi/ |
| Saiel (IT) | http://www.saiel.it/it_IT/rep/divisione_energia/mobilita_elettrica |
| Salzburg AG | https://www.salzburg-ag.at/e-mobilitaet/elektromobilitaet.html |
| Schleswiger Stadtwerke | https://www.schleswiger-stadtwerke.de/content/unternehmen/emobilitaet/ |
| Schnell Laden Berlin | http://www.schnell-laden-berlin.de |
| Schwabencard | https://www.swu.de/privatkunden/energie-wasser/elektromobilitaet/kunde-werden.html |
| Scottish Power | https://www.scottishpower.co.uk |
| SDE76 (FR) | http://www.sde76.fr/ |
| SDEG 16 (Syndicat Départemental d'Electricité et de Gaz de la Charente) | https://sdeg16.fr/nos-competences/les-bornes-vehicules-electriques/ |
| SDET (FR) | https://www.te81.fr/transition-energetique/bornes-de-recharge-pour-vehicules-electriques/ |
| SDEY (Fr) | http://sdey.fr/nos-missions/mobilite-electrique/ |
| Seom | https://www.seom.se/ |
| Sigeif (FR) | https://www.sigeif.fr/ |
| Silfi (Italy) | http://www.silfi.it/IT/index.php?id=63&label=Mappa%20colonnine%20ricarica%20veicoli%20elettrici%22 |
| Silverstone Green Energy | https://www.silverstonegreenenergy.co.uk/ |
| Slovenské elektrárne | https://sepredaj.seas.sk/elektricka-mobilita |
| Smart Charge by Sainsbury's (UK) | https://smartcharge.co.uk/ |
| SODO | https://en.sodo.si/fast-charging-stations/list-of-active-fast-charging-stations-on-slovenian-highways |
| sofos | http://www.sofos.es/productos-y-servicios/estaciones-de-recarga/ |
| Sperto (DK) | https://www.sperto.dk/ |
| Städtische Werke Magdeburg | http://www.sw-magdeburg.de/ |
| Stadtwerke Clausthal-Zellerfeld | http://www.stadtwerke-clausthal.de/ |
| Stadtwerke Dessau | https://www.dvv-dessau.de/ |
| Stadtwerke Düsseldorf AG | https://www.swd-ag.de |
| Stadtwerke Elmshorn | http://www.stadtwerke-elmshorn.de/cms/Dienstleistungen/E-Mobilitaet/Stadtwerke-investieren-in-E-Mobilitaet-.html |
| Stadtwerke Göttingen | http://www.swgoe.de/ |
| Stadtwerke Grevesmühlen | http://www.stadtwerke-gvm.de/ |
| Stadtwerke Haldensleben  SWH | http://www.swhdl.de/strom/e_mobility/ |
| Stadtwerke Halle | http://www.evh.de/EVH/Privatkunden/Natuerlich-EVH/Mit-Strom-fahren/ |
| Stadtwerke Hameln | https://www.stadtwerke-hameln.de/service/beratung.html |
| Stadtwerke Leipzig SWL | http://www.swl.de/web/swl/DE/Unternehmen/Elektromobilitaet/Elektromobilitaet.htm |
| Stadtwerke Lübeck | http://www.swhl.de/e-mobilitaet/ |
| Stadtwerke Münster | https://www.stadtwerke-muenster.de/privatkunden/strom/alle-stromprodukte/e-mobilitaet/uebersicht.html |
| Stadtwerke Neumünster (SWN). | http://www.stadtwerke-neumuenster.de/wDeutsch/privatkunden/unser-einsatz-fuer-die-umwelt/e-mobilitaet.php?navid=172 |
| Stadtwerke Northeim  SWN | http://www.stadtwerke-northeim.de/site/de/580/stromtankstelle.html |
| Stadtwerke Rinteln | http://www.stadtwerke-rinteln.de/ |
| Stadtwerke Rostock | http://www.swrag.de/ |
| Stadtwerke Soest | http://www.stadtwerke-soest.de/index.php?id=280 |
| Stadtwerke Strahlsund | http://www.stadtwerke-stralsund.de/energie/elektrofahrzeuge/elektrofahrzeuge/foerderung.php |
| Stadtwerke Uetersen | http://www.stadtwerke-uetersen.de/?page_id=1722 |
| Stadtwerke Verden | http://www.stadtwerke-verden.de/privatkunden/energiedienstleistungen/elektromobilitaet.html |
| Stadtwerke Wernigerode | http://www.stadtwerke-wernigerode.de/published_page.aspx?id=132 |
| Stadtwerke Wittenberg | http://stadtwerke.wittenberg.de/ |
| Stadtwerke Wolfenbüttel | http://www.stadtwerke-wf.de/ |
| Stop N Top (IE) | https://www.stopntop.ie |
| StromTicket | http://stromticket.de/ |
| Stromtreter | http://www.stromtreter.de/ |
| SUN Stadtwerke Union Nordhessen | http://www.sun-stadtwerke.de/energien-der-zukunft/e-mobilitaet/ladekarte.html |
| Sunenergy (PT) | https://www.sunenergy.pt/ |
| SureCharge (FM Conway) (UK) | https://www.fmconway.co.uk/our-services/surecharge/ |
| SWB / EWE | https://www.swb-gruppe.de/verantwortung/swb-und-umwelt/fahren-mit-strom.php |
| Swisscharge (CH) | https://swisscharge.ch/ |
| Sydego | https://www.sydego.fr/ |
| Tank & Rast | http://tank.rast.de/emobility/ |
| TANKE Wien Energie | http://www.tanke-wienenergie.at/ |
| TAURON Nowe Technologie | https://www.tauron.pl/tauron/tauron-innowacje/elektromobilnosc |
| TEA. | https://www.teapont.hu/ |
| The GeniePoint Network ( EQUANS EV Solutions ) | https://www.geniepoint.co.uk/ |
| Time Park (NO) | https://timepark.no/finn-ladepunkt/ |
| TIWAG Tiroler Wasserkraft AG (AT) | https://www.tiwag.at/no_cache/privatkunden/energieeffizienz/mobilitaet/ |
| Trenčiansky samosprávny kraj | https://www.tsk.sk/ |
| UEnergia (ES) | https://www.uenergia.es/es/puntos-de-recarga-para-vehiculos-electricos/ |
| Ultra-Fast | http://www.ultra-fast.nl/ |
| Umeå Energi | http://www.umeaenergi.se/ |
| Unipark [Stova] (LT) | https://unipark.lt/en/ |
| USEDA (FR) | http://www.useda.fr/ |
| UWAG | http://www.uewag.de/energie/stromtankstellen |
| Vattenfall InCharge | http://www.vattenfall.de/de/emobility/emobility.htm |
| Veolia | https://www.veolia.cz |
| Versorgungsbetriebe Hann. Münden | http://www.versorgungsbetriebe.de/hannmuendenGips/Gips?SessionMandant=HannMuenden&Anwendung=CMSWebpage&Methode=ShowHTMLAusgabe&RessourceID=12045 |
| Vibrate | http://www.emobility-vibrate.eu/ |
| Vilalta Greenergy (ES) | https://www.vilaltacorp.es/es/electricidad |
| VIRTA | http://virta.fi/ |
| Vlotte | http://www.vlotte.at/ |
| Volthero | https://volthero.hu/ |
| Voltrelli (RO) | https://evconnect.ro/ |
| VR Schneller-Strom-tanken | http://www.schneller-strom-tanken.de/ |
| VSE | http://www.zelenabuducnost.sk/ |
| We Drive Solar | https://www.wedrivesolar.nl/ |
| Weev | https://weev.ie/ |
| Werraenergie | http://www.werraenergie.de/www/werraenergie/webinfo/webinfo.nsf/DocsID/E-Mobilitaet |
| Wirtschaftsbetriebe Stadt Nienburg | http://www.wirtschaftsbetriebe-nienburg.de/ |
| ZE-MO (Be) | http://www.ze-mo.be/ |
| ZEAG Energie | http://www.zeag-energie.de |
| ZEN (Zero Emission Network)/PROVIRIDIS | https://z-e-n.fr/ |
| ZenCar | https://www.zencar.eu/ |
| Zepto | https://zepto.pl/ |
| ZSE Drive | https://zsedrive.sk/ |
