# AGENTS.md

Guía para agentes que trabajen en el **Sistema de Gestión de Agua Potable**.

## Stack y arquitectura

- Java 17 · Spring Boot 3.3.5 · Spring Web · Spring Data JPA · Spring Security + JWT (jjwt) · PostgreSQL · Maven (wrapper).
- Capas `Controller → Service → Repository → Entity`, empaquetado por módulo bajo `com.servicioagua` (`src/main/java/com/servicioagua/`). Módulos: `cliente`, `toma`, `medidor`, `lectura`, `tarifa`, `factura`, `pago`, `red`, `security`, `reporte`, `common`.
- `common/` (enums, `ApiResponse`, `GlobalExceptionHandler`, config) y las entidades/repos de cada módulo están consolidados y **no se modifican sin coordinación** (véase "Frontera de escritura").
- DTOs/entidades usan Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).

## Comandos — backend (siempre desde la raíz, con el wrapper)

Maven **no** está en el PATH global. Usar siempre el wrapper:

```powershell
.\mvnw.cmd -q compile          # compilar (verificación rápida)
.\mvnw.cmd -q test             # tests de API: MockMvc + H2 (sin necesidad de PostgreSQL)
.\mvnw.cmd -q test "-Dtest=ClienteApiTests"   # correr un solo test de módulo
.\mvnw.cmd -q clean package    # build completo (es lo que corre la CI)
```

**Arranque dev con PostgreSQL:** requiere la base `bd_servicio_agua` ya creada; credenciales vía `DB_URL`/`DB_USER`/`DB_PASSWORD` (defaults `localhost:5432`, `postgres`/`postgres`). Dev usa `ddl-auto: update` (Hibernate genera el esquema, sin migraciones SQL).

PostgreSQL **local** (este equipo): la instalación de Windows (`Program Files`) está rota/sin binarios; se usa un **cluster portable PG 17.11** en `%LOCALAPPDATA%\PostgreSQL\pgsql\bin` (datos en `%LOCALAPPDATA%\PostgreSQL\pgdata`, superuser `postgres`/`postgres`, puerto 5432). No es un servicio de Windows: arrancar con `pg_ctl -D <pgdata> -l <log> start` (o `pg_ctl stop`) antes/después de usar el backend. No "reparar" la instalación de `Program Files`.

```powershell
.\mvnw.cmd spring-boot:run     # API en http://localhost:8080 (Swagger en /swagger-ui.html, sin auth; login: admin/admin123, gustavo/admin123)
```

`DataSeeder` (`common/config/`) carga datos demo si `app.seed.enabled=true` (default): `.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--app.seed.enabled=false`. Incluye **3 facturas demo** (PENDIENTE/PARCIAL/VENCIDA) para la página Pagos; su número se genera con `facturaRepository.countByPeriodoDesde(...) + 1`.

**Dev rápido sin PostgreSQL** (H2 en memoria usando el `application.yml` de tests, datos no persistidos):

```powershell
.\mvnw.cmd spring-boot:test-run   # API en http://localhost:8080 con datos demo
```

> ⚠️ **Reinicios borran la sesión dev:** H2 en memoria crea los usuarios al arrancar (`AdminUserInitializer`) y pierde todo al reiniciar; si el navegador quedó con un token viejo, `client.js` responde a un 401 borrando el token y redirigiendo a `/login`. Tras reiniciar el backend, hacer logout/re-login.

## Seguridad (JWT)

- Todos los `/api/**` exigen `Authorization: Bearer <token>` salvo `POST /api/auth/login`, Swagger (`/swagger-ui.html`, `/swagger-ui/**`) y OpenAPI (`/v3/api-docs/**`): `permitAll` en `SecurityConfig`.
- `POST /api/auth/login` con `{ username, password }` → `{ token, username, rol }`. Roles: `ADMIN` (gestión general) y `SUPERADMIN` (único; además puede eliminar usuarios, cambiar rol y contraseña de otros). `@EnableMethodSecurity` + `@PreAuthorize("hasRole('SUPERADMIN')")` en los endpoints exclusivos; denegación sin permiso → **403** (`AccessDeniedException` mapeado en `GlobalExceptionHandler`).
- Usuarios dev se autocrean al arrancar (`AdminUserInitializer`) si `app.security.admin-auto-create=true` y no existen usuarios: `admin`/`admin123` (ADMIN) y `gustavo`/`admin123` (SUPERADMIN).
- Config por variables de entorno: `JWT_SECRET` (≥32 bytes, HS256), `JWT_EXPIRATION_MS`, `ADMIN_AUTO_CREATE`, `ADMIN_USER`, `ADMIN_PASSWORD`, `SUPERADMIN_USER`, `SUPERADMIN_PASSWORD` (defaults en `SecurityProperties`/`application.yml`; sin cambios el secreto expira en 24 h).
- Sin token/token inválido → 401 con `ApiResponse` JSON uniforme (entry-point en `SecurityConfig`; `AuthenticationException` → 401 también mapeado en `GlobalExceptionHandler`). Rutas inexistentes → 404.
- Swagger UI incluye botón **Authorize** (esquema `bearerAuth` en `OpenApiConfig`): pegar el token para probar endpoints protegidos con "Try it out".
- Gestión de usuarios en `/api/usuarios` (listar/crear/estado/reset clave; `DELETE /{id}` y `PUT /{id}/rol` solo SUPERADMIN) y `PUT /api/auth/change-password` para cambiar la propia clave. Guardas anti-lockout en `UsuarioService` (no gestionarse a sí mismo, no deshabilitar al último admin habilitado, solo un SUPERADMIN posible, no quitar el rol al único superadministrador → 409). Al probar reglas de usuarios en tests, fijar `app.security.admin-auto-create=false`.

## Frontend (fuera del build Maven)

- `frontend/`: React 19 + Vite 6 + Tailwind 4 (CSS-first: **no hay `tailwind.config.js`**; el tema/`@theme` vive en `src/index.css`). No lo compila Maven ni lo toca la CI.
- `npm run dev` desde `frontend/` (ejecutar con `npm.cmd` si PowerShell bloquea `npm.ps1`) → `http://localhost:5173`. Si el puerto está ocupado, Vite usa `5174`/`5175…`.
- Proxy de Vite: `/api` → `http://localhost:8080`. Error `[vite] http proxy error ... ECONNREFUSED` en la terminal = **el backend no está corriendo** (8080 cerrado), no es un bug del frontend.
- Rutas en `App.jsx`; auth: `Login.jsx` (`/login`), guard `RequireAuth`; token JWT en `localStorage` (`agua_token`) vía `auth` de `src/api/client.js`. `api` añade `Authorization: Bearer` automáticamente y en 401 limpia el token y redirige a `/login` (excepto `/auth/login`). Borrado de recursos con **`api.del` (no `api.delete`)**.
- Patrones reutilizados: `api` de `src/api/client.js` devuelve `payload.data`; `formatMoney/formatDate/m3` en `lib/format.js`; componentes en `components/ui.jsx` (`DataTable`, `Modal`, `ConfirmDialog`, `Badge`, `estadoTone`, `StatCard`, `EmptyState`, `Field`, `inputCls`, `Button`, `PageHeader`, `Spinner`, `Skeleton`).
- Tras modificar frontend, verificar con `npm run build` (desde `frontend/`). La CI corre `npm ci` + `npm run build` (job `frontend` en `.github/workflows/ci.yml`).
- **Smoke de rutas: `npm run smoke`** (desde `frontend/`, con backend en :8080 y Vite dev en :5173). Navega las 15 rutas en Edge/Chrome headless vía CDP, detecta `ReferenceError`/`TypeError`/pantallas en blanco y devuelve exit ≠ 0 si alguna falla. `npm run build` verde **no** detecta identificadores indefinidos (rollup no falla ante esas referencias); el smoke sí — es lo que cazó el bug de `DataTable` en Pagos.
- `ErrorBoundary` (`components/ErrorBoundary.jsx`) envuelve cada página en `App.jsx`: un error de render muestra una tarjeta de recuperación en lugar de desmontar toda la app (pantalla en blanco).

## Estándares de código (obligatorios)

1. Un módulo = paquete `com.servicioagua.<modulo>` con `XxxService`, `XxxController` y DTOs (`dto/`). La entidad y el repo ya existen.
2. DTOs con Jakarta Validation (`@NotBlank`, `@NotNull`, `@Min`, `@Email`, `@DecimalMin`).
3. Respuestas uniformes: `ApiResponse<T>` (`ApiResponse.ok(data)` / `ApiResponse.ok(msg, data)`); controllers devuelven `ResponseEntity<ApiResponse<...>>`.
4. Errores: `BusinessException` → 409, `DuplicateResourceException` → 409 (validar con `existsBy...` del repo, **no** try/catch de duplicados), `ResourceNotFoundException` → 404, `IllegalArgumentException` → 400. No agregar `@ExceptionHandler` por módulo: `GlobalExceptionHandler` (en `common`) cubre todo.
5. `@Service` sin interfaz; `@Transactional` en escrituras, `readOnly = true` en lecturas.
6. Endpoints bajo `/api/*`. Reglas de negocio en el Service, nunca en el Controller.
7. Tipos: `LocalDate` (fechas), `BigDecimal` (dinero), `Integer` (consumo), `Long` (ids). Sin migraciones SQL (Hibernate genera el esquema).
8. Nombres/métodos/columnas en inglés; mensajes al usuario (excepciones, validación) en español. Sin comentarios explicativos.
9. No agregar dependencias al `pom.xml`.

## Frontera de escritura

- Cada agente escribe **solo** en su módulo (services/controllers/DTOs). Puede agregar métodos al repositorio de **su propio** módulo; **no** tocar `common/`, entidades, repositorios ni paquetes ajenos sin autorización.
- Dominios por paquete: `cliente` (clientes), `toma`+`medidor` (vínculo medidor↔toma), `lectura`+`tarifa` (consumo + tarifa progresiva), `factura`+`pago` (facturación/pagos/estados), `red` (Sector/Tuberia/Fuga/Mantenimiento), `reporte` (agregaciones/CSV), `security` (Usuarios, JWT, SecurityConfig).
- `docs/` registra hallazgos/requisitos de ciclos previos: consultar la auditoría del módulo (`docs/auditoria*.md`) antes de tocar reglas de negocio existentes.

## Tests

- `src/test/java/com/servicioagua/<modulo>/*ApiTests.java`: `@SpringBootTest` + `MockMvc` + H2 (`MODE=PostgreSQL`) + `@Transactional`. No requieren PostgreSQL. Actualmente **119 tests**.
- Los tests de módulo usan `@AutoConfigureMockMvc(addFilters = false)` para **desactivar el filtro JWT** (no envían token). Solo `SecurityApiTests`/`UsuarioApiTests` ejercitan auth real.
- La mayoría desactiva el seed con `@SpringBootTest(properties = "app.seed.enabled=false")` y arma sus propios datos; `src/test/resources/application.yml` lo deja activo por defecto.
- Al cambiar reglas de negocio: actualizar/ampliar el `*ApiTests` del módulo.
- CI (`.github/workflows/ci.yml`) corre `./mvnw -B -ntp clean package` (+ frontend `npm ci`/`npm run build`) en `main`/PRs: el proyecto entero debe compilar y pasar tests, no solo el módulo tocado.

## Gotchas de módulo ya cerrados (no romper)

- Facturas: `FacturaVencimientoScheduler` (`factura/`) marca automáticamente como VENCIDA las pendientes/parciales vencidas cada día a las 00:05 (cron configurable con `app.facturas.marcar-vencidas-cron`); no tocar: si no corre, el marcado sigue disponible en `POST /api/facturas/marcar-vencidas`.
- Fugas: editables/borrables salvo estado `RESUELTA` → 409. Mantenimientos: editables/borrables solo en `PROGRAMADO` → 409; `fechaProgramada >= hoy` → 400. Los updates resuelven el sector con `sectorRepository.findById(...).orElseThrow(...)`.
- Frontend Red/Reportes: botones de editar/eliminar solo en estados permitidos; CSV de consumo usa `nombre`, no `sector`.
- `npm run build` verde **no garantiza runtime OK** (el bundler no detecta identificadores indefinidos): tras tocar páginas, correr `npm run smoke` (requiere backend :8080 + Vite dev :5173 levantados).

**Gotcha: aluvión de "cannot find symbol" por Lombok.** Con `clean compile`, cualquier error **real** de javac en un archivo (p. ej. método duplicado, typo) hace que se descarte la generación de Lombok para toda la compilación → lluvia de `cannot find symbol` de getters/setters/`builder()` por casi todos los módulos. NO son errores reales: los getters existen (Lombok `@Data`). Buscar el **primer** error que no sea de Lombok (suele estar en el archivo que se está editando); al arreglarlo, la lluvia desaparece sola.

## Criterios de "hecho"

- `.\mvnw.cmd -q compile` sin errores desde la raíz.
- Tests del módulo en verde; sin imports sin usar ni duplicación de código.
- `ApiResponse<T>` con códigos HTTP correctos (200/201/400/401/404/409).
- Reglas de negocio en Service (no en Controller).