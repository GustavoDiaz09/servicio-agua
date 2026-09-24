# AGENTS.md

Guía para agentes que trabajen en el **Sistema de Gestión de Agua Potable**.

## Stack y arquitectura

- Java 17 · Spring Boot 3.3.5 · Spring Web · Spring Data JPA · Spring Security + JWT (jjwt) · PostgreSQL · Maven (wrapper).
- Capas `Controller → Service → Repository → Entity`, empaquetado por módulo bajo `com.servicioagua` (`src/main/java/com/servicioagua/`).
- **Estado actual: backend completo + autenticación JWT** (véase "Seguridad"). `common/`, entidades y repositorios consolidados: **no modificarlos sin coordinación**.
- DTO/entidades usan Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).

## Comandos — backend

Maven **no** está en el PATH global. Usar siempre el wrapper desde la raíz:

```powershell
.\mvnw.cmd -q compile          # compilar (verificación rápida)
.\mvnw.cmd -q test             # tests de API: MockMvc + H2 (sin necesidad de PostgreSQL)
.\mvnw.cmd -q clean package    # build completo (es lo que corre la CI)
```

Arranque dev: requiere base PostgreSQL `bd_servicio_agua` ya creada; credenciales vía `DB_URL`/`DB_USER`/`DB_PASSWORD` (defaults `localhost:5432`, `postgres`/`postgres`). Dev usa `ddl-auto: update` (Hibernate genera el esquema, sin migraciones SQL).

PostgreSQL **local** (este equipo): la instalación de Windows (`Program Files`) está rota/sin binarios; se usa un **cluster portable PG 17.11** en `%LOCALAPPDATA%\PostgreSQL\pgsql\bin` (datos en `%LOCALAPPDATA%\PostgreSQL\pgdata`, superuser `postgres`/`postgres`, puerto 5432). No es un servicio de Windows: arrancar con `pg_ctl -D <pgdata> -l <log> start` (o `pg_ctl stop`) antes/después de usar el backend. No "reparar" la instalación de `Program Files`.

```powershell
.\mvnw.cmd spring-boot:run     # API en http://localhost:8080 (Swagger en /swagger-ui.html, sin auth de login: admin/admin123)
```

`DataSeeder` (`common/config/`) carga datos demo si `app.seed.enabled=true` (default): `.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--app.seed.enabled=false`.

**Dev rápido sin PostgreSQL** (H2 en memoria, mismo `application.yml` de tests vía classpath de test; datos no persistidos):

```powershell
.\mvnw.cmd spring-boot:test-run   # API en http://localhost:8080 con datos demo
```

## Seguridad (JWT)

- Todos los `/api/**` exigen `Authorization: Bearer <token>` salvo `POST /api/auth/login`, Swagger (`/swagger-ui.html`, `/swagger-ui/**`) y OpenAPI (`/v3/api-docs/**`): `permitAll` en `SecurityConfig`.
- `POST /api/auth/login` con `{ username, password }` → `{ token, username, rol }`. Un único rol `ADMIN`.
- Usuario dev `admin`/`admin123` se autocrea al arrancar (`AdminUserInitializer`) si `app.security.admin-auto-create=true` y no existen usuarios.
- Config por variables de entorno: `JWT_SECRET` (≥32 bytes, HS256), `JWT_EXPIRATION_MS`, `ADMIN_USER`, `ADMIN_PASSWORD`, `ADMIN_AUTO_CREATE` (defaults en `SecurityProperties`/`application.yml`; sin cambios el secreto expira en 24 h).
- Sin token/token inválido → 401 con `ApiResponse` JSON uniforme (entry-point en `SecurityConfig`; `AuthenticationException` → 401 también mapeado en `GlobalExceptionHandler`). Rutas inexistentes siguen → 404.
- Swagger UI incluye botón **Authorize** (esquema `bearerAuth` en `OpenApiConfig`): pegar el token para probar endpoints protegidos con "Try it out".
- Gestión de usuarios en `/api/usuarios` (listar/crear/estado/reset clave, rol único `ADMIN`) y `PUT /api/auth/change-password` para cambiar la propia clave. Guardas anti-lockout en `UsuarioService` (no deshabilitarse a sí mismo, no deshabilitar al único admin habilitado → 409). Al probar reglas de usuarios en tests, fijar `app.security.admin-auto-create=false`.

## Frontend (fuera del build Maven)

- `frontend/`: React 19 + Vite 6 + Tailwind 4. No lo compila Maven ni lo toca la CI.
- `npm run dev` desde `frontend/` (ejecutar con `npm.cmd` si PowerShell bloquea `npm.ps1`) → `http://localhost:5173`. Si el puerto está ocupado, Vite usa `5174`/`5175…`.
- Proxy de Vite: `/api` → `http://localhost:8080`. Error `[vite] http proxy error ... ECONNREFUSED` en la terminal = **el backend no está corriendo** (8080 cerrado), no es un bug del frontend.
- Páginas implementadas (rutas en `App.jsx`): Dashboard, Clientes, Tomas, Medidores, Lecturas, Tarifas, Facturas, Pagos, Red (Sectores/Tuberias/Fugas/Mantenimientos), Reportes.
- **Auth en el frontend:** `Login.jsx` (`/login`), guard `RequireAuth` en `App.jsx`; el token JWT vive en `localStorage` (`agua_token`) vía `auth` de `client.js`; `api` añade `Authorization: Bearer` automáticamente y en 401 limpia el token y redirige a `/login` (excepto en `/auth/login`). Botón "Salir" en `AppLayout`.
- Patrones reutilizados: `api` de `src/api/client.js` (`payload.data`), `formatMoney/formatDate/m3` en `lib/format.js`, componentes de `components/ui.jsx` (`DataTable`, `Modal`, `ConfirmDialog`, `Badge`, `estadoTone`, `StatCard`, `EmptyState`, `Field`, `inputCls`, `Button`).
- Tras modificar frontend, verificar con `npm run build` (desde `frontend/`). La CI corre `npm ci` + `npm run build` (job `frontend` en `.github/workflows/ci.yml`).

## Estándares de código (obligatorios)

1. Un módulo = paquete `com.servicioagua.<modulo>` con `XxxService`, `XxxController` y DTOs (`dto/`). La entidad y el repo ya existen.
2. DTOs con Jakarta Validation (`@NotBlank`, `@NotNull`, `@Min`, `@Email`, `@DecimalMin`).
3. Respuestas uniformes: `ApiResponse<T>` (`ApiResponse.ok(data)` / `ApiResponse.ok(msg, data)`); controllers devuelven `ResponseEntity<ApiResponse<...>>`.
4. Errores: `BusinessException` → 409, `DuplicateResourceException` → 409 (validar con `existsBy...` del repo, **no** try/catch de duplicados), `ResourceNotFoundException` → 404, `IllegalArgumentException` → 400. No agregar `@ExceptionHandler` por módulo: `GlobalExceptionHandler` (en `common`) cubre todo.
5. `@Service` sin interfaz; `@Transactional` en escrituras, `readOnly = true` en lecturas.
6. Endpoints bajo `/api/*`. Reglas de negocio en el Service, nunca en el Controller.
7. Tipos: `LocalDate` (fechas), `BigDecimal` (dinero), `Integer` (consumo), `Long` (ids). Sin migraciones SQL (Hibernate genera el esquema).
8. Nombres/métodos/columnas en inglés; mensajes al usuario (excepciones, validación) en español. Sin comentarios explicativos.
9. No agregar dependencias al `pom.xml`, no tocar `common/`, entidades ni repos de otros módulos sin autorización.

## Mapa de propiedad (frontera de escritura)

| Paquete | Servicios/Cotrollers/DTOs | Dueño |
|---|---|---|
| `cliente/` | `ClienteService/Controller`, DTOs | Agente A |
| `toma/` + `medidor/` | services, controllers, DTOs (medidor↔toma) | Agente B |
| `lectura/` + `tarifa/` | services, controllers, DTOs (consumo + tarifa progresiva) | Agente C |
| `factura/` + `pago/` | services, controllers, DTOs (facturación, pagos, estados) | Agente D |
| `red/` | `Sector`/`Tuberia`/`Fuga`/`Mantenimiento` + DTOs | Agente E |
| `reporte/` | `ReporteController/Service`, projections/DTOs | Agente F |
| `security/` | `Usuario` (entity+repo+service+controller), `AuthService/Controller`, `JwtService`, `SecurityConfig` | ciclo 6-7 |
| `common/` + entidades + repos | consolidados fase 0 | coordinador |

Cada agente trabaja **solo** dentro de su paquete. Un método nuevo en el repositorio de su módulo puede agregarlo; nada fuera de ese paquete.

## Tests

- `src/test/java/com/servicioagua/<modulo>/*ApiTests.java`: `@SpringBootTest` + `MockMvc` + H2 (`MODE=PostgreSQL`) + `@Transactional`. No requieren PostgreSQL.
- Los tests de módulo usan `@AutoConfigureMockMvc(addFilters = false)` para **desactivar el filtro JWT** (no envían token). Solo `SecurityApiTests` ejercita auth real: crea su propio usuario con `PasswordEncoder` y saca token vía `POST /api/auth/login`.
- La mayoría desactiva el seed con `@SpringBootTest(properties = "app.seed.enabled=false")` y arma sus propios datos; `src/test/resources/application.yml` lo deja activo por defecto.
- Al cambiar reglas de negocio: actualizar/ampliar el `*ApiTests` del módulo. Correr uno solo: `.\mvnw.cmd -q test "-Dtest=ClienteApiTests"`.
- CI (`.github/workflows/ci.yml`) corre `./mvnw -B -ntp clean package` en `main`/PRs: el proyecto entero debe compilar y pasar tests, no solo el módulo tocado.
- `docs/auditoria.md`, `docs/auditoria-ciclo2.md`, `docs/auditoria-ciclo5.md`, `docs/auditoria-red.md`, `docs/auditoria-ciclo6.md` y `docs/auditoria-ciclo7.md` registran hallazgos/requisitos de ciclos previos: consultarlos antes de tocar reglas de negocio existentes.

## Criterios de "hecho"

- `.\mvnw.cmd -q compile` sin errores desde la raíz.
- Tests del módulo en verde; sin imports sin usar ni duplicación de código.
- `ApiResponse<T>` con códigos HTTP correctos (200/201/400/401/404/409).
- Reglas de negocio en Service (no en Controller).

## Estado para continuar — Red (Agente E)

**Cerrado y auditado el 2026-09-23** (commits `e077e42` + correcciones; verificado con `mvnw -q clean package` y `npm run build`). Datos clave para no romper reglas:
- Fugas: editables/borrables salvo `RESUELTA` (409). Mantenimientos: editables/borrables solo en `PROGRAMADO` (409); `fechaProgramada >= hoy` (400). Updates resuelven el sector con `sectorRepository.findById(...).orElseThrow(...)`.
- Frontend borra con `api.del`, **no** `api.delete`; botones de editar/eliminar solo en estados permitidos; CSV de consumo usa `nombre`, no `sector`.
- El "bloqueo" previo de `factura/`/`lectura/` era diagnóstico erróneo: esas entidades/DTOs **sí** tienen los campos (`Factura.total/periodoDesde/periodoHasta/vencimiento/estado`, `LecturaRequest.medidorId`); `HEAD` siempre compiló verde.
- Auditoría completa en `docs/auditoria-red.md` (7 hallazgos corregidos): consultarlo antes de tocar este módulo.

**Gotcha: aluvión de "cannot find symbol" por Lombok.** Con `clean compile`, cualquier error **real** de javac en un archivo (p. ej. método duplicado, typo) hace que se descarte la generación de Lombok para toda la compilación → lluvia de `cannot find symbol` de getters/setters/`builder()` por casi todos los módulos (`cliente`, `factura`, `lectura`, …). NO son errores reales: los getters existen (Lombok `@Data`). Hay que buscar el **primer** error que no sea de Lombok (suele estar en el archivo que se está editando) y, al arreglarlo, la lluvia desaparece sola.

- No modificar `common/`, entidades, repos ni otros paquetes sin autorización.