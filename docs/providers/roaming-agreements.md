# RoamingPartnerships — acuerdos de roaming entre operadores

Tabla estática (CLAUDE.md sección 5) que alimenta `roamingProviderIds` de
cada `Charger`: para un cargador con operador nativo `X`, estas son las
apps de terceros que también dan acceso a él. **No confundir con
`ProviderDirectory`** (que solo mapea app → datos de lanzamiento): esta
tabla decide qué apps son candidatas para un cargador dado.

**Actualización tras documentar las 13 redes del MVP:** Ionity y Fastned
(2 de las 3 confirmadas) sí tienen roaming verificado — con Plugsurfing —,
detectado al documentar la ficha de Plugsurfing (`docs/providers/plugsurfing.md`).
Tesla sigue sin ningún acuerdo de roaming detectado.

**Nota arquitectónica importante:** Chargemap y Plugsurfing (2 de las 10
redes "pendientes") no son operadores nativos de cargadores propios, sino
agregadores de roaming — ver `docs/providers/chargemap.md` y
`docs/providers/plugsurfing.md`. Es decir, la fila de la tabla debería
leerse como "casi cualquier operador nativo puede tener a Plugsurfing y/o
Chargemap como candidato de roaming", no solo los 3 casos ya confirmados
abajo — falta investigar sistemáticamente cuáles de las redes nativas
tienen acuerdo con estos dos agregadores en concreto.

| Operador nativo | Apps de roaming con acceso confirmado | Fuente |
|---|---|---|
| Zunder | Repsol Waylet (>1.200 puntos operables desde Waylet: localizar, iniciar/parar, pagar) | [Híbridos y Eléctricos](https://www.hibridosyelectricos.com/coches/mas-1200-puntos-carga-rapida-traves-su-app-repsol-zunder-firman-importante-acuerdo-en-espana_81116_102.html) |
| Endesa X | Electromaps (>6.200 puntos incorporados a la app de Electromaps) | [Endesa — sala de prensa](https://www.endesa.com/es/prensa/sala-de-prensa/noticias/transicion-energetica/movilidad-electrica/acuerdo-electromaps-acceso-carga-vehiculos-electricos) |
| Eranovum | Accesible desde la app de Zunder (Zunder App/eZCard/eZTag), no al revés | [Zunder — cómo cargar en Eranovum](https://www.zunder.com/cargar-red-de-eranovum-con-zunder/) |
| Ionity | Plugsurfing (citado explícitamente en la ficha oficial de la app) | [App Store — Plugsurfing](https://apps.apple.com/us/app/plugsurfing-ev-charging/id793188906) |
| Fastned | Plugsurfing (citado explícitamente en la ficha oficial de la app) | [App Store — Plugsurfing](https://apps.apple.com/us/app/plugsurfing-ev-charging/id793188906) |
| EnBW | Plugsurfing (citado explícitamente en la ficha oficial de la app) | [App Store — Plugsurfing](https://apps.apple.com/us/app/plugsurfing-ev-charging/id793188906) |

**Notas para `researcher-android`/`researcher-ios`:**
- No dar por hecho que la relación es simétrica: que Waylet dé acceso a
  cargadores Zunder no implica que la app de Zunder dé acceso a nada de
  Repsol/Waylet — cada fila es unidireccional (app de roaming → red nativa).
- Antes de añadir cualquier entrada nueva, exigir una fuente pública
  (comunicado del operador, nota de prensa) — no inferir acuerdos de
  roaming solo porque dos apps listan el mismo punto en un dataset como
  Open Charge Map, ya que eso puede deberse a otras causas (duplicados en
  el dataset, cobertura genérica tipo "muestra otros operadores" sin dar
  acceso real — ver el caso de Fastned en `docs/providers/fastned.md`).
- Pendiente: revisar si Ionity y Fastned tienen roaming adicional más allá
  de Plugsurfing, y seguir intentando confirmar alguno para Tesla (ninguno
  detectado hasta ahora). Pendiente también investigar sistemáticamente el
  alcance real de Chargemap y Plugsurfing sobre el resto de las 13 redes
  del MVP (Allego, EnBW ya confirmado, Shell Recharge, TotalEnergies,
  Iberdrola, Wenea), no solo confiar en menciones puntuales en tiendas de
  apps.
