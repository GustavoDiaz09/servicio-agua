# Auditoría de consistencia — Ciclo 5 (endurecimiento, HEAD `f9979b2`)

Fecha: 2026-09-23
Alcance: ítems de decisión del ciclo 2 (§1-bis / §1-ter) verificados en HEAD actual, handler global de 404 para rutas inexistentes, y estado de las limitaciones documentadas (tarifas concurrentes, código muerto).
Método: lectura del código actual + verificación de tests existentes + nuevos tests de regresión. Verificado con `.\mvnw.cmd -q clean package` (85 tests verdes, exit 0).

---

## 1. Hallazgo nuevo (ALTA) corregido

| Diagnóstico | Corrección |
|---|---|
| **Rutas inexistentes → 500 en vez de 404.** En Spring 6.1 los paths sin controlador caen en el resource handler `/**`, que lanza `NoResourceFoundException` ("No static resource api/…"). `GlobalExceptionHandler` solo cubría `NoHandlerFoundException` (camino hoy inalcanzable en la app real), por lo que el handler genérico `Exception.class` devolvía **500**. Contradice lo documentado en el ciclo 2 (§4): el 404 JSON nunca se ejercitaba. | Se agregó `@ExceptionHandler(NoResourceFoundException.class)` → **404 JSON** con mensaje "No se encontró el recurso solicitado" (`common/exception/GlobalExceptionHandler.java`). **Toque en `common/` autorizado por el orquestador** para este ciclo. |

Tests de regresión nuevos — `src/test/java/com/servicioagua/common/CommonApiTests.java` (2 tests, contexto `seed=false`):
- `rutaInexistenteDevuelve404` — `GET /api/ruta-inexistente` → 404, `$.success=false`, mensaje esperado.
- `rutaInexistentePostDevuelve404` — `POST /api/ruta-inexistente` → 404.

Sin regresión en Springdoc: `/swagger-ui/**` y `/v3/api-docs/**` usan resource handlers propios con ubicaciones válidas; el handler nuevo solo cambia la respuesta de recursos realmente ausentes.

## 2. Ítems de decisión del ciclo 2 — verificados RESUELTOS en HEAD

| Ítem ciclo 2 | Estado en HEAD | Verificación |
|---|---|---|
| 3 — Factura anulada impide refacturar | **Resuelto.** `FacturaService.generar` usa `existsByClienteIdAndPeriodoDesdeAndEstadoNot(…, ANULADA)`; el corte de pendientes usa `maxPeriodoHastaNoAnulada`. | Test `FacturaApiTests.generarFacturaTrasAnularLaAnteriorDevuelve201`. |
| 6 — `retirar` mes actual / `setToma(null)` | **Resuelto.** `MedidorService.retirar` aplica el corte por factura (misma regla que la baja); el guard bloquea el retiro si hay lecturas `ACTIVA` posteriores al corte → no queda consumo sin facturar. | Test `TomaMedidorApiTests.retirarMedidorConLecturasSinFacturarDevuelve409`. |
| 7 — README vs 409 en reemplazo | **Resuelto.** Regla 3 del README alineada: "asignar otro a una toma con medidor activo → 409 (se requiere retirar el anterior)". | Código `MedidorService.asignarAToma` + README :117. |
| 8 — Huecos entre tramos / remanente | **Resuelto.** Remanente sobre el último tramo se cobra al precio del tramo superior; política de huecos documentada (escalado al tramo inmediato superior). | Test `FacturaApiTests.generarFacturaConSobranteSobreUltimoTramoCobradoDevuelve201`. |
| 1 — Handlers de entrada inválida → 400 | **Resuelto.** `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `ConstraintViolationException` → 400. | Test `FacturaApiTests.obtenerFacturaConIdNoNumericoDevuelve400`. |
| 2, 4, 5, 10, 12 | Resto de resoluciones del orquestador (locks pesimistas, morosos con descuento de pagos, `existsBy…` sin N+1, corte `maxPeriodoHasta` no anulada, mensaje de serie). | Código verificado; suite completa verde. |

## 3. Limitaciones ratificadas (decisión del orquestador)

| Ítem | Decisión |
|---|---|
| 9 — Alta/actualización concurrente de tarifas con tramos solapados (validación en JVM sobre `findAll`) | **Se mantiene como limitación conocida.** Requiere constraint de exclusión (`[vigenteDesde, vigenteHasta)` x rango m3) que no es portable a H2 y contradice "no se toca esquema fase 0" (Hibernate genera el esquema, sin migraciones SQL). Herramienta admin monousuario; el riesgo (dos altas simultáneas desde dos sesiones) es aceptable y se documenta. Mitigación futura sugerida: lock de aplicación o aislamiento serializable al detectar doble sesión. |
| 11 — Código muerto de fase 0 (`TomaRepository.findByCodigo`, `FugaRepository.findByEstadoNot`, `Tarifa.vigenteEn()`, campo `Factura.lectura`) | **Se mantiene** por directiva (fase 0 no se modifica; sin impacto funcional). |

## Resumen

- **1 hallazgo ALTA corregido** (404 de rutas inexistentes) + **2 tests nuevos** de regresión.
- **5 ítems de decisión del ciclo 2 verificados resueltos** en HEAD actual (3, 6, 7, 8, 1) con sus tests.
- **2 limitaciones ratificadas** y documentadas (9 concurrencia tarifas, 11 código muerto).
- Suite total: **85 tests verdes** (`mvnw clean package` exit 0). Sin cambios de frontend.