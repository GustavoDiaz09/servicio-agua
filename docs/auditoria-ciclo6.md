# Auditoría de seguridad — Ciclo 6 (JWT)

Fecha: 2026-09-23
Alcance: módulo `security/` completo (entidad `Usuario`, `Rol`, repos, `AuthService/Controller`, `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `SecurityProperties`, `AdminUserInitializer`, DTOs), `GlobalExceptionHandler` (handler `AuthenticationException` → 401), `application.yml` (`app.security.*`), traba del frontend (`Login.jsx`, `api/client.js`, `App.jsx`, `AppLayout.jsx`) y test `SecurityApiTests`. Ajuste de todos los `*ApiTests` a `@AutoConfigureMockMvc(addFilters = false)`.
Método: lectura del diff y del código completo del módulo + verificación de que los endpoints protegidos responden 401/404 correctos + suite completa (`.\mvnw.cmd -q clean package` → 94 tests verdes, exit 0) + `npm run build` exit 0.

---

## 1. Aspectos verificados correctos

| Tema | Resultado |
|---|---|
| Frontera de autenticación | `SecurityConfig` exige token en todo `/api/**` salvo `POST /api/auth/login`, Swagger y OpenAPI (`permitAll`). Sesión `STATELESS`, CSRF desactivado. |
| Login | `POST /api/auth/login` valida con `AuthenticationManager` (BCrypt): credenciales OK → `{ token, username, rol }`; inválidas/inexistente → 401. Respuestas uniformes `ApiResponse`. |
| Token | `JwtService` HS256 (`JWT_SECRET` ≥32 bytes), embed `subject` (username) + claim `rol`, expiración `JWT_EXPIRATION_MS` (default 24 h). Firma con `jjwt 0.12`. |
| Estados HTTP | Sin token/token inválido → 401 con `ApiResponse` (entry-point `SecurityConfig`); credenciales fallidas → 401 (handler `AuthenticationException` en `GlobalExceptionHandler`); rutas inexistentes → 404. Sin `/api/auth` adicionales. |
| Inicialización | `AdminUserInitializer` crea `admin`/`admin123` en arranque si `app.security.admin-auto-create=true` y `count()==0`; no pisa usuarios existentes. |
| Frontend | `api` adjunta `Authorization: Bearer` (token en `localStorage` key `agua_token`); ante 401 limpia y redirige a `/login` (excepto en `/auth/login`); guard `RequireAuth` protege las rutas; botón "Salir" en `AppLayout`. |
| Config | `JWT_SECRET`/`JWT_EXPIRATION_MS`/`ADMIN_*` por variables de entorno con defaults para dev; CORS permitido explícito (`app.cors.allowed-origins`). |
| Tests | `SecurityApiTests` ejercita auth real (usuario propio vía `PasswordEncoder` + `POST /api/auth/login`); el resto de módulos desactiva el filtro (`addFilters = false`) y no envía token. Suite total 94 verdes tras los fixes. |

## 2. Hallazgos detectados (severidad · estado)

1. **[ALTA · backend] — Usuario `enabled=false` podía autenticarse — `security/UsuarioDetalleService.java` y `security/JwtAuthenticationFilter.java` → **corregido**.**
   `Usuario.enabled` existe, pero `UserDetails` se construía con `User.builder()` sin `.disabled(...)` (default habilitado) → un usuario deshabilitado seguía logueándose; y `JwtAuthenticationFilter` validaba el token sin chequear `isEnabled` → un token emitido seguía sirviendo tras deshabilitar al usuario. Corrección: `.disabled(!usuario.isEnabled())` en `UsuarioDetalleService.loadUserByUsername` y `&& userDetails.isEnabled()` en el filtro. `DaoAuthenticationProvider` lanza `DisabledException` (subclase de `AuthenticationException`) que el handler global mapea a 401 genérico (sin revelar el motivo).

2. **[MEDIA · backend] — El handler de error genérico filtraba el mensaje interno — `common/GlobalExceptionHandler.handleGeneric` → **corregido**.**
   `"Error interno del servidor: " + ex.getMessage()` exponía detalles internos (clases, SQL, rutas) al cliente — fuga de información. Corrección: mensaje fijo `"Error interno del servidor"`. Ningún test dependía del mensaje (riesgo 0 de regresión).

3. **[MEDIA · integración] — Swagger UI sin botón "Authorize" — `common/config/OpenApiConfig.java` → **corregido**.**
   Los endpoints protegidos devolvían 401 desde Swagger porque `OpenApiConfig` no declaraba esquema de seguridad `Bearer`; probar la API exigía pegar el token a mano en cada request. Corrección (autorizada por el orquestador, toca `common/`): `components.securitySchemes` `bearerAuth` (HTTP bearer, formato JWT) + `security` global. Swagger UI ahora muestra el botón **Authorize**; con el token guardado, "Try it out" envía `Authorization: Bearer <token>`. El candado aparece también en `/api/auth/login` (cosmético: no requiere token).

4. **[decisión · configuración] — `JWT_SECRET` default dev conocido → **nota**.**
   Si se despliega sin setear `JWT_SECRET`, la clave firmante es pública en el repo → tokens falsificables. Diseño aceptado para dev (24 h de expiración). En producción **siempre** setear `JWT_SECRET` por entorno (y considerar `ADMIN_AUTO_CREATE=false`). Opcional futuro: fallar al arranque si `JWT_SECRET` no viene del entorno.

5. **[BAJA · UX] — `RequireAuth` valida solo existencia del token → **nota**.**
   Un token vencido/presente pero inválido se detecta en la primera llamada (`api` → 401 → redirige a `/login`). Sin página de login dirigida por estado de sesión; comportamiento aceptable y ya cubierto por tests (401 en filtro).

6. **[BAJA · tests] — Cobertura de estado `enabled` → **corregido**.**
   Añadidos 2 tests de regresión a `SecurityApiTests` (7 → 9): login de usuario deshabilitado → 401 y token de usuario deshabilitado → 401 (filtro). Suite total 94 verdes.

## 3. Notas

- `AuthenticationException` → 401 con mensaje genérico ("Usuario o contraseña incorrectos") no revela si falla el usuario o la contraseña; por diseño.
- Sin gestión de usuarios (cambio de clave, listado) fuera de alcance: rol único `ADMIN`, un solo usuario se autocrea (o la consola de BD para altas manuales).
- JWT sin `jti`/revocación activa: no hay cierre de sesión server-side. El cierre es client-side (botón "Salir" limpia `agua_token`). Aceptable para el alcance; documentado.
- `spring.jpa.open-in-view` quedó activo (warning en tests); heredado, fuera de alcance.

## Resumen

- Backend de seguridad: correcto y alineado a las convenciones (`SecurityApiTests` 9, suite total **94** verdes, `clean package` OK).
- Correcciones del ciclo: **1 ALTA** (usuario deshabilitado autenticable — login + token) y **1 MEDIA** (fuga de información en error genérico), ambas con tests de regresión.
- Queda **1 hallazgo abierto (MEDIA)**: esquema Bearer en Swagger — requiere coordinación por tocar `common/`. → **Corregido** (2026-09-23, con autorización del orquestador).