# AGENTS.md

Guía para agentes que trabajen en el **Sistema de Gestión de Agua Potable**.

## Visión general

- Stack: Java 17 · Spring Boot 3.3.x · Spring Web · Spring Data JPA · PostgreSQL · Maven.
- Arquitectura por capas: `Controller → Service → Repository → Entity`, empaquetado por **módulo** (cliente, toma, medidor, lectura, tarifa, factura, pago, red, reporte).
- El dominio completo (entidades, repositorios, enums, manejo de errores, config) ya está consolidado en la fase 0. **No lo modifiques sin autorización del agente coordinador.**
- Lombok disponible: usá `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` en DTO/entidades.

## Comandos

Maven no está en el PATH global. Usá **siempre** el wrapper:

```powershell
.\mvnw.cmd -q compile          # compilar
.\mvnw.cmd -q package          # empaquetar (con tests)
.\mvnw.cmd -q clean package    # limpieza + empaquetado
```

Fallback (sin wrapper): `& "C:\Program Files\NetBeans-24\netbeans\java\maven\bin\mvn.cmd" ...`.

Arranque local en dev usa `ddl-auto: update` (PostgreSQL vía `DB_URL`/`DB_USER`/`DB_PASSWORD`).

## Estándares de código (obligatorios)

1. **Ruta base:** `src/main/java/com/servicioagua/`, paquete root `com.servicioagua`.
2. Un módulo = un paquete con: `Xxx.java` (entity, ya existe), `XxxRepository.java` (ya existe), `XxxService.java`, `XxxController.java`, DTOs (`XxxRequest` / `XxxResponse` dentro del paquete o subpaquete `dto`).
3. DTOs con Jakarta Validation (`@NotBlank`, `@NotNull`, `@Min`, `@Email`, `@DecimalMin`).
4. Respuestas uniformes: envolver en `ApiResponse<T>` — `ApiResponse.ok(data)` / `ApiResponse.ok(msg,data)`. Controladores devuelven `ResponseEntity<ApiResponse<...>>`.
5. Errores: lanzar `BusinessException` (negocio/conflicto → 409), `ResourceNotFoundException` (→ 404), `IllegalArgumentException` (→ 400). **No** atrapar duplicados con try/catch local; usar el validador `existsBy...` del repositorio y lanzar `DuplicateResourceException`.
6. Endpoints bajo `/api/*`. NO agregar control handlers de excepciones por módulo (ya está `GlobalExceptionHandler`).
7. `@Service` en clases de servicio (sin interfaz). `@Transactional` en operaciones de escritura.
8. Fechas: `LocalDate`. Dinero: `BigDecimal` (nunca double). Consumo: `Integer`.
9. Identificadores de entidad: `Long`. NO crear nuevas migraciones SQL (Hibernate genera el esquema).
10. No agregar dependencias al `pom.xml` sin permiso del coordinador.
11. Sin comentarios explicativos en el código (salvo Javadoc de nivel de clase cuando aporte).
12. Idioma: nombres de clases/métodos/columnas en inglés; mensajes al usuario (excepciones, DTO messages) en español.

## Mapa de propiedad de archivos (evita conflictos)

| Módulo (paquete) | Archivos | Dueño |
|---|---|---|
| `common/` | enums, excepciones, `ApiResponse`, config | coordinador (fase 0) |
| dominios completos | entidades + repos de todos los módulos | coordinador (fase 0) |
| `cliente/` | `ClienteService`, `ClienteController`, DTOs | Agente A |
| `toma/` + `medidor/` | services, controllers, DTOs (medidor↔toma) | Agente B |
| `lectura/` + `tarifa/` | services, controllers, DTOs (cálculo consumo + tarifa progresiva) | Agente C |
| `factura/` + `pago/` | services, controllers, DTOs (facturación, pagos, estados) | Agente D |
| `red/` | `SectorService/Controller`, `Tuberia/…`, `Fuga/…`, `Mantenimiento/…` + DTOs | Agente E |
| `reporte/` | `ReporteController`, projections/DTOs de agregación | Agente F |

**Regla de oro:** cada agente escribe EXCLUSIVAMENTE dentro de su paquete. Si necesita un método adicional en un repositorio de su módulo, puede agregarlo (está en su paquete). Nunca tocar archivos de otros módulos ni `common/`.

## Plan de orquestación con agentes paralelos

Ejecutar en **dos fases** para evitar conflictos:

### Fase A — Fundación (coordinador, serie)
1. `pom.xml`, `application.yml`, clase principal, `.gitignore`, wrapper de Maven.
2. `common/`: enums, excepciones, `ApiResponse`, `OpenApiConfig`, `WebConfig`.
3. Entidades y repositorios de los 11 dominios (compila sin lógica de negocio).
4. **Verificación:** `.\mvnw.cmd -q compile` → debe pasar.

### Fase B — Módulos (agentes paralelos, simultáneos)
Lanzar en paralelo un agente `general` por módulo con este brief común:

- Contexto: proyecto Spring Boot en `C:\Users\gusta\Documents\proyectos serios\Servicio del agua`.
- Tarea: crear Service, Controller y DTOs SOLO en `com.servicioagua.<tu_proyecto>`.
- Entidades y repos ya existen (leerlas primero). Reutilizar `common/` (ApiResponse, excepciones).
- No modificar `pom.xml`, entidades ajenas, `common/`, ni archivos de otros paquetes.
- Verificación local: compilar TODO el proyecto con `mvnw.cmd -q compile` (no fallar compilación general).
- Reportar: archivos creados, endpoints expuestos, reglas de negocio implementadas y cualquier dependencia cruzada detectada.

Orden funcional de entrega (los agentes pueden correr a la vez; la integración es al final):

1. **Agente A — cliente:** CRUD Cliente (crear, listar, buscar por id/documento, actualizar, cambiar estado). `GET/POST /api/clientes`, `GET/PUT/DELETE /api/clientes/{id}`.
2. **Agente B — toma + medidor:** CRUD Toma (crear con cliente+sector, listar por cliente/sector, dar de baja solo si no hay lecturas pendientes). CRUD Medidor (alta por serie única, asignar a toma verificando un solo activo por toma, retirar medidor). Endpoints `/api/tomas/*`, `/api/medidores/*`.
3. **Agente C — lectura + tarifa:** CRUD Tarifa (tramos con vigencia). Registro de Lectura con cálculo `consumo = valorActual − valorAnterior`; rechazo si consumo < 0 (BusinessException). Historial por medidor. Endpoints `/api/tarifas/*`, `/api/lecturas/*`.
4. **Agente D — factura + pago:** generar factura por cliente+periodo usando lecturas del periodo y tarifas vigentes (progresivo por tramo + cargoFijo). Anular. Registrar pago (total/parcial) y pasar factura a PAGADA/PARCIAL. Endpoints `/api/facturas/*`, `/api/pagos/*`.
5. **Agente E — red:** CRUD Sector, Tubería, Fuga (con estados reportada/curso/resuelta) y Mantenimiento (preventivo/correctivo, estados). Endpoints `/api/sectores`, `/api/tuberias`, `/api/fugas`, `/api/mantenimientos`.
6. **Agente F — reportes:** consultas de agregación: consumo por sector, ventas por periodo, clientes morosos, fugas activas. Endpoints `/api/reportes/*`. Usa JPQL sobre los repositorios existentes (proyecciones/interfaces o DTO).

### Fase C — Integración (coordinador, serie)
1. Resolver conflictos de compilación/imports.
2. `DataSeeder` (componente `CommandLineRunner`) con datos de demo: sectores, tarifas, clientes, tomas, medidores, lecturas.
3. `mvnw.cmd -q clean package` completo (verde).
4. Revisión de advisors de seguridad si aplica y README.

## Criterios de "hecho"

- Compila `.\mvnw.cmd -q compile` sin errores desde la raíz del módulo.
- Endpoints devuelven `ApiResponse<T>` con códigos HTTP correctos (200/201/400/404/409).
- Reglas de negocio del módulo implementadas en Service (no en Controller).
- No hay duplicación de código ni importaciones sin usar (`mvnw.cmd -q compile` no emite warnings bloqueantes).