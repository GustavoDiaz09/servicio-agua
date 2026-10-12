#requires -version 5.1
<#
.SYNOPSIS
    Auditoría local de seguridad/auth del Sistema de Gestión de Agua Potable.

.DESCRIPTION
    Verifica por código invariantes de seguridad (JWT, credenciales por defecto,
    exposición de Swagger, CORS, rate-limit, handlers 401/403/429, guardas de
    usuarios, higiene de secretos) y corre los tests de seguridad. Genera un
    informe Markdown en docs/audit/ y devuelve exit code != 0 si algo falla.

.PARAMETER SkipTests
    No ejecuta Maven (solo checks estáticos).

.PARAMETER OutDir
    Carpeta de salida de los informes (default: docs/audit).
#>
[CmdletBinding()]
param(
    [switch]$SkipTests,
    [string]$OutDir
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
if (-not $repo) { $repo = (Get-Location).Path }

if (-not $OutDir) { $OutDir = Join-Path $repo 'docs\audit' }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$inicio = Get-Date
$stamp = $inicio.ToString('yyyyMMdd-HHmmss')
$results = New-Object System.Collections.Generic.List[object]

function Get-RepoText([string]$rel) {
    $p = Join-Path $repo $rel
    if (Test-Path -LiteralPath $p) { return (Get-Content -LiteralPath $p -Raw) }
    return $null
}

function Add-Check([string]$id, [string]$desc, [bool]$ok, [string]$detail) {
    $results.Add([pscustomobject]@{ Id = $id; Desc = $desc; Ok = $ok; Detail = $detail })
}

function Test-Has([string]$content, [string]$needle) {
    return $null -ne $content -and $content.Contains($needle)
}

function Test-Lacks([string]$content, [string]$needle) {
    return $null -ne $content -and -not $content.Contains($needle)
}

# ---------------------------------------------------------------------------
# 1) Invariantes de seguridad/auth (checks estáticos)
# ---------------------------------------------------------------------------

$secProps = Get-RepoText 'src/main/java/com/servicioagua/security/SecurityProperties.java'
Add-Check 'A2/A3' 'Sin secretos ni credenciales por defecto (SecurityProperties)' `
    ((Test-Lacks $secProps 'servicio-agua-d3v-2026') -and (Test-Lacks $secProps 'admin123') `
        -and -not ($secProps -match 'jwtSecret\s*=\s*"') `
        -and -not ($secProps -match '(adminPassword|superadminPassword)\s*=\s*"')) `
    'jwtSecret/adminPassword/superadminPassword no deben tener valor literal.'

$appYml = Get-RepoText 'src/main/resources/application.yml'
Add-Check 'A2/A3' 'application.yml sin defaults de secreto/credenciales' `
    ((Test-Lacks $appYml 'servicio-agua-d3v-2026') -and (Test-Lacks $appYml 'admin123') `
        -and (Test-Has $appYml 'jwt-secret: ${JWT_SECRET:}') `
        -and (Test-Has $appYml 'admin-password: ${ADMIN_PASSWORD:}') `
        -and (Test-Has $appYml 'superadmin-password: ${SUPERADMIN_PASSWORD:}')) `
    'El valor por defecto debe quedar vacio y venir del entorno.'

$secCfg = Get-RepoText 'src/main/java/com/servicioagua/security/SecurityConfig.java'
Add-Check 'M5' 'Swagger/OpenAPI gateado por app.security.public-docs' `
    ((Test-Has $secCfg 'publicDocs') -and (Test-Has $secCfg '"/swagger-ui/**"')) `
    'Los matchers de Swagger deben ir dentro del if (publicDocs).'
Add-Check 'M5' 'Health publico (/api/health)' `
    (Test-Has $secCfg '"/api/health"') '/api/health debe ser permitAll para el healthcheck.'
Add-Check 'M6' 'CORS integrado en la cadena de Spring Security' `
    ((Test-Has $secCfg '.cors(') -and (Test-Has $secCfg 'corsConfigurationSource')) `
    'SecurityConfig debe usar .cors(...) con CorsConfigurationSource.'
Add-Check 'M6' 'accessDeniedHandler presente (403 uniforme)' `
    (Test-Has $secCfg '.accessDeniedHandler(') 'Debe responder 403 JSON uniforme.'

$geh = Get-RepoText 'src/main/java/com/servicioagua/common/exception/GlobalExceptionHandler.java'
Add-Check 'M7' 'RateLimitException mapeada a 429' `
    ((Test-Has $geh 'RateLimitException') -and (Test-Has $geh 'TOO_MANY_REQUESTS')) 'Login sin lockout fuerza bruta.'
Add-Check '1' 'AccessDeniedException mapeada a 403' `
    ((Test-Has $geh 'AccessDeniedException') -and (Test-Has $geh 'FORBIDDEN')) 'Denegacion debe ser 403.'
Add-Check '1' 'AuthenticationException mapeada a 401' `
    ((Test-Has $geh 'AuthenticationException') -and (Test-Has $geh 'UNAUTHORIZED')) 'Sin token = 401.'

$loginSvc = Get-RepoText 'src/main/java/com/servicioagua/security/LoginAttemptService.java'
Add-Check 'M7' 'LoginAttemptService existe' ($null -ne $loginSvc -and (Test-Has $loginSvc 'MAX_INTENTOS')) `
    'Debe limitar intentos por usuario.'
$authSvc = Get-RepoText 'src/main/java/com/servicioagua/security/AuthService.java'
Add-Check 'M7' 'AuthService aplica el rate-limit' `
    (Test-Has $authSvc 'verificarBloqueo') 'login() debe llamar a verificarBloqueo.'

$usrCtl = Get-RepoText 'src/main/java/com/servicioagua/security/UsuarioController.java'
Add-Check '1' 'UsuarioController protegido con @PreAuthorize' `
    (Test-Has $usrCtl "hasAnyRole('ADMIN','SUPERADMIN')") 'Endpoints de usuarios requieren rol.'

$usrSvc = Get-RepoText 'src/main/java/com/servicioagua/security/UsuarioService.java'
$gestiones = ([regex]::Matches($usrSvc, 'validarGestionSegura\(')).Count
Add-Check '1' 'resetPassword/cambiarEstado usan validarGestionSegura' ($gestiones -ge 4) `
    "Se esperaban >=4 llamadas (eliminar, cambiarRol, resetPassword, cambiarEstado); hay $gestiones."
Add-Check 'A1' 'Guarda de jerarquia ADMIN vs SUPERADMIN' `
    ((Test-Has $usrSvc 'AccessDeniedException') -and (Test-Has $usrSvc 'Rol.SUPERADMIN')) `
    'Un ADMIN no debe gestionar a un SUPERADMIN.'

$clientJs = Get-RepoText 'frontend/src/api/client.js'
Add-Check 'M14' 'client.js limpia token y usa Bearer' `
    ((Test-Has $clientJs 'auth.clear()') -and (Test-Has $clientJs 'Bearer ${token}')) 'Sesion expirada = logout.'

# 2) Higiene de secretos: .env no versionado
$trackedEnv = @()
try {
    $trackedEnv = @(git -C $repo ls-files | Where-Object { $_ -match '(^|/)\.env(\.|$)' })
} catch { }
Add-Check 'A3' 'Ningun .env versionado en git' ($trackedEnv.Count -eq 0) `
    ("Archivos encontrados: " + ($(if ($trackedEnv.Count) { $trackedEnv -join ', ' } else { 'ninguno' })))

# ---------------------------------------------------------------------------
# 3) Tests de seguridad (MockMvc + H2)
# ---------------------------------------------------------------------------
$testExit = $null
$testLog = $null
if (-not $SkipTests) {
    $mvnw = Join-Path $repo 'mvnw.cmd'
    $testLog = Join-Path $OutDir "security-tests-$stamp.log"
    Write-Host 'Ejecutando tests de seguridad...' -ForegroundColor Cyan
    $eaAnterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $mvnw -q test '-Dtest=SecurityApiTests,UsuarioApiTests' *> $testLog
        $testExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $eaAnterior
    }
    Add-Check 'TESTS' 'Tests SecurityApiTests + UsuarioApiTests' ($testExit -eq 0) `
        "Salida de Maven (exit=$testExit) en $(Split-Path -Leaf $testLog)."
}

# ---------------------------------------------------------------------------
# 4) Informe
# ---------------------------------------------------------------------------
$fallos = @($results | Where-Object { -not $_.Ok })
$veredicto = if ($fallos.Count -eq 0) { 'OK' } else { 'REVISAR' }
$head = ''
try { $head = (git -C $repo rev-parse --short HEAD 2>$null) } catch { }
$dirty = 0
try { $dirty = @(git -C $repo status --porcelain 2>$null).Count } catch { }

$reportPath = Join-Path $OutDir "security-audit-$stamp.md"
$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine('# Auditoria de seguridad / auth')
[void]$sb.AppendLine('')
[void]$sb.AppendLine("- Fecha: $($inicio.ToString('yyyy-MM-dd HH:mm:ss'))")
[void]$sb.AppendLine("- Commit: $head (working tree: $dirty archivos modificados)")
[void]$sb.AppendLine("- Alcance: seguridad/auth (JWT, credenciales, exposicion, CORS, rate-limit, guardas de usuarios)")
[void]$sb.AppendLine("- Veredicto: **$veredicto** ($($results.Count - $fallos.Count)/$($results.Count) en verde)")
[void]$sb.AppendLine('')
[void]$sb.AppendLine('| Check | Resultado | Descripcion | Detalle |')
[void]$sb.AppendLine('|---|---|---|---|')
foreach ($r in $results) {
    $marca = if ($r.Ok) { 'OK' } else { 'FAIL' }
    [void]$sb.AppendLine("| $($r.Id) | $marca | $($r.Desc) | $($r.Detail -replace '\|', '\|') |")
}
if ($testLog -and (Test-Path -LiteralPath $testLog)) {
    [void]$sb.AppendLine('')
    [void]$sb.AppendLine('## Salida de tests (ultimas lineas)')
    [void]$sb.AppendLine('')
    [void]$sb.AppendLine('```')
    [void]$sb.AppendLine((Get-Content -LiteralPath $testLog -Tail 30) -join "`n")
    [void]$sb.AppendLine('```')
}
Set-Content -LiteralPath $reportPath -Value $sb.ToString() -Encoding UTF8
Copy-Item -LiteralPath $reportPath -Destination (Join-Path $OutDir 'latest.md') -Force

Write-Host ''
foreach ($r in $results) {
    $color = if ($r.Ok) { 'Green' } else { 'Red' }
    $txt = if ($r.Ok) { 'OK  ' } else { 'FAIL' }
    Write-Host ("[{0}] {1} - {2}" -f $txt, $r.Id, $r.Desc) -ForegroundColor $color
}
Write-Host ''
Write-Host "Informe: $reportPath" -ForegroundColor Cyan
Write-Host "Veredicto: $veredicto" -ForegroundColor $(if ($veredicto -eq 'OK') { 'Green' } else { 'Red' })

if ($fallos.Count -gt 0) { exit 1 }
exit 0
