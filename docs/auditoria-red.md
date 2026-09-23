# Auditoría de consistencia — Fase Red (Ciclo 4, commit `e077e42`)

Fecha: 2026-09-23
Alcance: commit `e077e42` ("Red: completar CRUD de Fugas y Mantenimientos (PUT/DELETE) con tests API; Exportar CSV en Reportes; actualizar AGENTS.md") + `bac2ac9` (solo AGENTS.md). Archivos: `red/FugaService|Controller`, `red/MantenimientoService|Controller`, `RedApiTests`, `frontend/src/pages/Fugas.jsx|Mantenimientos.jsx|Reportes.jsx`, `AGENTS.md`.
Método: lectura del diff y del código completo del módulo `red/` + entidades + DTOs de reportes + suite completa (`.\mvnw.cmd -q clean package` → 77 tests verdes, exit 0) + verificación de patrones contra módulos ya consolidados (`TuberiaController`, `Sectores.jsx`, `api/client.js`).

---

## 1. Aspectos verificados correctos

| Tema | Resultado |
|---|---|
| Convención del módulo | `PUT /{id}` y `DELETE /{id}` replican exactamente `TuberiaController` (200, `ApiResponse.ok(msg, data)` con `data=null` en DELETE). Sin handler de excepciones por módulo. |
| Reglas de negocio en Service | Sí: `FugaService.update` bloquea `RESUELTA` (409); `MantenimientoService.update` bloquea estados ≠ `PROGRAMADO` (409). Controllers solo delegan. |
| Validación de DTO | `@Valid` + `@NotNull/@NotBlank/@Size` en `FugaRequest`/`MantenimientoRequest` (se reutilizan en PUT y POST). |
| Transaccionalidad | `@Transactional` en escrituras, `readOnly = true` en lecturas. |
| Errores HTTP | `BusinessException`→409 (estado no editable), `ResourceNotFoundException`→404 (fuga/mantenimiento o sector inexistente). |
| Entidades | `Fuga`/`Mantenimiento` referencian `Sector` por `@JoinColumn`; borrar una fuga/mantenimiento no rompe FKs (ninguna entidad los referencia). Reporte `fugas-activas` consulta `estado IN ('REPORTADA','EN_CURSO')` en `reporte/`, ajeno al borrado. |
| Rutas | Sin conflictos: `PUT/DELETE /api/fugas/{id}` conviven con `/{id}/en-curso` y `/{id}/resolver`; `PUT /api/mantenimientos/{id}` con `/{id}/estado` (Spring resuelve por especificidad). Sin endpoints duplicados. |
| Tests | `RedApiTests` pasa de 8 a 18: cubre update/delete de fuga (200/409/404) y mantenimiento (200/409), y 404 posterior al delete. Suite total 77 verdes. |
| Builds | `mvnw clean compile`, `mvnw test` y `mvnw clean package` exit 0. `npm run build` exit 0. |

## 2. Hallazgos detectados (severidad · estado)

Ordenados por severidad. Estados: **corregido** (aplicado y verificado tras la auditoría) o **abierto** (pendiente de decisión).

1. **[ALTA · frontend] — `api.delete(...)` no existe: se debe usar `api.del(...)` — `frontend/src/pages/Fugas.jsx:136` y `frontend/src/pages/Mantenimientos.jsx:152` → **corregido**.**
   `src/api/client.js` expone **`del`** (no `delete`). Las páginas consolidadas usan `api.del` (`Sectores.jsx:99`, `Tarifas.jsx:137`, `Tuberias.jsx:118`). Las nuevas llamaban `api.delete(...)` → `TypeError: api.delete is not a function` en runtime: el botón **Eliminar** de Fugas/Mantenimientos nunca borraba y mostraba el error. `npm run build` no lo detectaba (identificador indefinido). Corrección: `api.delete` → `api.del` en ambos archivos.

2. **[ALTA · frontend] — `DownloadSimple` usado sin import — `frontend/src/pages/Reportes.jsx:182` → **corregido**.**
   `import { ChartLineUp, MapTrifold, Receipt, UsersThree, Warning }` no incluía `DownloadSimple`. El botón "Exportar CSV" renderiza en la sección default (`consumo`) → `ReferenceError` al abrir Reportes; sin error boundary, la página (posiblemente toda la app) quedaba en blanco. Corrección: añadir `DownloadSimple` al import.

3. **[MEDIA · frontend] — CSV de consumo exporta el ID de sector en vez del nombre — `frontend/src/pages/Reportes.jsx` → **corregido**.**
   `ConsumoSectorDto(Long sector, String nombre, Long consumoTotal)`: `f.sector` es el id numérico; el nombre es `f.nombre` (la `DataTable` de consumo ya lo usaba `key: 'nombre'`). La exportación usaba `{ key: 'sector', header: 'Sector' }` → columna "Sector" con IDs. Corrección: usar `key: 'nombre'`.

4. **[MEDIA · backend] — `MantenimientoService.update` no valida fecha programada pasada — `red/MantenimientoService.java` → **corregido**.**
   `create` rechaza `fechaProgramada < hoy` (IllegalArgumentException → 400); `update` replicaba el estado pero **omitía ese guard** → por API se podía reprogramar un mantenimiento programado a una fecha pasada. Corrección: replicar el guard en `update` + 2 tests de regresión (`RedApiTests` pasa de 18 a 20: + 400 fecha pasada, + 404 inexistente).

5. **[decisión · BAJA] — Borrado sin restricción de estado — `FugaService.delete` / `MantenimientoService.delete` → **corregido** (decisión: lo que no se puede editar tampoco se puede borrar).**
   Se podía eliminar permanentemente una fuga `RESUELTA` o un mantenimiento `COMPLETADO/CANCELADO` (histórico operativo), mientras el resto del catálogo protege borrados (sector con tubería → 409, tarifa vigente → 409). Regla adoptada (simétrica a la de edición): `FugaService.delete` lanza 409 si `estado == RESUELTA` (msg "No se puede eliminar una fuga resuelta"); `MantenimientoService.delete` lanza 409 si `estado != PROGRAMADO` (msg "Solo se puede eliminar un mantenimiento programado").

6. **[BAJA · UX] — Botón "editar" visible siempre — `Fugas.jsx`, `Mantenimientos.jsx` → **corregido**.**
   El backend rechazaba con 409 editar fuga `RESUELTA` y mantenimiento no `PROGRAMADO`, pero la UI mostraba lápiz y papelera en cualquier estado. Ahora los botones editar/eliminar se renderizan solo en estados editables (`Fugas`: `estado !== 'RESUELTA'`; `Mantenimientos`: `estado === 'PROGRAMADO'`), en coherencia con la API y con el nuevo guard de borrado.

7. **[BAJA · tests] — Cobertura de update/delete incompleta → **corregido**.**
   Añadidos 4 tests: update con sector inexistente → 404 (fuga y mantenimiento), delete de fuga `RESUELTA` → 409, delete de mantenimiento `COMPLETADO` → 409. `RedApiTests` pasa de 20 a 24.

## 3. Notas

- `npm run build` verde **no garantiza runtime OK**: los errores 1 y 2 son identificadores indefinidos que el bundler no detecta. Para la fase: smoke manual en `Reportes`, `Fugas` y `Mantenimientos` (navegador + backend `spring-boot:test-run`).
- N+1 heredado (no regresión de esta fase): `toResponse` toca `fuga.getSector()`/`mantenimiento.getSector()` dentro de `findAll` (1 consulta por fila). Mismo patrón de `TuberiaService`/`ReporteService`; documentado en ciclos previos.
- `FugaService.update` permite editar una fuga `EN_CURSO` (coherente con "no resuelta = editable").
- Código muerto de fase 0 conservado (`FugaRepository.findByEstadoNot`, etc.), fuera de alcance.

## Resumen

- Backend del módulo: correcto y alineado a las convenciones (`RedApiTests` 24, suite total **83** verdes, `clean package` OK).
- Frontend de la fase: **2 bugs de runtime (ALTA)** que rompían eliminar (Fugas/Mantenimientos) y toda la página Reportes; **1 bug de datos en CSV (MEDIA)**; **1 inconsistencia de validación en backend (MEDIA)**; **1 decisión de borrado**; **1 UX de acciones por estado**; **cobertura de tests** — **los 7 hallazgos quedan corregidos**, compilados (`mvnw`, `npm run build`) y verificados.