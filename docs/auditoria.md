# Auditoría de consistencia — Sistema de Gestión de Agua Potable

Fecha: 2026-09-22
Alcance: `src/main/java/com/servicioagua/**` (~110 clases) + `src/main/resources/application.yml`.
Método: lectura completa del código (sin ejecutar Maven). Revisión de imports, convenciones, bugs no-estructurales, hardening de configuración y duplicidad de rutas.

---

## 1. Hallazgos corregidos

| Archivo | Corrección |
|---|---|
| `src/main/java/com/servicioagua/red/TuberiaService.java` | Import sin usar eliminado: `com.servicioagua.common.dto.ApiResponse` (era copiado de Controller). |
| `src/main/java/com/servicioagua/factura/dto/GenerarFacturaRequest.java` | Mensajes de validación capitalizados para cumplir convención (español, arranque en mayúscula): "el cliente…", "el período…", "el vencimiento…" → "El …". |
| `src/main/java/com/servicioagua/pago/dto/RegistrarPagoRequest.java` | Ídem: "la factura…", "el monto…", "el método…" → capitalizados. |

No se detectaron otros imports sin usar en el código (se revisaron los 109 archivos). Los mensajes restantes del proyecto están en español y los identificadores/nombres de métodos/columnas en inglés, conforme al estándar.

## 1-bis. Resoluciones del orquestador (2026-09-22)

Decisión sobre los hallazgos de la sección 2. Cada ítem fue atendido y verificado con tests (`.\mvnw.cmd -q test` → 65/65).

| Item | Decisión | Implementación |
|---|---|---|
| 1 (off-by-one tarifa) | Corregir | `FacturaService.calcularSubtotal` con patrón "restante" y capacidad de banda (`desde=0 → hasta`; si no `hasta-desde+1`). Tests: `FacturaApiTests.generarFacturaConLecturasDevuelve201ConTotalesCorrectos` (60.00 / 12560.00). |
| 2 (re-registro de lectura) | Permitir re-registro si la anterior está `ANULADA` | `LecturaRepository.existsByMedidorIdAndFechaLecturaAndEstadoNot(..., ANULADA)` + `LecturaService:31`. Test: `LecturaApiTests.anularYReRegistrarEnLaMismaFechaDevuelve201`. |
| 3 (NoHandlerFoundException → 500) | Mapear a 404 JSON | `@ExceptionHandler(NoHandlerFoundException.class)` en `GlobalExceptionHandler`. Se conserva `spring.mvc.throw-exception-if-no-handler-found` (funcional en Boot 3.3; deprecación sin impacto). |
| 4 (springdoc expuesto) | Documentar restricción (sin añadir dependencias) | Sección "Seguridad en producción" en `README.md`. |
| 5 (update no valida vacíos) | Rechazar blank en `documento`/`nombre` | Guards en `ClienteService.update` → 400. Test: `ClienteApiTests.actualizarClienteConDocumentoVacioDevuelve400`. |
| 6 (vencimiento < fin de período) | Validar | `FacturaService.generar` rechaza vencimiento anterior a `periodoHasta` → 400. Test: `FacturaApiTests.generarFacturaConVencimientoAnteriorAlPeriodoDevuelve400`. |
| 7 (baja con lecturas pendientes) | **Regla definida por el negocio**: bloquear si CUALQUIER medidor de la toma tiene lecturas `ACTIVA` posteriores al cierre de la última factura del cliente (o si no existen facturas) | `TomaService.validarSinLecturasPendientes` (inyecta `FacturaRepository`). Tests: `TomaMedidorApiTests.darDeBajaConLecturasPendientesDevuelve409` / `darDeBajaSinLecturasPendientesDevuelve200`. |
| 8 (sobrepago) | **Regla definida por el negocio**: rechazar monto > deuda pendiente → 400 | `PagoService.registrar` compara `monto` contra `total - SUM(pagos)`. Test: `PagoApiTests.pagoMayorADeudaPendienteDevuelve400`. |
| 9 (eliminar tarifa vigente) | **Regla definida por el negocio**: bloquear borrado si está vigente hoy → 409 | `TarifaService.eliminar`. Tests: `TarifaApiTests.eliminarTarifaVigenteDevuelve409` / `eliminarTarifaExpiradaDevuelve200`. |
| 10 (mensajes inconsistentes) | Mantenido (cosmético) | No se modifica para no alterar el contrato de API; los mensajes son claros en ambos formatos. |
| 11 (código muerto) | Mantenido | Fase 0; no se toca. |
| 12 (estilo) | Mantenido (cosmético) | Sin impacto funcional. |

Tests añadidos: 8 (65 en total, todos verdes).

## 2. Hallazgos detectados NO corregidos

Ordenados por severidad. Requieren decisión del orquestador o del agente dueño del módulo.

1. **Bug de contorno en tarifa progresiva — `factura/FacturaService.java:169-181`** (`calcularSubtotal`).
   Los tramos se definen como rangos `[desde, hasta]` contiguos (p. ej. 0–10 y 11–30) pero el cálculo hace `min(consumo, hastaM3) - desdeM3`, lo que "saltea" el m3 del límite: consumo=11 factura 10 m3 (el m3 11 cae en `consumoTotal <= desdeM3` → `continue`), consumo=30 factura 29 m3, consumo=31 factura 29 m3. Requiere redefinir rangos (p. ej. 0–10, 10–29, 29–N) o aplicar `desdeM3` exclusivo de forma consistente. **No corregido: modifica montos facturados.** Dueño: Agente C/D.

2. **`lectura/LecturaRepository.java:26` + `lectura/LecturaService.java:30`** — `existsByMedidorIdAndFechaLectura` bloquea el re-registro en una fecha aunque la lectura previa esté `ANULADA` (y `findEntreFechas` sí excluye anuladas). Una lectura anulada no puede reinstaurarse/corrergirse en la misma fecha. Posible intención de negocio, pero incoherente con la exclusión de anuladas del resto del flujo. Reportar a Agente C.

3. **`application.yml` — `spring.mvc.throw-exception-if-no-handler-found: true`.**
   Propiedad deprecada en Spring Boot 3.2+; con el resource handler activo no tiene efecto real. Si llegara a lanzarse `NoHandlerFoundException`, el `GlobalExceptionHandler` no la mapea y caería en el handler genérico → **500 en vez de 404**. No modifiqué el archivo: habilitar/remodelar esto sin agregar el handler cambiaría el comportamiento de errores. Recomendación al orquestador: agregar `@ExceptionHandler(NoHandlerFoundException.class)` → 404 (mismo status HTTP que el default) y revisar la propiedad.

4. **`application.yml` — springdoc sin protección.**
   `/swagger-ui.html` y `/v3/api-docs` quedan expuestos. No hay spring-security en el classpath ni schemes en `OpenApiConfig`, así que no se puede endurecer sin agregar dependencia (prohibido). Recomendación: documentar restricción por red/entorno en producción.

5. **`cliente/ClienteService.java:64-89` (`update`).**
   Se valida con `ClienteRequest` sin `groups`, por lo que `@NotBlank` (declarado `groups = OnCreate`) no aplica en PUT: un `""` en `documento`/`nombre` **sobreescribe** el valor actual sin error. Validación parcial. No corregido (cambiaría comportamiento de validación/contrato del endpoint).

6. **`factura/FacturaService.java:43-52`** — no valida que `vencimiento` sea posterior a `periodoHasta`; puede generar facturas con vencimiento anterior al período facturado. Reportar a Agente D.

7. **`toma/TomaService.java:126-145` (`darDeBaja`)** — solo bloquea si el medidor ACTIVO tiene lecturas en el **mes actual**. Tomas sin medidor activo o con lecturas de meses previos pueden darse de baja. Regla "sin lecturas pendientes" ambigua; confirmar con Agente B si "pendientes" = mes corriente o período abierto.

8. **`pago/PagoService.java`** — se permite registrar monto mayor a la deuda; la factura pasa a `PAGADA` igual (no hay validación de excedente ni vuelto). Decisión de negocio a confirmar.

9. **`tarifa/TarifaService.java:77-81` (`eliminar`)** — borra tarifas sin validar uso previo en facturas (Factura no referencia Tarifa, por lo que no hay FK que lo impida). Agente C que confirme política.

10. **Inconsistencia de mensajes `ResourceNotFoundException`.** Mezcla "el sector/el cliente/la toma" (español, minúscula) con "Cliente/Factura/Sector" (capitalizado): "No se encontró el sector con identificador: 1" vs "No se encontró Cliente con identificador: 1". Cosmético; no toqué services ajenos.

11. **Código muerto (fase 0, no se modifica):** `TomaRepository.findByCodigo`, `FugaRepository.findByEstadoNot`, `Tarifa.vigenteEn()` no se usan en ningún servicio.

12. **Estilo (cosmético):** `TomaService`/`MedidorService` usan `Collectors.toList()` y varios controllers usan wildcard `import ...annotation.*` mientras el resto usa `.toList()` e imports explícitos. `@Column(name=...)` se declara explícito en `Tuberia/Fuga/Mantenimiento/lectura/tarifa` e implícito (naming snake_case) en `Cliente/Toma/Sector`; el esquema resultante es consistente. Sin impacto funcional.

## 3. Conflictos de rutas / endpoints

- **No hay endpoints duplicados** (mismo method+path) entre controllers ni dentro de cada uno. Bases: `/api/clientes`, `/api/tomas`, `/api/medidores`, `/api/lecturas`, `/api/tarifas`, `/api/facturas`, `/api/pagos`, `/api/sectores`, `/api/tuberias`, `/api/fugas`, `/api/mantenimientos`, `/api/reportes`.
- `POST /api/facturas/generar` y `POST /api/facturas/marcar-vencidas` son rutas estáticas, sin colisión con `GET/PUT /api/facturas/{id}*`.
- **Ambigüedad potencial (sin conflicto real):** en varios controllers conviven `GET /{id}` con sub-rutas `/estado/…`, `/serie/…`, `/sector/…`, `/cliente/…`. Spring resuelve por especificidad (más específica gana), pero `GET /api/medidores/serie` (sin valor) intentará bindear `{id}` → 400. Es el patrón estándar Sprint MVC; se reporta solo informativamente (renombrar rutas está fuera de alcance).

## 4. Notas de configuración (`application.yml`)

- `show-sql: false` ✓, `open-in-view: false` ✓, `jackson.write-dates-as-timestamps: false` ✓, datasource con variables de entorno ✓. No se modificó ni el datasource ni el puerto.
- Único endurecimiento sugerido (no aplicado): handler de `NoHandlerFoundException` → 404 y revisión de exposición de la UI de springdoc (ver sección 2, items 3 y 4).