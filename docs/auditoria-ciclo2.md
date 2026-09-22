# Auditoría de consistencia — Ciclo 2 (commit `16727fa`)

Fecha: 2026-09-22
Alcance: commit `16727fa` ("Atender pendientes de auditoria (items 2-9)") + revisión global de `src/main/java/com/servicioagua/**` (98 archivos) y `src/test/java/**` (9 archivos).
Método: lectura completa del código (sin ejecutar Maven por decisión del orquestador). Se verificó el diff del commit, imports, convenciones, reglas de negocio reimplementadas, problemas de concurrencia/performance y código muerto heredado. Working tree limpio al inicio; se aplicó una única corrección aditiva (ver sección 1).

---

## 1. Hallazgos corregidos

| Archivo | Corrección |
|---|---|
| `src/main/java/com/servicioagua/pago/dto/RegistrarPagoRequest.java` | Validación aditiva en `monto`: `@DecimalMin("0.01")` + `@Digits(integer = 10, fraction = 2)`. El monto se compara sin redondear en `PagoService.java:40` pero se persiste en una columna `NUMERIC(12,2)`; un monto con más de 2 decimales (p. ej. 49.999) era redondeado por la BD mientras el cálculo de deuda usaba el valor crudo → inconsistencia silenciosa. La anotación es aditiva (ningún monto válido pasa a ser rechazado) y sigue el patrón del resto del catálogo. |

No se detectaron imports sin usar, typos, mensajes sin capitalizar ni gaps de guard en los archivos del commit (los mensajes ya arrancan en mayúscula y `ClienteService.update` ya valida blank en `documento`/`nombre`).

## 1-bis. Ítems pendientes de decisión del orquestador

Los hallazgos de la sección 2 marcados como **[decisión]** requieren definición de negocio o tocan archivos de `common/`/módulo ajeno; se dejan documentados sin modificar (misma mecánica que el ciclo 1).

## 1-ter. Resoluciones del orquestador (2026-09-22, commit posterior)

El orquestador corrigió los hallazgos 1, 2, 3, 4, 5, 6, 7, 8 (parte remanente), 10 y 12. Resoluciones y decisiones:

| Ítem | Decisión / Resolución |
|---|---|
| 1 | Handlers explícitos → **400**: `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `ConstraintViolationException` (`GlobalExceptionHandler`). |
| 2 | `@Lock(PESSIMISTIC_WRITE)`: `FacturaRepository.findByIdConBloqueo` (usado en `PagoService.registrar` serializa la deuda) y `ClienteRepository.findByIdConBloqueo` (usado en `FacturaService.generar` serializa duplicados y el correlativo de `numero` por cliente/período). |
| 3 | `generar` usa `existsByClienteIdAndPeriodoDesdeAndEstadoNot(..., ANULADA)` → se puede refacturar un período con factura anulada. |
| 4 | `clientesMorosos` descuenta los pagos: `deuda = Σ(total − Σ(pagos))` mediante `PagoRepository.sumMontosPorFacturas` (una sola consulta agrupada, sin N+1). |
| 5 | `LecturaRepository`: `existsByMedidorIdAndEstado` y `existsByMedidorIdAndEstadoAndFechaLecturaGreaterThan`; `TomaService.validarSinLecturasPendientes` deja de cargar el historial completo (N+1 eliminado). |
| 6 | `retirar` aplica la misma regla de corte que la baja (sin factura **no anulada** del cliente → bloquea con lecturas `ACTIVA`). |
| 7 | Se alinea el **README** al comportamiento (asignar segundo medidor activo → 409; reglas 3 y 6 actualizadas). |
| 8 | Se cobra el **remanente** de consumo que excede el `hastaM3` del último tramo al precio del tramo superior. Los **huecos entre tramos** (0-10 y 15-30) se aceptan: los m3 intermedios se escalonan al tramo inmediato superior (política documentada). |
| 10 | El corte ahora usa `MAX(periodoHasta)` de facturas **no anuladas** del cliente (`FacturaRepository.maxPeriodoHastaNoAnulada`). |
| 12 | Mensaje corregido: "No se encontró el medidor con serie: X". |
| 9 | **Diferido (fase E).** Requiere constraint de exclusión en BD o aislamiento serializable; no se toca esquema de fase 0. Documentado como limitación. |
| 11 | Código muerto de fase 0 mantenido (fuera de alcance). |

Tests de regresión agregados: `FacturaApiTests` (`generarFacturaConSobranteSobreUltimoTramoCobradoDevuelve201`, `generarFacturaTrasAnularLaAnteriorDevuelve201`, `obtenerFacturaConIdNoNumericoDevuelve400`), `TomaMedidorApiTests.retirarMedidorConLecturasSinFacturarDevuelve409`, `ReporteApiTests.clientesMorososDescuentaPagosDevuelve200`.

## 2. Hallazgos detectados NO corregidos

Ordenados por severidad. Entre paréntesis, dueño del módulo / tipo.

1. **[ALTA · decisión] — 500 en vez de 400 para entradas mal tipadas o JSON inválido — `common/exception/GlobalExceptionHandler.java:50-54`.**
   El handler genérico `@ExceptionHandler(Exception.class)` absorbe `MethodArgumentTypeMismatchException` (id/estado no convertible), `HttpMessageNotReadableException` (body malformado o enum inválido) y `MissingServletRequestParameterException` → respuesta **500**. Ejemplo: `GET /api/facturas/abc`, `GET /api/tomas/estado/xyz`, JSON con enum inexistente. `MethodArgumentNotValidException` sí está cubierto (:42). Propuesta: handlers explícitos → 400, con mensaje en español. Archivo en `common/` (coordinador). Sin test que falle hoy (ninguno ejercita esos casos).

2. **[MEDIA · concurrencia] — TOCTOU en pago y facturación — `pago/PagoService.java:38-40`, `factura/FacturaService.java:53` y `:192`.**
   `PagoService.registrar` lee `sumMontosByFacturaId` y valida sin `@Lock`: dos pagos simultáneos por el total pueden superar la deuda. `FacturaService.generar` valida con `existsByClienteIdAndPeriodoDesde` sin constraint único (carrera → factura duplicada) y `generarNumero` (`:192`) deriva del `count` (carrera → posible colisión de `numero`). Propuesta: `@Lock(PESSIMISTIC_WRITE)` al cargar la `Factura`, unique constraint `(cliente_id, periodo_desde)` mapeando `DataIntegrityViolation` → 409, y reserva serial de número.

3. **[MEDIA · decisión] — Factura anulada impide refacturar el período — `factura/FacturaService.java:53`.**
   `existsByClienteIdAndPeriodoDesde` no filtra `ANULADA`: al anular una factura no se puede regenerar el período, las lecturas quedan sin facturar y, además, `TomaService.validarSinLecturasPendientes` (nuevo este commit) las considera pendientes → la baja del socio queda bloqueada sin vía de resolución. Propuesta: excluir `ANULADA` del chequeo o permitir una regeneración/reinstalación explícita.

4. **[MEDIA · decisión] — `clientesMorosos` sobre-estima la deuda — `reporte/ReporteService.java:52-68`.**
   `deudaTotal = Σ total de facturas` (`:62-63`) sin descontar pagos registrados: una factura `PARCIAL` con pagos suma su total como deuda completa. Propuesta: deuda = `total − SUM(pagos de la factura)` o definir formalmente "moroso".

5. **[MEDIA · performance] — `validarSinLecturasPendientes`: N+1 y carga de historial completo — `toma/TomaService.java:138-148`.**
   Por cada medidor de la toma se hace `findByMedidorIdOrderByFechaLecturaDesc(medidorId)` (toda la historia de lecturas) y el filtrado `ACTIVA` + `isAfter(corte)` se resuelve en JVM (`:145-148`). Propuesta: `existsByMedidorIdAndEstadoAndFechaLecturaAfter(medidorId, ACTIVA, corte)` (null-safe) en `LecturaRepository` (Agente C). La regla de corte en sí es correcta: la última factura del cliente por `periodoDesde` DESC, y lecturas posteriores al `periodoHasta` cierran la baja; sin facturas → bloquea (borde conservador).

6. **[MEDIA · decisión] — Retiro de medidor deja consumo sin facturar — `medidor/MedidorService.java:102-116`.**
   `retirar` (a) valida lecturas solo del **mes actual** (`YearMonth.now()`, `:107-109`), inconsistente con la regla "sin pendientes" del commit (corte por factura), y (b) hace `setToma(null)` (`:114`) sin expulsar específicamente lecturas pendientes previas: `FacturaService.sumarConsumoPeriodo` (`:139-141`, filtrado de toma/medidor por estado) y `TomaService.validarSinLecturasPendientes` dejan de verlas → consumo del período sin facturar. Contraste directo con el alta del hallazgo 3.

7. **[BAJA · docs vs código] — Reemplazo de medidor — `medidor/MedidorService.java:89-93` vs `README.md`.**
   README regla 3: "asignar otro medidor retira el anterior"; la implementación **lanza 409** "La toma ya tiene un medidor activo" (solo se admite retiro previo). Alinear documento o implementación (Agente B).

8. **[BAJA · decisión] — Tarifas: huecos entre tramos y remanente no facturable — `tarifa/TarifaService.java:116-131` y `factura/FacturaService.java:172-186`.**
   `validarSinSolape` evita solape pero permite rangos separados (0-10 y 15-30): los m3 11-14 se cobran al precio del tramo superior. Además, `calcularSubtotal` no factura el consumo que excede el `hastaM3` del último tramo (el "restante" sobrante no se cobra). Decisión de negocio (definir topes y tolerancia de gaps).

9. **[BAJA · concurrencia] — Solape de tarifas validado en memoria — `tarifa/TarifaService.java:118-128`.**
   `findAll()` + validación en JVM: dos altas simultáneas pueden insertar tramos solapados (carrera). Propuesta: constraint de exclusión en DB o transacción serializable (decisión del orquestador; fase 0 no toca esquema).

10. **[BAJA] — Corte de `validarSinLecturasPendientes` frágil en bordes — `toma/TomaService.java:139-142`.**
    Usa la última factura por `periodoDesde` DESC, no el máximo `periodoHasta`: si los períodos se solapan o la última factura del cliente es de otra toma, el `corte` puede no reflejar el último cierre real (anuladas cuentan como cierre). Bajo impacto en el flujo mensual.

11. **[BAJA · muerto] — Código sin uso (fase 0, no se modifica):** `TomaRepository.findByCodigo`, `FugaRepository.findByEstadoNot`, `Tarifa.vigenteEn()`, y el campo `Factura.lectura` (nunca asignado; el DTO expone `numero`/`vencimiento`).

12. **[BAJA · cosmético] — Mensajes heredados mantenidos por decisión del ciclo 1:** "No se encontró el medidor con serie con identificador: X" (`medidor/MedidorService.java:77`) y mezcla "el cliente"/"Cliente". No se tocan services ajenos.

## 3. Conflictos de rutas / endpoints

- **Sin endpoints duplicados** (mismo method+path). Bases: `/api/clientes`, `/api/tomas`, `/api/medidores`, `/api/lecturas`, `/api/tarifas`, `/api/facturas`, `/api/pagos`, `/api/sectores`, `/api/tuberias`, `/api/fugas`, `/api/mantenimientos`, `/api/reportes`.
- **Ambigüedad informativa ya conocida** (ciclo 1 §3): conviven `/{id}` con sub-rutas literales (`/serie`, `/sector`, `/estado/…`); Spring resuelve por especificidad, pero una sub-ruta sin valor bindea `{id}` → `MethodArgumentTypeMismatchException` → **hoy 500** hasta resolver hallazgo 1.
- El commit **no añadió rutas nuevas** (16 archivos: services/repos/DTOs/tests + aplicar.yml de test).

## 4. Notas de configuración (`application.yml`)

- Item 3 del ciclo 1 resuelto: `@ExceptionHandler(NoHandlerFoundException.class)` → 404 JSON (`GlobalExceptionHandler:36-40`) y `spring.mvc.throw-exception-if-no-handler-found: true` activo en application.yml de main y test. Pendiente: **no hay test que ejercite el handler** (recomendación heredada; fuera de alcance por no tocar `common/`).
- **Tests con H2 aislado por contexto:** la URL `jdbc:h2:mem:servicioagua-${app.seed.enabled}` embebe el valor; los `@SpringBootTest(properties = "app.seed.enabled=false")` usan la DB `servicioagua-false` (seeder off) y `ServicioAguaApplicationTests` usa `servicioagua-true` (seeder on) en un contexto separado. Diseño correcto, sin compartición accidental de datos entre contextos.
- Sin cambios a datasource, puerto ni `show-sql`. springdoc expuesto sigue documentado en `README.md` (decisión ciclo 1, sin dependencias nuevas).

## Resumen

- Archivos revisados: 98 (`main`) + 9 (`test`).
- Correcciones aplicadas en la auditoría: 1 (aditiva, validación de `monto`) + 10 resoluciones del orquestador (ver sección 1-ter).
- Hallazgos NO corregidos: 2 documentados como limitaciones — 9 (concurrencia en Alta tarifas, diferido a fase E) y 11 (código muerto fase 0).
- Los ítems 2, 6, 7, 8 del commit `16727fa` se verificaron correctos e integrados (tests `PagoApiTests`, `LecturaApiTests`, `TomaMedidorApiTests`, `ClienteApiTests`, `FacturaApiTests`, `TarifaApiTests` presentes en el diff).