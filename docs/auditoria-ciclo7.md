# Auditoría de consistencia — Ciclo 7 (Gestión de usuarios)

Fecha: 2026-09-23
Alcance: ampliación de `security/`: `UsuarioService|Controller` nuevos, DTOs (`UsuarioDto`, `CrearUsuarioRequest`, `EstadoUsuarioRequest`, `ResetPasswordRequest`, `CambioPasswordRequest`), `UsuarioRepository.countByRolAndEnabled`, endpoint `PUT /api/auth/change-password` en `AuthService/Controller`, `UsuarioApiTests`. Commit `2104f35` (ciclo 6) como base.
Método: lectura del diff y del código completo + suite completa (`.\mvnw.cmd -q clean package` → 107 tests verdes, exit 0) + verificación de patrones contra `cliente/` y `red/`.

---

## 1. Aspectos verificados correctos

| Tema | Resultado |
|---|---|
| Convención del módulo | `UsuarioController` replica `ClienteController` (`GET` list, `POST` 201, `PUT /{id}/estado`, `PUT /{id}/password`) con `ApiResponse<T>`; respuestas sin data usan `ApiResponse.ok(msg, null)` (patrón de `red/`). Sin `@ExceptionHandler` por módulo. |
| Reglas de negocio en Service | Toda la lógica en `UsuarioService`: duplicado por `existsByUsername` → 409 (**no** try/catch), 404 con `ResourceNotFoundException`, guardas anti-lockout (no auto-deshabilitarse 409; no deshabilitar al único admin habilitado 409). Controllers solo delegan. |
| Cambio de propia clave | `POST /api/auth/login` (permitAll) y `PUT /api/auth/change-password` (exige token: lo cubre `anyRequest().authenticated()`, ya que el permitAll es solo la ruta exacta `/api/auth/login`). Verifica `passwordEncoder.matches(claveActual, hash)` → 409 si no coincide; la nueva se guarda con BCrypt. |
| Interacción con ciclo 6 | Al deshabilitar un usuario, sus tokens quedan inválidos (fix de `JwtAuthenticationFilter`): la guarda anti-lockout evita que un admin se deje fuera. Sin cambios en `SecurityConfig`. |
| Validación | `@NotBlank`/`@Size(min=8)` en contraseñas; `@NotNull` en `enabled`; mensajes en español. |
| Transaccionalidad | `@Transactional` en escrituras, `readOnly = true` en lecturas. |
| Tests | `UsuarioApiTests` (13) con `addFilters = false` y **`app.security.admin-auto-create=false`** para que el `admin` boot no ensucie conteos/guardas; contexto de autenticación fijado manualmente donde el servicio lee `SecurityContextHolder` (self-disable, change-password). Cubre 400/401/404/409. |

## 2. Hallazgos detectados (severidad · estado)

1. **[ALTA · tests] — `UsuarioApiTests` heredaba el `admin` boot de `AdminUserInitializer` → **corregido**.**
   Con `admin-auto-create=true` (default) el contexto de test arrancaba con el usuario `admin`, rompiendo los conteos (`listar` esperaba 2 y había 3) y la guarda de "único admin" (siempre había 2 habilitados → nunca 409). Corrección: `app.security.admin-auto-create=false` en la suite. Nota de uso: al probar reglas de usuarios, desactivar el auto-create para que los datos sean deterministas.

2. **[MEDIA · backend] — `ApiResponse.ok(msg)` sin data no compila (`Void` vs `String`) → **corregido**.**
   Solo existen `ok(data)` y `ok(msg, data)`. Usar `ok(mensaje)` infiere `ApiResponse<String>` y no encaja con `ApiResponse<Void>`. Corrección: `ApiResponse.ok(msg, null)`, patrón ya usado en `red/`/`tarifa/`.

3. **[decisión] — Reset de contraseña sin confirmación → aceptado.**
   `PUT /api/usuarios/{id}/password` resetea sin pedir la clave actual del target (es prerrogativa de admin; el admin ya pasó su propio auth JWT). No se exige doble confirmación de `newPassword` (consistente con el resto del sistema, sin campos de confirmación). Queda a criterio del orquestador si un reset debe forzar re-login (no: los tokens siguen válidos al no cambiar el username; solo la contraseña cambia).

4. **[decisión · BAJA] — Frontend sin página de usuarios → **abierto**.**
   El alcance del ciclo fue backend. `GET/POST /api/usuarios`, `PUT /{id}/estado`, `PUT /{id}/password` y `PUT /api/auth/change-password` están listos; falta la página `Usuarios` y un "cambiar contraseña" desde el menú de usuario. Recomendado como ciclo corto de frontend o dentro del próximo ciclo.

## 3. Notas

- `UsuarioDto` omite `password` (nunca se serializa). El único lugar que conoce hashes es el servicio.
- No se expone `GET /api/usuarios/{id}` (el listado trae todo; sin secretos). Añadirlo solo si el frontend lo requiere.
- `cambiarEstado` no valida el campo `enabled=false` sobre un usuario ya deshabilitado (no-op idempotente). Aceptado.
- Si en el futuro hay roles ≠ `ADMIN`, revisar `countByRolAndEnabled` para que la guarda aplique por rol crítico, no solo `ADMIN`.

## Resumen

- Backend del ciclo correcto y alineado a convenciones (**107 tests verdes**, `clean package` OK). `UsuarioApiTests` 13.
- 2 hallazgos corregidos (tests con `admin` boot; overload de `ApiResponse`).
- 1 hallazgo **abierto (BAJA)**: página frontend de usuarios + cambio de contraseña desde la UI.