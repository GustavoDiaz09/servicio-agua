# Auditoría de consistencia — Ciclo 8 (Despliegue + seguridad)

Fecha: 2026-10-12
Alcance: puesta en producción (backend **Render** `servicio-agua-backend-45jr`, frontend **Vercel** `servicio-agua.vercel.app`, BD **Supabase** `mqkhuekcqfjbkempjcmf`), módulo `security/`, módulos de negocio backend y frontend React.
Método: lectura de código + 3 exploraciones (seguridad, backend, frontend) + suite backend completa (`.\mvnw.cmd -q test` → **121 tests verdes, 0 fallos, exit 0**) + `npm run build` (exit 0, 19.99 s) + verificación en vivo (healthcheck, login, RLS, conteos) + `git grep` de secretos.

---

## 1. Despliegue verificado (correcto)

| Tema | Resultado |
|---|---|
| Backend Render | `GET /swagger-ui.html` → 200 (tras redirect) · `POST /api/auth/login` → 200 · `GET /api/clientes` → 200 |
| Frontend Vercel | `GET https://servicio-agua.vercel.app/` → 200 (SPA "Sistema de Agua") |
| Proxy frontend→backend | `POST /servicio-agua.vercel.app/api/auth/login` → 200 (rewrite `/api` → Render) |
| Base de datos | Supabase por **session pooler** IPv4; RLS `true` en las **12 tablas** de `public` |
| Datos demo | 5 clientes · 3 facturas · 1 pago · 10 lecturas (persistidos) |
| Usuarios | `admin`/ADMIN y `gustavo`/SUPERADMIN, `enabled=true`; claves rotadas este ciclo (`admin1234`, `30470872`) |
| Higiene de secretos | `.env*` **no trackeado** (`git check-ignore` OK); sin credenciales reales en archivos versionados |
| Correcciones de este ciclo | Vercel `rootDirectory` estaba vacío (publicaba la raíz → 404); fijado a `frontend`. `render.yaml`/`vercel.json` apuntando al backend real. |

## 2. Convenciones (verificado correcto)

- Único `@RestControllerAdvice` (`common/GlobalExceptionHandler`), **sin** handlers por módulo; mapeos 400/401/403/404/409/500 completos.
- Los 14 controllers devuelven `ResponseEntity<ApiResponse<...>>`; los 83 endpoints bajo `/api/*`, sin rutas duplicadas.
- `@Transactional` en escrituras y `readOnly = true` en lecturas en todos los services.
- Validación Jakarta en todos los DTO `*Request` (mensajes en español); nombres/columnas en inglés.
- Reglas de negocio en Service (salvo `ReporteController.resolverRango`, ver hallazgo).
- Guards de estado correctos: fugas `RESUELTA`→409; mantenimientos solo `PROGRAMADO` + `fechaProgramada>=hoy`→400; locks pesimistas deliberados (Pago/Cliente).
- `FacturaVencimientoScheduler` con cron configurable **y** `ApplicationReadyEvent`.
- Frontend: endpoints/DTOs/enums **100% alineados** con el backend; sin secretos hardcodeados; `api` con Bearer + limpieza/redirect en 401; `ErrorBoundary` por página; sin `TODO`/`console.log` de depuración.

## 3. Hallazgos (severidad · estado)

### Seguridad

1. **[ALTA · ABIERTO] Escalada de privilegios vía reset de contraseña.**
   `PUT /api/usuarios/{id}/password` (`UsuarioController.java:50`) **no tiene `@PreAuthorize`** y `UsuarioService.resetPassword` (`UsuarioService.java:62`) no llama a `validarGestionSegura` ni valida rol: cualquier autenticado (ADMIN) puede fijar la clave del SUPERADMIN y tomar el control.

2. **[ALTA · ABIERTO] Secreto JWT por defecto hardcodeado en el repo.**
   `SecurityProperties.java:12` y `application.yml:41` (`jwt-secret` default `servicio-agua-d3v-2026-…`). Si un despliegue no define `JWT_SECRET`, la clave firmante es pública. (En Render va `generateValue: true`, así que prod no lo usa; el default sigue en el repo.)

3. **[ALTA · ABIERTO] Credenciales por defecto + `admin-auto-create=true`.**
   `SecurityProperties.java:16-24`, `application.yml:43-47`; publicadas además en `README.md:69-79` y `AGENTS.md:28,45`. Mitigado en la BD actual (usuarios ya existen y claves rotadas), peligroso en BD nueva.

4. **[MEDIA · ABIERTO] Un ADMIN puede deshabilitar al SUPERADMIN.**
   `PUT /{id}/estado` (`UsuarioController.java:43`) sin guard de rol; la única guarda impide deshabilitar al *último* admin habilitado (`UsuarioService.java:53`).

5. **[MEDIA · ABIERTO] Swagger/OpenAPI públicos en producción.**
   `SecurityConfig.java:48` (`permitAll` sin perfil); `render.yaml:10` además lo usa como healthcheck.

6. **[MEDIA · ABIERTO] CORS fuera de la cadena de Spring Security.**
   `WebConfig.java:19-32` define un `CorsFilter` bean con `allowCredentials(true)`, pero `SecurityConfig.java:42-58` no invoca `.cors(...)` ni declara `CorsConfigurationSource`: los preflight `OPTIONS` pueden ser rechazados con 401 antes del filtro (oculto en dev por el proxy de Vite).

7. **[MEDIA · ABIERTO] Login sin rate-limit/lockout.**
   `AuthService.java:27-33` permite fuerza bruta contra `POST /api/auth/login`.

### Backend (negocio)

8. **[MEDIA · ABIERTO] `SectorService.delete` no valida fugas/mantenimientos asociados.**
   `SectorService.java:59-67` solo consulta tubería/toma; las FK `Fuga.sector`/`Mantenimiento.sector` (`optional=false`) hacen fallar el delete → `DataIntegrityViolationException` → **500** (no 409). No cubierto por tests.

9. **[MEDIA · ABIERTO] Sin handler de `DataIntegrityViolationException`.**
   `GlobalExceptionHandler` cae en `handleGeneric` → **500** ante una colisión real de `numero` de factura (unique, `Factura.java:21`).

10. **[MEDIA · ABIERTO] Race del correlativo de `numero`.**
    `FacturaService.generarNumero` (`:197-205`) deriva del `count` global por período, pero el lock es **por cliente** (`findByIdConBloqueo`): dos facturas de clientes distintos y concurrentes en el mismo período pueden calcular el mismo correlativo.

11. **[MEDIA · ABIERTO] Duplicación de lógica.**
    Tarifa progresiva y cargo fijo repetidos en `FacturaService.calcularSubtotal` (`:173-195`) y `DataSeeder.calcularSubtotal` (`:132-154`); "lecturas pendientes" repetido en `TomaService.validarSinLecturasPendientes` (`:137-147`) y `MedidorService.retirar` (`:110-119`). Riesgo de divergencia.

12. **[MEDIA · ABIERTO] `handleGeneric` sin logging.**
    `GlobalExceptionHandler.java:96-100`: los 500 se silencian (no hay `Logger` en `src/main`); sin observabilidad de errores internos.

### Frontend

13. **[MEDIA · ABIERTO] `npm run smoke` depende de `WebSocket` global (Node ≥ 22).**
    `smoke-routes.cjs:78` sin import; `package.json` sin `engines`; la CI usa **Node 20** → `ReferenceError: WebSocket is not defined`.

14. **[MEDIA · ABIERTO] Fallback nullish inconsistente en `client.js`.**
    `client.js:39` `return payload?.data ?? payload`: cuando el backend responde `data: null` (p. ej. `UsuarioController.eliminar`/`resetPassword`) retorna el **envelope** completo en vez de `null`.

15. **[MEDIA · ABIERTO] Errores silenciados en fetches secundarios.**
    `Lecturas.jsx:61`, `Facturas.jsx:114` (`.catch(() => {})`) y `Medidores.jsx:85-87`: si falla la carga de medidores/clientes para un selector, queda vacío sin feedback.

### Bajas

16. **[BAJA · ABIERTO] Backend:** import muerto `TomaRepository.java:3`; código muerto (`TomaRepository.findByCodigo`, `FugaRepository.findByEstadoNot`, `Tarifa.vigenteEn`, campo `Factura.lectura` nunca asignado); `@Data` en entidades JPA (riesgo `LazyInitializationException`/N+1 con `open-in-view:false`); `ClienteController.changeEstado` recibe `Map<String,String>` sin validación; lógica de negocio en `ReporteController.resolverRango`; `ReporteRepository extends JpaRepository<Lectura>` (expone `save/delete`); listados sin paginación (filtrado en memoria/N+1); `Factura.estaPagada()` devuelve `true` también para `ANULADA` (mensaje "factura pagada" impreciso); `Usuario.enabled=true` sin `@Builder.Default`; `cambiarRol` cuenta superadmins deshabilitados; `Collectors.toList()` vs `.toList()`; imports wildcard.
17. **[BAJA · ABIERTO] Seguridad:** tokens sin revocación (cambio de clave/logout no invalida el JWT hasta 24 h); `LoginRequest.password` sin `@Size` (hashing BCrypt sobre cuerpos enormes); `app.seed.enabled` default `true`; sin `accessDeniedHandler` explícito (403 de filtros usa respuesta por defecto); `@Data` en `Usuario` expone el hash en `toString`/logs; sin pin explícito de algoritmo en `JwtService`.
18. **[BAJA · ABIERTO] Frontend:** import sin usar `Tomas.jsx:2` (`ArrowClockwise`); `estadoTone` no cubre `CANCELADO`/`INACTIVO`/`INOPERANTE`; credenciales y path absoluto hardcodeados en `smoke-routes.cjs:6-9`; sin `ErrorBoundary` raíz (no envuelve `AppLayout`/`RequireAuth`); rutas desconocidas renderizan Dashboard (`App.jsx:85`); "todas las facturas" = N requests (no hay `GET /facturas`); métrica "Deuda pendiente" (`Pagos.jsx:97-100`) sobreestima al sumar `total` de PARCIAL sin restar abonos; `.vercel/project.json` en disco; URL del backend hardcodeada en `vercel.json:7`.

## 4. Notas

- `.env` (working tree, gitignored) contiene credenciales reales de producción; `.env.local` un token OIDC de Vercel vivo. No se versionan, pero son material sensible: tratarlos como tal.
- Vercel tiene **Deployment Protection (SSO)** en deployments/preview; el alias de producción es público y funciona.
- Al rotar claves de usuarios por API, los tokens JWT emitidos antes siguen válidos (ver 17).
- Los hallazgos 1, 6 y 8 son los de mayor riesgo real en producción y no requieren refactor grande.

## Resumen

- Deploy en producción **operativo y verificado** end-to-end (Render + Vercel + Supabase), RLS en 12/12 tablas, datos persistidos, secretos fuera del repo, **121 tests verdes** y build frontend OK.
- **3 hallazgos ALTA** (todos en `security/`): reset de contraseña sin control de rol, secreto JWT por defecto y credenciales por defecto.
- **12 MEDIA** (seguridad, negocio y frontend) y una lista de **BAJA** de limpieza/deuda técnica.
- Ninguno bloquea el go-live actual; se recomienda priorizar #1 (escalada de privilegios) seguido de #6 y #8.
