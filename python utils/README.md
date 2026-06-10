# Utilidades Python para variantes de Diplomacy

Scripts para procesar mapas SVG y generar/transformar archivos de variantes
de Diplomacy en el formato JSON usado por UDiplomacy.

---

## `extract_ids.py`

Extrae los códigos de provincia de un SVG clasificándolos por tipo
según la capa (`SEA`, `COAST`, `INLAND`) en la que se encuentren dentro
del archivo.

```
python3 extract_ids.py <mapa.svg> [salida.csv]
```

**Parámetros:**
- `mapa.svg` — archivo SVG del mapa (con `data-code="CODE"` en cada provincia)
- `salida.csv` — opcional; nombre del CSV generado (por defecto `provincias.csv`)

**CSV generado:** dos columnas, `Province` y `Type`, con una fila por provincia.
Los tipos se asignan según el grupo SVG: `SEA` → `SEA`, `COAST` → `COASTAL`,
`INLAND` → `INLAND`. Las provincias aparecen ordenadas alfabéticamente.

---

## `add_datacode.py`

Añade el atributo `data-code="CODE"` a todo elemento `<path>` del SVG
que tenga un `id="CODE"` (código de 2 a 5 caracteres en mayúsculas)
y no tenga ya `data-code`. Modifica el archivo *in situ*.

```
python3 add_datacode.py <mapa.svg>
```

**Parámetros:**
- `mapa.svg` — archivo SVG a modificar

Es útil cuando el SVG solo tiene `id` en los paths pero el backend
necesita `data-code` para identificar las provincias.

---

## `prefix_ids.py`

Renombra los `id` de los elementos del SVG que tienen `data-code`
al formato `provincia-CODE` que espera el frontend para posicionar
unidades y colorear provincias.

```
python3 prefix_ids.py <mapa.svg>
```

**Parámetros:**
- `mapa.svg` — archivo SVG a modificar (se modifica *in situ*)

**Efecto:** Convierte `id="LON"` → `id="provincia-LON"` solo en
elementos que ya tienen `data-code="LON"`. El resto de elementos
(rect, defs, etc.) no se tocan.

**Flujo de trabajo típico:**
```bash
python3 add_datacode.py mapa.svg    # añade data-code a los paths
python3 prefix_ids.py mapa.svg      # renombra id a provincia-CODE
```

---

## `generate_variant.py`

Genera un JSON de variante básico a partir de un SVG y un CSV de
adyacencias. Útil para prototipado rápido cuando el CSV solo contiene
adyacencias (sin tipos, naciones ni unidades).

```
python3 generate_variant.py <mapa.svg> <adyacencias.csv> [salida.json]
```

**Parámetros:**
- `mapa.svg` — SVG del mapa (para obtener la lista de provincias)
- `adyacencias.csv` — CSV donde la 1.ª columna es la provincia y las
  siguientes son sus adyacentes. Soporta la sintaxis `PROV:coast`
  para adjacencias costeras (ej. `VYR:north`, `BUL:east`, `SPA:south`).
- `salida.json` — opcional; si se omite, imprime el JSON por stdout.

**Genera:** JSON con todas las provincias tipadas como `COASTAL`,
`homeNation: null`, `supplyCenter: false`, sin unidades iniciales.
Pensado como esqueleto para completar a mano.

---

## `csv_to_variant.py`

Convierte un CSV completo (con tipos, naciones, supply centers,
unidades iniciales y adyacencias) al JSON de variante del proyecto.

```
python3 csv_to_variant.py <entrada.csv> [salida.json]
```

**Formato del CSV de entrada:**

| Province | Type    | Supply Center | Nation   | unit  | Adj1 | Adj2 | ... |
|----------|---------|---------------|----------|-------|------|------|-----|
| LON      | COASTAL | TRUE          | ENGLAND  | FLEET | YOR  | WAL  | ... |
| YOR      | COASTAL | FALSE         |          |       | LON  | NTH  | ... |
| ADRI     | SEA     | FALSE         |          |       | APU  | VEN  | ... |

- **Province** — código de la provincia (ej. `LON`)
- **Type** — `SEA`, `COASTAL` o `INLAND`
- **Supply Center** — `TRUE` o vacío/FALSE
- **Nation** — nación propietaria al inicio (vacío si neutra)
- **unit** — `ARMY`, `FLEET` o vacío
- **Adj1, Adj2, …** — códigos de provincias adyacentes. Se pueden
  usar sufijos `:north`, `:south`, `:east` para adjacencias costeras
  específicas de una costa.

**Sufijos de costa (`:north`, `:south`, `:east`)**:

Cuando una provincia tiene múltiples costas, una unidad en una costa
concreta solo puede atacar/apoyar a través de esa costa. El sufijo
indica qué costa concreta se usa para esa adyacencia.

**Ejemplo de CSV con sufijos costeros:**

```csv
Province,Type,Supply Center,Nation,unit,Adjacencys
STP,COASTAL,TRUE,RUSSIA,FLEET,BOT,LVN,BAR,NWT,SIB,FIN,GOB
BUL,COASTAL,FALSE,,,RUM,SER,CON,GRE,BLAC,BLA:east,AEG
SPA,COASTAL,FALSE,,,GAS,BRE,PIC,ENG,WES,LYO,MAR,POR,GOU:north,GOU:south
```

En el JSON resultante, las adyacencias con sufijo se renderizan como:
```json
{
  "name": "SPA",
  "adjacencies": {
    "GAS": null,
    "BRE": null,
    "GOU": "north"
  }
}
```
En `BUL` la adyacencia a `BLA` quedaría como `"BLA": "east"`.

**Genera:** JSON completo con:
- `id` y `name` editables (`world-variant` / `World Variant` por defecto)
- `colonialRule: false`
- Array `provinces` con `adjacencies` como mapas
- Array `initialUnits` agrupado por nación

**Ejemplo básico:**
```bash
python3 csv_to_variant.py provincias.csv world-variant.json
```
