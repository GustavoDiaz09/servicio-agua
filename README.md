# 💧 Sistema de Gestión de Agua Potable

API REST para la administración integral de un servicio de agua potable: clientes, tomas, medidores, lecturas de consumo, tarifas progresivas, facturación, pagos, red de distribución e indicadores.

> Documento técnico para agentes/multiagentes: ver `AGENTS.md`. Diagramas: `diagramas/Diagramas-Sistema-Gestion-Agua-Potable.pdf`.

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3.x |
| Web | Spring Web (REST) |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL 16 |
| Seguridad | Spring Security + JWT (jjwt) |
| Build | Maven (wrapper `./mvnw`) |
| Validación | Jakarta Validation |
| Documentación API | Springdoc / Swagger UI |
| Utilidades | Lombok |

## Arquitectura

Arquitectura por capas por módulo:

```
Controller → Service → Repository → Entity
    ↓           ↓           ↓          ↓
  DTOs      reglas de  consultas   mapeo JPA
  +HTTP     negocio    JPQL        a PostgreSQL
```

Módulos (`com.servicioagua`):

```
common/     enums, excepciones globales, ApiResponse, config (CORS, OpenAPI)
cliente/    Cliente (CRUD, estados, validación por documento)
toma/       Toma (alta/baja, vínculo cliente+sector)
medidor/    Medidor (serie única, asignación 1-1 a toma, retiro)
lectura/    Lectura (cálculo de consumo, historial, anulación)
tarifa/     Tarifa (tramos progresivos con vigencia)
factura/    Factura (generación por periodo, cálculo progresivo, anulación)
pago/       Pago (total/parcial, actualiza estado de factura)
red/        Sector, Tuberia, Fuga, Mantenimiento
security/   Usuario, login JWT, filtro de autenticación, config Seguridad
reporte/    Consultas de agregación
```

## Requisitos

- JDK 17
- PostgreSQL 16 (crear la base `bd_servicio_agua`)
- Maven 3.9+ (o usar `./mvnw`)

## Configuración

Variables de entorno (con defaults para dev):

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/bd_servicio_agua` | URL JDBC |
| `DB_USER` | `postgres` | Usuario |
| `DB_PASSWORD` | `postgres` | Contraseña |
| `CORS_ORIGINS` | `http://localhost:5173,...` | Orígenes permitidos |
| `JWT_SECRET` | clave dev (≥32 bytes) | Secreto HS256 para firmar tokens |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Vida del token |
| `ADMIN_AUTO_CREATE` | `true` | Crea usuario admin inicial si no hay usuarios |
| `ADMIN_USER` | `admin` | Usuario admin inicial |
| `ADMIN_PASSWORD` | `admin123` | Contraseña admin inicial |

> Dev usa `ddl-auto: update` (Hibernate crea/actualiza el esquema). Crear la base antes de arrancar:
>
> ```sql
> CREATE DATABASE bd_servicio_agua;
> ```

> **Seguridad:** todos los endpoints bajo `/api/**` exigen token JWT (`Authorization: Bearer <token>`), salvo `POST /api/auth/login`, Swagger (`/swagger-ui/**`, `/swagger-ui.html`) y OpenAPI (`/v3/api-docs/**`). Credenciales dev: `admin` / `admin123` (cambiar `ADMIN_PASSWORD` en producción y desactivar `ADMIN_AUTO_CREATE` si procede).

## Seguridad en producción

- Autenticación **JWT stateless** (Spring Security 6): `POST /api/auth/login` devuelve `{ token, username, rol }`; el resto de `/api/*` requiere el token en `Authorization`.
- Un único rol `ADMIN`; el usuario inicial se autocrea en arranque si `ADMIN_AUTO_CREATE=true` y no existen usuarios.
- Swagger UI / OpenAPI siguen accesibles sin autenticación (window de documentación o restringir por red/gateway en producción).
- Desconocidos → 404 con `ApiResponse` uniforme (`NoHandlerFoundException` mapeado en `GlobalExceptionHandler`). Pérdida/ausencia de autenticación → 401 con `ApiResponse`.
- Credenciales de BD y JWT solo por variables de entorno (nunca en el repositorio).

## Ejecución

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Endpoints principales

| Módulo | Endpoints |
|---|---|
| Auth | `POST /api/auth/login` |
| Clientes | `GET/POST /api/clientes`, `GET/PUT/DELETE /api/clientes/{id}`, `GET /api/clientes?documento=` |
| Tomas | `GET/POST /api/tomas`, `GET /api/tomas/cliente/{id}`, `GET /api/tomas/sector/{id}`, `PUT /api/tomas/{id}/baja` |
| Medidores | `GET/POST /api/medidores`, `PUT /api/medidores/{id}/asignar`, `PUT /api/medidores/{id}/retirar` |
| Lecturas | `POST /api/lecturas`, `GET /api/lecturas/medidor/{id}`, `PUT /api/lecturas/{id}/anular` |
| Tarifas | `GET/POST /api/tarifas`, `GET /api/tarifas/vigentes` |
| Facturas | `POST /api/facturas/generar`, `GET /api/facturas/{id}`, `GET /api/facturas/cliente/{id}`, `PUT /api/facturas/{id}/anular` |
| Pagos | `POST /api/pagos`, `GET /api/pagos/factura/{id}` |
| Red | `/api/sectores`, `/api/tuberias`, `/api/fugas`, `/api/mantenimientos` (CRUD + cambios de estado) |
| Reportes | `GET /api/reportes/consumo-por-sector`, `GET /api/reportes/ventas-por-periodo`, `GET /api/reportes/clientes-morosos`, `GET /api/reportes/fugas-activas` |

Todas las respuestas se envuelven en `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": { },
  "timestamp": "2026-09-22T10:00:00"
}
```

## Reglas de negocio

1. `Consumo = lectura actual − lectura anterior`; consumo negativo → 409 (relectura inválida). Una lectura `ANULADA` permite re-registrar en la misma fecha.
2. Facturación **progresiva por tramos** de tarifa vigente a la fecha del período + cargo fijo. El vencimiento no puede ser anterior al fin del período facturado; una tarifa vigente hoy no puede eliminarse.
3. Un **solo medidor activo** por toma; asignar otro a una toma con medidor activo → 409 (se requiere retirar el anterior). Retirar un medidor con lecturas sin facturar → 409.
4. Pagos totales o parciales; la factura pasa a `PAGADA` cuando `SUM(pagos) ≥ total`. No se aceptan montos mayores a la deuda pendiente (400).
5. Facturas sin pago total con vencimiento superado → `VENCIDA` (job/consulta).
6. Baja de toma solo si no hay lecturas sin facturar: lecturas `ACTIVA` posteriores al cierre de la última factura **no anulada** del cliente (o si el cliente no tiene facturas) bloquean la baja.
7. Una factura `ANULADA` permite regenerar el mismo período. El consumo que excede el último tramo de tarifa se cobra al precio del tramo superior.

## Códigos de estado

| Código | Uso |
|---|---|
| 200 / 201 | Éxito |
| 400 | Validación / argumentos inválidos |
| 401 | No autenticado / credenciales o token inválidos |
| 404 | Recurso no encontrado |
| 409 | Conflicto de negocio o duplicado |

## Testing y calidad

```powershell
.\mvnw.cmd test          # tests unitarios (contexto + repos)
.\mvnw.cmd clean package # build completo
```

## Repositorio

- Inicializar: `git init && git add -A && git commit -m "Fase 0: fundación del proyecto"`.
- Flujo sugerido: rama `main` + ramas por módulo por agente, merge tras la Fase C.
- No commitear secretos (usar variables de entorno). Wrapper Maven incluido.