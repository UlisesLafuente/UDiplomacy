# Adyacencias costeras en variantes de Diplomacy

Cómo funcionan las costas, las adyacencias marítimas y terrestres,
y cómo se reflejan en el JSON de variante de UDiplomacy.

---

## Índice

1. [Conceptos básicos](#1-conceptos-básicos)
2. [Provincias de una sola costa](#2-provincias-de-una-sola-costa)
3. [Provincias con múltiples costas](#3-provincias-con-múltiples-costas)
4. [El sufijo de costa en adyacencias](#4-el-sufijo-de-costa-en-adyacencias)
5. [Bidireccionalidad](#5-bidireccionalidad)
6. [Cómo valida el backend](#6-cómo-valida-el-backend)
7. [shareSeaNeighbor: flotas entre costeras sin mar directo](#7-shareseaneighbor-flotas-entre-costeras-sin-mar-directo)
8. [Diagrama de decisión](#8-diagrama-de-decisión)
9. [Ejemplos completos](#9-ejemplos-completos)
10. [Convenciones](#10-convenciones)

---

## 1. Conceptos básicos

Cada provincia tiene uno de estos tipos (`ProvinceType`):

| Tipo | ¿Puede tener unidades? | ¿Puede tener costa? |
|------|------------------------|---------------------|
| `SEA` | Solo flotas | No (es agua) |
| `COASTAL` | Flotas y ejércitos | Sí |
| `INLAND` | Solo ejércitos | No |

Una **costa** es una sección nombrada de una provincia costera que permite
conexiones marítimas diferenciadas. La mayoría de provincias tienen una
sola costa (y por tanto no necesitan nombrarla). Solo unas pocas tienen
dos costas separadas geográficamente (ej. España con costa norte y sur).

En el JSON de provincia, las costas se declaran con el campo `coasts`:

```json
{
  "name": "SPA",
  "type": "COASTAL",
  "coasts": ["north", "south"]
}
```

Si una provincia tiene una sola costa o ninguna, `coasts` se omite o
se pone a `null`:

```json
{
  "name": "BEL",
  "type": "COASTAL",
  "coasts": null
}
```

---

## 2. Provincias de una sola costa

Son la mayoría. Aunque tengan salida al mar, solo tienen una línea de
costa sin nombre. Todas sus adyacencias (terrestres y marítimas) se
marcan con `null` como valor de costa.

**Ejemplo: Bélgica (BEL)**

```json
{
  "name": "BEL",
  "type": "COASTAL",
  "coasts": null,
  "adjacencies": {
    "HOL": null,
    "RUH": null,
    "BUR": null,
    "PIC": null,
    "ENG": null,
    "NTH": null
  }
}
```

- `"ENG": null` — un mar; la flota en BEL puede ir a ENG sin especificar costa
- `"HOL": null` — provincia costera; la flota puede ir HOL vía mar y el ejército por tierra
- `"BUR": null` — provincia INLAND; solo el ejército puede ir (la flota no entra a INLAND)

Para una flota en BEL, todas las conexiones marítimas están disponibles
porque no hay múltiples costas que restrinjan.

---

## 3. Provincias con múltiples costas

Son provincias cuya geografía tiene dos litorales separados, cada uno
dando a masas de agua distintas. En el mapa clásico hay cuatro:

| Provincia | Costas | Se conecta con |
|-----------|--------|----------------|
| SPA (España) | `["north", "south"]` | MID por norte, WES/LYO por sur |
| BUL (Bulgaria) | `["east", "south"]` | BLA/CON por este, AEG por sur |
| STP (San Petersburgo) | `["north", "south"]` | BAR por norte, GOB/FIN por sur |
| EDI (Edimburgo) | `["north", "south"]` | NWG por norte, NTH por sur |

El array `coasts` nombra las costas. Estos nombres se usan luego en el mapa de `adjacencies` para qué costa concreta da acceso a cada vecino.

**Importante:** una provincia con costas nombradas tiene restricciones
de movimiento para las flotas. Una flota en SPA/*south* **no** puede
moverse a MID (que solo es accesible desde la costa norte). La flota
tiene que especificar en qué costa está al dar la orden:
`F SPA/south - WES`.

---

## 4. El sufijo de costa en adyacencias

El mapa `adjacencies` de cada provincia asigna a cada vecino un valor
que puede ser:

| Valor | Significado |
|-------|-------------|
| `null` | **Sin restricción de costa.** Cualquier unidad (ejército o flota) puede usar esta adyacencia, independientemente de la costa. |
| `"north"`, `"south"`, `"east"`, `"west"` | **Solo accesible por esa costa concreta.** Una flota solo puede usar esta ruta si está posicionada en la costa indicada. |

**El sufijo solo se pone en la provincia que tiene costas nombradas.**
La provincia vecina (mar o costera sin costas nombradas) siempre usa
`null` para la misma conexión.

### Ejemplo: España (SPA) con dos costas

```json
{
  "name": "SPA",
  "type": "COASTAL",
  "coasts": ["north", "south"],
  "adjacencies": {
    "MID": "north",
    "WES": "south",
    "LYO": "south",
    "GAS": null,
    "POR": null,
    "MAR": null
  }
}
```

- `"MID": "north"` — solo una flota en la costa **norte** de SPA puede ir a MID
- `"WES": "south"` — solo una flota en la costa **sur** de SPA puede ir a WES
- `"LYO": "south"` — solo la costa sur puede ir a LYO
- `"GAS": null` — cualquier unidad (ejército o flota en cualquier costa) puede ir a GAS
- `"POR": null` — cualquier unidad puede ir a POR

Y en el lado del mar (sin costas nombradas):

```json
{
  "name": "MID",
  "type": "SEA",
  "coasts": null,
  "adjacencies": {
    "SPA": null,
    "POR": null,
    "GAS": null,
    "BRE": null,
    "ENG": null,
    "IRI": null,
    "NAO": null
  }
}
```

```json
{
  "name": "WES",
  "type": "SEA",
  "coasts": null,
  "adjacencies": {
    "SPA": null,
    "POR": null,
    "LYO": null,
    "TYS": null,
    "NAO": null,
    "WMED": null
  }
}
```

### Ejemplo: Bulgaria (BUL) con este y sur

```json
{
  "name": "BUL",
  "type": "COASTAL",
  "coasts": ["east", "south"],
  "adjacencies": {
    "BLA": "east",
    "CON": "east",
    "AEG": "south",
    "SER": null,
    "RUM": null,
    "GRE": null
  }
}
```

- `"BLA": "east"` — solo desde la costa este se navega al Mar Negro
- `"CON": "east"` — Constantinopla solo accesible desde la costa este
- `"AEG": "south"` — el Egeo solo desde la costa sur
- `"SER": null", "RUM": null, "GRE": null` — adyacencias terrestres, cualquier unidad

### Casos frontera dentro del JSON

- **Mar a provincia multicosta:** siempre `null` (el mar no tiene costas)
- **Provincia multicosta a INLAND:** siempre `null` (es terrestre)
- **Dos provincias multicosta vecinas:** cada una marca la costa correspondiente,
  pero esto es rarísimo en la práctica

---

## 5. Bidireccionalidad

Las adyacencias en Diplomacy son **siempre bidireccionales**. Si A es
adyacente a B, entonces B es adyacente a A. Ambas direcciones deben
aparecer en el JSON.

Si SPA tiene `"MID": "north"`, MID debe tener `"SPA": null`. No se
puede tener una dirección sin la otra, o el motor de juego la tratará
como una adyacencia unidireccional (que no existe en Diplomacy).

---

## 6. Cómo valida el backend

El motor de juego valida los movimientos de flotas en
`ConflictResolver.findInvalidMoves()` (líneas 202-218).

Para cada orden `F origen - destino`, obtiene los valores de costa
de ambos lados:

```java
Coast srcCoast = srcProv.adjacencies().get(target);
Coast tgtCoast = tgtProv.adjacencies().get(source);
```

Luego aplica esta lógica:

### 6.1 Si algún lado tiene costa no nula

Por ejemplo, `SPA` → `WES` con `srcCoast = "south"`, `tgtCoast = null`.

La ruta va por una costa específica → **válida para la flota**.
Si la provincia origen tiene costas nombradas, la flota debe estar
en esa costa concreta (quien da la orden especifica `SPA/south`).

### 6.2 Si ambos lados son `null`

Entramos en la rama restrictiva:

```
srcCoast == null && tgtCoast == null
├── ¿Uno de los dos es SEA?
│   └── Sí → VÁLIDO (ruta marítima de mar a costa, o mar a mar)
├── ¿Alguna provincia tiene costas nombradas? (coasts no vacío)
│   └── Sí → INVÁLIDO (es ruta terrestre; si tuviera costas no sería null)
├── ¿Comparten un vecino marítimo? (shareSeaNeighbor)
│   └── Sí → VÁLIDO (flota navega vía ese mar común)
└── ── No → INVÁLIDO (ruta terrestre sin paso marítimo)
```

#### Caso 1: ambos null y uno es SEA

```json
{ "name": "BEL", "adjacencies": { "ENG": null } }
{ "name": "ENG", "adjacencies": { "BEL": null } }
```

BEL es COASTAL, ENG es SEA → VÁLIDO. Una flota en BEL puede ir a ENG.

#### Caso 2: ambos null y alguno tiene costas nombradas

```json
{ "name": "SPA", "coasts": ["north","south"], "adjacencies": { "GAS": null } }
{ "name": "GAS", "adjacencies": { "SPA": null } }
```

SPA tiene costas nombradas, pero la adyacencia es `null` en ambos lados.
El motor interpreta que es una conexión **terrestre** (no marítima).
Una flota **no** puede ir de SPA a GAS por mar (tendría que ir un ejército).
Para que una flota pasara, tendría que especificar una costa:
`"GAS": "north"` — indicando que desde la costa norte de SPA se puede
navegar a GAS.

#### Caso 3: ambos null, sin costas nombradas, comparten mar

```json
{ "name": "LON", "adjacencies": { "WAL": null } }
{ "name": "WAL", "adjacencies": { "LON": null } }
```

LON y WAL son COASTAL sin costas nombradas. Ambos son adyacentes a ENG
(English Channel). `shareSeaNeighbor()` encuentra que ENG es vecino
común → VÁLIDO. Una flota en LON puede navegar a WAL vía ENG, o una
en WAL a LON.

#### Caso 4: ambos null, sin costas nombradas, sin mar compartido

```json
{ "name": "WAL", "adjacencies": { "YOR": null } }
{ "name": "YOR", "adjacencies": { "WAL": null } }
```

WAL da a ENG e IRI. YOR da a NTH. No comparten mar.
`shareSeaNeighbor()` devuelve `false` → INVÁLIDO.
Una flota **no** puede ir directamente de WAL a YOR.
Tendría que rodear: `WAL → ENG → NTH → YOR`.

---

## 7. shareSeaNeighbor: flotas entre costeras sin mar directo

`shareSeaNeighbor()` (líneas 488-498 de `ConflictResolver.java`)
comprueba si dos provincias comparten al menos un vecino de tipo `SEA`:

```
¿Existe una provincia M de tipo SEA tal que M sea adyacente a A y a B?
```

Ejemplo: LON y WAL comparten ENG.

Esta función es el **último filtro** para determinar si una ruta entre
dos provincias costeras (ambas con `null` en la adyacencia y sin costas
nombradas) es navegable para una flota.

| Provincias | Vecinos marítimos | ¿Comparten mar? | ¿Flota pasa? |
|------------|-------------------|-----------------|--------------|
| LON ↔ WAL | LON→ENG, WAL→ENG | Sí (ENG) | Sí |
| WAL ↔ YOR | WAL→ENG,IRI; YOR→NTH | No | **No** |
| CON ↔ BUL | CON→BLA; BUL→BLA,AEG | Sí (BLA) | Sí |
| EDI ↔ NWG | EDI→NTH,NWG; NWG→EDI | Sí (NWG) | Sí |

### Implicación para el diseño de mapas

Si quieres que dos provincias costeras estén conectadas para flotas
pero **no** compartan mar directamente, puedes añadir una zona marítima
intermedia que ambas toquen. O simplemente hacer que no compartan mar
y la conexión será solo para ejércitos.

---

## 8. Diagrama de decisión

Para saber qué valor poner en cada adyacencia del JSON:

```
Para cada par (provincia_origen → provincia_destino):

¿La provincia_origen tiene costas nombradas? (coasts no vacío)
├── No → el valor es null
└── Sí:
    ├── ¿La conexión es terrestre? (ambas INLAND/COASTAL sin mar de por medio)
    │   └── Sí → null
    ├── ¿La conexión es con un mar?
    │   └── Sí → el nombre de la costa ("north", "south", "east", "west")
    └── ¿La conexión es con otra costera pero solo navegable por una costa?
        └── Sí → el nombre de la costa
```

**Regla práctica:** si una provincia tiene `coasts: ["algo"]`, todo lo
que sea marítimo y solo accesible por una costa concreta lleva el nombre
de esa costa. Todo lo demás (terrestre, o accesible por cualquier costa)
lleva `null`.

---

## 9. Ejemplos completos

### 9.1 SPA — dos costas bien diferenciadas

```json
{
  "name": "SPA",
  "type": "COASTAL",
  "coasts": ["north", "south"],
  "adjacencies": {
    "GAS": null,
    "POR": null,
    "MAR": null,
    "MID": "north",
    "WES": "south",
    "LYO": "south"
  }
}
```

| Vecino | Valor | Motivo |
|--------|-------|--------|
| GAS | `null` | Terrestre (ambas INLAND) o conexión sin restricción |
| POR | `null` | Costera sin costas nombradas; vale cualquier costa |
| MAR | `null` | Costera sin costas nombradas; vale cualquier costa |
| MID | `"north"` | Mar solo accesible por la costa norte de SPA |
| WES | `"south"` | Mar solo accesible por la costa sur de SPA |
| LYO | `"south"` | Mar solo accesible por la costa sur de SPA |

### 9.2 BUL — dos costas (este y sur)

```json
{
  "name": "BUL",
  "type": "COASTAL",
  "coasts": ["east", "south"],
  "adjacencies": {
    "SER": null,
    "RUM": null,
    "GRE": null,
    "BLA": "east",
    "CON": "east",
    "AEG": "south"
  }
}
```

| Vecino | Valor | Motivo |
|--------|-------|--------|
| SER | `null` | INLAND, terrestre |
| RUM | `null` | Costera, terrestre |
| GRE | `null` | Costera sin costas nombradas, terrestre/marítima sin restricción |
| BLA | `"east"` | Mar Negro solo por costa este |
| CON | `"east"` | Constantinopla solo por costa este |
| AEG | `"south"` | Egeo solo por costa sur |

### 9.3 STP — dos costas (norte y sur)

```json
{
  "name": "STP",
  "type": "COASTAL",
  "coasts": ["north", "south"],
  "adjacencies": {
    "MOS": null,
    "FIN": null,
    "LVN": null,
    "BAR": "north",
    "NWT": null,
    "SIB": null,
    "GOB": "south"
  }
}
```

| Vecino | Valor | Motivo |
|--------|-------|--------|
| MOS | `null` | INLAND, terrestre |
| FIN | `null` | Costera sin costas nombradas |
| LVN | `null` | Costera sin costas nombradas |
| NWT | `null` | Ártico canadiense, costera sin costas nombradas |
| SIB | `null` | INLAND, terrestre |
| BAR | `"north"` | Mar de Barents solo por costa norte |
| GOB | `"south"` | Gulf of Bothnia solo por costa sur |

### 9.4 EDI — dos costas (norte y sur)

```json
{
  "name": "EDI",
  "type": "COASTAL",
  "coasts": ["north", "south"],
  "adjacencies": {
    "CLY": null,
    "LVP": null,
    "NWG": "north",
    "NTH": "south"
  }
}
```

| Vecino | Valor | Motivo |
|--------|-------|--------|
| CLY | `null` | Terrestre (INLAND) |
| LVP | `null` | Costera sin costas nombradas |
| NWG | `"north"` | Norwegian Sea solo por costa norte |
| NTH | `"south"` | North Sea solo por costa sur |

### 9.5 BEL — una sola costa

```json
{
  "name": "BEL",
  "type": "COASTAL",
  "coasts": null,
  "adjacencies": {
    "HOL": null,
    "RUH": null,
    "BUR": null,
    "PIC": null,
    "ENG": null,
    "NTH": null
  }
}
```

Todo `null`. No hay ambigüedad de costa porque solo tiene una.

### 9.6 WAL y YOR — adyacentes pero sin ruta de flota directa

```json
{
  "name": "WAL",
  "type": "COASTAL",
  "coasts": null,
  "adjacencies": {
    "YOR": null,
    "LON": null,
    "LVP": null,
    "ENG": null,
    "IRI": null
  }
}
```

```json
{
  "name": "YOR",
  "type": "COASTAL",
  "coasts": null,
  "adjacencies": {
    "WAL": null,
    "LON": null,
    "NTH": null
  }
}
```

Aunque ponga `null` en ambos lados, el motor detecta que WAL y YOR
**no comparten ningún mar** (WAL da a ENG/IRI, YOR da a NTH), y por
tanto una flota no puede usar esa adyacencia directamente.

Un ejército sí puede, porque las adyacencias terrestres no tienen
restricción de `shareSeaNeighbor`.

---

## 10. Convenciones

### Nombres de costa

Usar siempre minúscula y uno de estos cuatro valores:

| Nombre | Uso típico |
|--------|------------|
| `"north"` | Costa que da al norte |
| `"south"` | Costa que da al sur |
| `"east"` | Costa que da al este |
| `"west"` | Costa que da al oeste |

No usar nombres inventados como `"mediterranean"` o `"atlantic"`.
El back-end los trata como strings, pero por convención del mapa
clásico se usan puntos cardinales.

### Provincias del mapa clásico con costas múltiples

| Provincia | Costas |
|-----------|--------|
| SPA | `["north", "south"]` |
| BUL | `["east", "south"]` |
| STP | `["north", "south"]` |
| EDI | `["north", "south"]` |

### Validación rápida

Después de generar el JSON, comprobar:

1. Toda adyacencia es **bidireccional** (si A→B existe, B→A existe)
2. Las provincias con `coasts: [...]` tienen valores no-null solo para
   sus vecinos marítimos diferenciados
3. Ningún mar (`SEA`) tiene `coasts` no nulo
4. Ninguna provincia INLAND tiene `coasts` no nulo
5. Si una provincia tiene `coasts: ["north","south"]`, todas sus
   adyacencias marítimas deberían tener un sufijo de costa (o null
   si aplican a cualquiera)

### Errores comunes

| Error | Consecuencia |
|-------|-------------|
| Poner `null` en SPA→MID | El motor ve `null`+`null` y SPA tiene costas → **invalida el movimiento** de flota (lo trata como terrestre) |
| Poner `"south"` en MID→SPA | MID es SEA y no tiene costas; técnicamente funciona pero rompe la convención |
| Olvidar la bidirección (SPA→MID sí, MID→SPA no) | MID no podría atacar SPA ni moverse allí |
| Poner costas en una provincia que no las necesita | No hay problema de validación, pero complica las órdenes (los jugadores tienen que especificar costa siempre) |
