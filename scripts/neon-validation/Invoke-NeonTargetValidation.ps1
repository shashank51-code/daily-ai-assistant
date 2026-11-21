<#
.SYNOPSIS
    Guarded validation workflow for the Neon "deployment-validation" branch.

.DESCRIPTION
    Runs ONLY in an interactive PowerShell session that already has
    NEON_DATABASE_URL in its environment. Nothing is persisted to the Windows
    User or Machine environment, and no credential is ever written to a log,
    the console, or the command line.

    Phases (each one self-checks its preconditions, so order mistakes fail safe):

      Preflight  Read-only. Parses the target, confirms host + database,
                 confirms the TLS connection reaches that exact host, and
                 verifies the public schema is empty.
      Restore    Requires -ApproveRestore. Re-runs Preflight, verifies the
                 backup by SHA-256 and archive TOC, then restores with
                 --single-transaction + --exit-on-error. Aborts if the target
                 is not empty.
      Migrate    Requires -ApproveMigrate. Re-checks the restored state,
                 starts the backend with Flyway enabled and
                 ddl-auto=validate (AI disabled, RSS startup fetch disabled),
                 asserts "to version 12" + successful startup, then stops it.
      Verify     Read-only summary of the resulting schema.

.SAFETY GUARANTEES
    * Only $env:NEON_DATABASE_URL is read. DB_URL / DB_* / .env are never used
      as a source, so the original local database cannot be reached by accident.
    * Every database call passes Assert-TargetGuard first: host must equal the
      expected Neon FQDN, database must equal "dailyai", and the URL's sslmode
      must be an encrypted mode. libpq is then forced to sslmode=verify-full
      against the OS/container trust store, so no call can succeed unless the
      server certificate chains to a trusted CA and matches the pinned host.
    * The restore is refused unless the target public schema is empty.
    * The restore is refused unless the backup's SHA-256 matches -ExpectedSha256
      AND the archive is a custom/tar dump that contains flyway_schema_history
      (so Flyway continues at V11 instead of re-baselining).
    * Secrets live only in process-scope PG* environment variables for the
      duration of a single call, and are restored in a finally block. In Docker
      mode only variable NAMES are passed (-e PGHOST ...), never values, so the
      credential never appears in the docker command line or in shell history.
    * AI stays disabled: AI_ENABLED=false and AI_API_KEY is blanked for the app
      run. BOOTSTRAP_EMAIL / BOOTSTRAP_PASSWORD are cleared so no account is
      created or claimed during validation.

.PARAMETER Phase
    One or more of: Preflight, Restore, Migrate, Verify.

.PARAMETER BackupPath
    Path to the verified pre-V12 backup. Must be a pg_dump custom (-Fc) or tar
    archive. Required by Preflight (verification only) and Restore.

.PARAMETER ExpectedSha256
    The pinned SHA-256 of the backup. It must equal $PinnedBackupSha256 below; a hash
    recomputed from the file at runtime is not accepted as the trust anchor.

.PARAMETER ApproveRestore
    Explicit approval gate for the destructive step.

.PARAMETER ApproveMigrate
    Explicit approval gate for the Flyway V12 / app startup step.

.PARAMETER LogDir
    Directory for the run log and row-count snapshots. Defaults to
    <script dir>\logs.

.EXAMPLE
    .\Invoke-NeonTargetValidation.ps1 -Phase Preflight `
        -BackupPath D:\backups\dailyai_pre_v12.dump -ExpectedSha256 ABCD1234...

.EXAMPLE
    .\Invoke-NeonTargetValidation.ps1 -Phase Restore -ApproveRestore `
        -BackupPath D:\backups\dailyai_pre_v12.dump -ExpectedSha256 ABCD1234...
#>

[CmdletBinding()]
param(
    [ValidateSet('Preflight', 'Restore', 'Migrate', 'Verify')]
    [string[]] $Phase = @('Preflight'),

    [string] $BackupPath,
    [string] $ExpectedSha256,

    [switch] $ApproveRestore,
    [switch] $ApproveMigrate,

    [string] $LogDir
)

$ErrorActionPreference = 'Stop'

# All SQL lives in the sibling module so it can be inspected and tested offline.
# It defines only functions; nothing here connects to a database.
. (Join-Path $PSScriptRoot 'NeonValidationSql.ps1')
. (Join-Path $PSScriptRoot 'NeonValidationAppRun.ps1')

# ---------------------------------------------------------------------------
# Constants: the deployment-validation branch under test.
# ---------------------------------------------------------------------------
$ExpectedHost     = 'ep-twilight-wildflower-b34d59ew.c-4.ap-southeast-1.aws.neon.tech'
$ExpectedDatabase = 'dailyai'
$ExpectedPort     = 5432
$FlywayTarget     = 12
$PreV12TableCount = 12          # application tables at V11: V1 (11) + V11 ai_usage_daily
$ExpectedFlywaySequence = '1,2,3,4,5,6,7,8,9,10,11'
$PinnedBackupName   = 'dailyai-local-20261119T122805Z.dump'
$PinnedBackupSha256 = 'f89927278ab152f8a5725f17aef02174e7a729bf7a798ecc113a9ef9e0ee420b'
$DockerImage      = 'postgres:16-alpine'
$AppPort          = 18080
$AppStartTimeout  = 180         # seconds
$RepoRoot         = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

if (-not $LogDir) { $LogDir = Join-Path $PSScriptRoot 'logs' }
$LogDir = [System.IO.Path]::GetFullPath($LogDir)

$script:Target        = $null
$script:ClientMode    = $null
$script:PsqlPath      = $null
$script:PgRestorePath = $null
$script:ClientLabel   = ''
$script:SavedPgEnv    = @{}
$script:SnapshotPath  = Join-Path $LogDir 'counts.before.json'
$script:LogFile       = $null
$script:BackupMountHost = $null
$script:ListMountHost   = $null

$PgEnvVars = @('PGHOST', 'PGPORT', 'PGUSER', 'PGPASSWORD', 'PGDATABASE', 'PGSSLMODE', 'PGSSLROOTCERT', 'PGCONNECT_TIMEOUT', 'PGAPPNAME', 'PGSERVICE', 'PGSERVICEFILE')

# ---------------------------------------------------------------------------
# Output helpers. Nothing here may receive a credential.
# ---------------------------------------------------------------------------
function Initialize-Log {
    if (-not (Test-Path -LiteralPath $LogDir)) {
        New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
    }
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $script:LogFile = Join-Path $LogDir "neon-validation-$stamp.log"
    Add-Content -LiteralPath $script:LogFile -Value "neon deployment-validation run $stamp (phase: $($Phase -join ','))"
}

function Write-Line {
    param([string] $Text, [string] $Kind = 'info')
    $prefix = switch ($Kind) {
        'ok'    { '[ OK ]   ' }
        'warn'  { '[WARN]   ' }
        'fail'  { '[FAIL]   ' }
        'step'  { '[STEP]   ' }
        default { '         ' }
    }
    Write-Host ($prefix + $Text)
    if ($script:LogFile) { Add-Content -LiteralPath $script:LogFile -Value ($prefix + $Text) }
}

function Write-Section {
    param([string] $Title)
    Write-Host ''
    Write-Host ('== ' + $Title + ' ' + ('=' * [Math]::Max(0, 68 - $Title.Length)))
    if ($script:LogFile) { Add-Content -LiteralPath $script:LogFile -Value "`n== $Title ==" }
}

function Stop-Workflow {
    param([string] $Message)
    Write-Line $Message 'fail'
    if ($script:LogFile) { Add-Content -LiteralPath $script:LogFile -Value "STOPPED: $Message" }
    throw $Message
}

function Assert-NoSecretInLog {
    if (-not $script:LogFile -or -not $script:Target) { return }
    if ([string]::IsNullOrEmpty($script:Target.Password)) { return }
    $content = Get-Content -LiteralPath $script:LogFile -Raw -ErrorAction SilentlyContinue
    if ($content -and $content.Contains($script:Target.Password)) {
        Remove-Item -LiteralPath $script:LogFile -Force -ErrorAction SilentlyContinue
        throw 'A credential reached the log file. The log was deleted; the run is invalid.'
    }
}

# ---------------------------------------------------------------------------
# Target parsing / guarding.
# ---------------------------------------------------------------------------
function Get-NeonTarget {
    $raw = [Environment]::GetEnvironmentVariable('NEON_DATABASE_URL', 'Process')
    if ([string]::IsNullOrWhiteSpace($raw)) {
        Stop-Workflow 'NEON_DATABASE_URL is not present in this PowerShell process. Run the script in the interactive session that owns it.'
    }

    $uri = $null
    try { $uri = [Uri]$raw } catch { $uri = $null }
    if (-not $uri -or -not $uri.Host) {
        Stop-Workflow 'NEON_DATABASE_URL is not a parsable postgres:// URL (value not shown).'
    }
    if ($uri.Scheme -notin @('postgres', 'postgresql')) {
        Stop-Workflow 'NEON_DATABASE_URL does not use the postgres:// scheme (value not shown).'
    }

    $userInfo = $uri.UserInfo
    $username = ''
    $password = ''
    if (-not [string]::IsNullOrEmpty($userInfo)) {
        $parts = $userInfo.Split(':', 2)
        $username = [Uri]::UnescapeDataString($parts[0])
        if ($parts.Count -gt 1) { $password = [Uri]::UnescapeDataString($parts[1]) }
    }

    $database = $uri.AbsolutePath.TrimStart('/')
    $port = if ($uri.Port -gt 0) { $uri.Port } else { 5432 }

    $sslMode = 'require'
    if (-not [string]::IsNullOrEmpty($uri.Query)) {
        foreach ($pair in $uri.Query.TrimStart('?').Split('&')) {
            if ([string]::IsNullOrEmpty($pair)) { continue }
            $kv = $pair.Split('=', 2)
            if ($kv[0] -eq 'sslmode' -and $kv.Count -eq 2) { $sslMode = [Uri]::UnescapeDataString($kv[1]) }
        }
    }

    if ([string]::IsNullOrEmpty($username)) {
        Stop-Workflow 'NEON_DATABASE_URL contains no username (value not shown).'
    }

    return [pscustomobject]@{
        Host     = $uri.Host.ToLowerInvariant()
        Port     = $port
        Database = $database
        Username = $username
        Password = $password
        SslMode  = $sslMode.ToLowerInvariant()
    }
}

function Assert-ExpectedTarget {
    param([pscustomobject] $Target)

    if ($Target.Host -ne $ExpectedHost) {
        Stop-Workflow "Target mismatch: connection host '$($Target.Host)' is not the deployment-validation host '$ExpectedHost'."
    }
    if ($Target.Database -ne $ExpectedDatabase) {
        Stop-Workflow "Target mismatch: connection database '$($Target.Database)' is not '$ExpectedDatabase'."
    }
    if ($Target.Port -ne $ExpectedPort) {
        Stop-Workflow "Target mismatch: connection port $($Target.Port) is not $ExpectedPort."
    }
    if ($Target.Host -notmatch '\.neon\.tech$') {
        Stop-Workflow "Refusing: '$($Target.Host)' is not a Neon endpoint."
    }
    if ($Target.SslMode -notin @('require', 'verify-ca', 'verify-full')) {
        Stop-Workflow "Refusing: sslmode '$($Target.SslMode)' is not an encrypted mode."
    }
}

# Re-checked immediately before every database call, not just once at startup.
function Assert-TargetGuard {
    if (-not $script:Target) { Stop-Workflow 'No verified target in context; refusing to run a database command.' }
    Assert-ExpectedTarget -Target $script:Target
}

# ---------------------------------------------------------------------------
# Client tool resolution: native PG16+, otherwise Docker postgres:16.
# ---------------------------------------------------------------------------
function Resolve-PgClient {
    $psql = Get-Command psql -ErrorAction SilentlyContinue
    $pgrestore = Get-Command pg_restore -ErrorAction SilentlyContinue

    if ($psql -and $pgrestore) {
        $verText = ''
        try { $verText = ((& $psql.Source --version 2>&1) -join ' ').Trim() } catch { $verText = '' }
        if ($verText -match '(\d+)\.') {
            $major = [int]$Matches[1]
            if ($major -ge 16) {
                $script:ClientMode = 'Native'
                $script:PsqlPath = $psql.Source
                $script:PgRestorePath = $pgrestore.Source
                $script:ClientLabel = "$verText ($($psql.Source))"
                return
            }
            Write-Line "Native client is older than PostgreSQL 16 ($verText); using Docker instead." 'warn'
        }
    } else {
        Write-Line 'Native psql / pg_restore not found; using Docker instead.' 'warn'
    }

    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $docker) {
        Stop-Workflow 'Neither PostgreSQL 16 client tools nor Docker are available. Install PG16 client tools or start Docker Desktop.'
    }
    & docker info --format '{{.ServerVersion}}' 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Stop-Workflow 'Docker CLI is present but the daemon is not reachable (start Docker Desktop).'
    }
    $script:ClientMode = 'Docker'
    $script:ClientLabel = "docker image $DockerImage"
}

# ---------------------------------------------------------------------------
# Process-scope credential handling. Values are never echoed or persisted.
# ---------------------------------------------------------------------------
function Set-PgEnvironment {
    $script:SavedPgEnv = @{}
    foreach ($name in $PgEnvVars) {
        $script:SavedPgEnv[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    }
    $t = $script:Target
    $env:PGHOST           = $t.Host
    $env:PGPORT           = [string]$t.Port
    $env:PGUSER           = $t.Username
    $env:PGPASSWORD       = $t.Password
    $env:PGDATABASE       = $t.Database
    # Neon terminates TLS at its proxy, so server-side views (pg_stat_ssl,
    # inet_server_addr) describe the proxy->compute leg, not this client. Enforce the
    # strongest mode libpq offers instead: the certificate must chain to the trust store
    # and match the pinned host, or libpq refuses before any SQL is sent.
    $env:PGSSLMODE        = 'verify-full'
    if ($script:ClientMode -eq 'Docker') { $env:PGSSLROOTCERT = '/etc/ssl/certs/ca-certificates.crt' }
    else { $env:PGSSLROOTCERT = 'system' }
    $script:EffectiveCaPath = $env:PGSSLROOTCERT
    $env:PGCONNECT_TIMEOUT = '15'
    $env:PGAPPNAME        = 'dailyai-neon-validation'
    # A libpq service file must not be able to redirect the connection.
    foreach ($name in @('PGSERVICE', 'PGSERVICEFILE')) { Remove-Item -LiteralPath ("Env:" + $name) -ErrorAction SilentlyContinue }
}

function Restore-PgEnvironment {
    foreach ($name in $PgEnvVars) {
        $previous = $script:SavedPgEnv[$name]
        if ($null -eq $previous) { Remove-Item -LiteralPath ("Env:" + $name) -ErrorAction SilentlyContinue }
        else { Set-Item -LiteralPath ("Env:" + $name) -Value $previous }
    }
    $script:SavedPgEnv = @{}
}

# ---------------------------------------------------------------------------
# Docker plumbing. Only NAMES are forwarded, so values stay off the CLI.
# ---------------------------------------------------------------------------
function ConvertTo-ContainerPath {
    param([string] $Path)
    $full = [System.IO.Path]::GetFullPath($Path)
    if ($script:BackupMountHost -and $full.StartsWith($script:BackupMountHost, [StringComparison]::OrdinalIgnoreCase)) {
        return '/backup' + $full.Substring($script:BackupMountHost.Length).Replace('\', '/')
    }
    if ($script:ListMountHost -and $full.StartsWith($script:ListMountHost, [StringComparison]::OrdinalIgnoreCase)) {
        return '/list' + $full.Substring($script:ListMountHost.Length).Replace('\', '/')
    }
    return $full.Replace('\', '/')
}

function Get-DockerArgs {
    $dockerArgs = @('run', '--rm', '-i')
    foreach ($name in $PgEnvVars) { $dockerArgs += @('--env', $name) }
    if ($script:BackupMountHost) {
        $dockerArgs += @('-v', ($script:BackupMountHost.Replace('\', '/') + ':/backup:ro'))
    }
    if ($script:ListMountHost) {
        $dockerArgs += @('-v', ($script:ListMountHost.Replace('\', '/') + ':/list:ro'))
    }
    $dockerArgs += $DockerImage
    return $dockerArgs
}

# ---------------------------------------------------------------------------
# Database calls.
# ---------------------------------------------------------------------------
function Invoke-Sql {
    param([Parameter(Mandatory = $true)][string] $Sql)

    Assert-TargetGuard
    Set-PgEnvironment
    try {
        if ($script:ClientMode -eq 'Native') {
            $output = & $script:PsqlPath -X -q -v ON_ERROR_STOP=1 -A -t -F '|' -c $Sql 2>&1
            $code = $LASTEXITCODE
        } else {
            $dockerArgs = Get-DockerArgs
            $output = & docker @dockerArgs psql -X -q -v ON_ERROR_STOP=1 -A -t -F '|' -c $Sql 2>&1
            $code = $LASTEXITCODE
        }
        if ($code -ne 0) {
            throw ('psql failed with exit code ' + $code + ': ' + (($output | Out-String).Trim()))
        }
        # The leading comma stops PowerShell from unrolling a one-element result into a
        # bare [string] at the caller. Without it, $rows[0] on a single row returns a
        # [char] (the string indexer), not the string.
        return ,@($output | ForEach-Object { [string]$_ } | Where-Object { $_ -ne '' })
    }
    finally {
        Restore-PgEnvironment
    }
}

function Invoke-SqlScalar {
    param([Parameter(Mandatory = $true)][string] $Sql)
    # Normalise to an array before indexing. Invoke-Sql already returns an array, but
    # @() keeps this contract even if a caller hands back a bare string; indexing a
    # [string] yields [System.Char], which has no .Split()/.Trim().
    $rows = @(Invoke-Sql -Sql $Sql)
    if ($rows.Count -eq 0) { return '' }
    $value = $rows[0]
    if ($null -eq $value) { return '' }
    return [string]$value
}

function Invoke-PgRestoreVerb {
    param([Parameter(Mandatory = $true)][string[]] $RestoreArgs)

    Assert-TargetGuard
    Set-PgEnvironment
    try {
        if ($script:ClientMode -eq 'Native') {
            $output = & $script:PgRestorePath @RestoreArgs 2>&1
            $code = $LASTEXITCODE
        } else {
            $mapped = @()
            foreach ($a in $RestoreArgs) {
                if ($a -and -not $a.StartsWith('-') -and (Test-Path -LiteralPath $a)) { $mapped += (ConvertTo-ContainerPath $a) }
                else { $mapped += $a }
            }
            $dockerArgs = Get-DockerArgs
            $output = & docker @dockerArgs pg_restore @mapped 2>&1
            $code = $LASTEXITCODE
        }
        return [pscustomobject]@{ ExitCode = $code; Output = @($output | ForEach-Object { [string]$_ }) }
    }
    finally {
        Restore-PgEnvironment
    }
}

# Client-side TLS attestation. libpq is already pinned to verify-full, so a successful
# connection proves chain + hostname; this probe reports the negotiated parameters from
# the client's own view (psql banner / \conninfo), which is the only view that reflects
# the real client->proxy hop on Neon.
function Test-ClientTls {
    Assert-TargetGuard
    Set-PgEnvironment
    try {
        if ($script:ClientMode -eq 'Native') {
            $output = & $script:PsqlPath -X -A -t -v ON_ERROR_STOP=1 -c '\conninfo' 2>&1
            $code = $LASTEXITCODE
        } else {
            $dockerArgs = Get-DockerArgs
            $output = & docker @dockerArgs psql -X -A -t -v ON_ERROR_STOP=1 -c '\conninfo' 2>&1
            $code = $LASTEXITCODE
        }
    }
    finally {
        Restore-PgEnvironment
    }

    $text = (@($output) | ForEach-Object { [string]$_ }) -join "`n"
    if ($script:Target.Password) { $text = $text.Replace($script:Target.Password, '***') }

    if ($code -ne 0) {
        $detail = ($text -replace '\s+', ' ').Trim()
        if ($detail.Length -gt 400) { $detail = $detail.Substring(0, 400) }
        Stop-Workflow "TLS probe failed (exit $code) under sslmode=verify-full: $detail"
    }
    if ($text -match 'SSL connection \(protocol:\s*(TLSv[0-9.]+),\s*cipher:\s*([^,)\r\n]+)') {
        return [pscustomobject]@{ Reported = $true; Protocol = $Matches[1]; Cipher = $Matches[2].Trim() }
    }
    return [pscustomobject]@{ Reported = $false; Protocol = ''; Cipher = '' }
}

# ---------------------------------------------------------------------------
# Checks.
# ---------------------------------------------------------------------------
# Every statement comes from NeonValidationSql.ps1, which is dot-sourced above and
# covered by tests/Test-NeonValidationSql.ps1.
$SqlEmptinessObjects = Get-NeonSqlEmptinessObjects
$SqlTableCount       = Get-NeonSqlTableCount
$SqlLatestFlyway     = Get-NeonSqlLatestFlyway
$SqlFlywayFailures   = Get-NeonSqlFlywayFailures
$SqlRssTableExists   = Get-NeonSqlRssTableExists

function Get-RowCounts {
    $sql = Get-NeonSqlRowCounts
    $map = @{}
    foreach ($row in (Invoke-Sql -Sql $sql)) {
        $parts = $row.Split('|')
        if ($parts.Count -eq 2) { $map[$parts[0]] = [int64]$parts[1] }
    }
    return $map
}

function Test-Connectivity {
    Write-Section 'Target confirmation (read-only)'

    $resolved = @()
    try {
        $resolved = @([System.Net.Dns]::GetHostAddresses($ExpectedHost) | ForEach-Object { $_.IPAddressToString })
    } catch {
        Stop-Workflow "DNS resolution failed for $ExpectedHost."
    }
    Write-Line ("Host resolves: " + ($resolved -join ', ')) 'ok'

    # Eight pipe-separated fields, built with concat() in NeonValidationSql.ps1.
    # host() strips the inet netmask (e.g. "169.254.254.254/32"), which can never appear
    # in a DNS answer. pg_stat_ssl is diagnostic only: on Neon it describes the
    # proxy->compute leg, so it cannot attest this client's encryption.
    $row = Invoke-SqlScalar -Sql (Get-NeonSqlServerIdentity)
    $f = $row.Split('|')
    if ($f.Count -lt 8) { Stop-Workflow 'Could not read the server identity tuple from the target.' }

    $serverAddr    = $f[0]
    $serverPort    = $f[1]
    $serverDb      = $f[2]
    $serverUser    = $f[3]
    $serverVer     = $f[4]
    $computeSsl    = $f[5]
    $computeTlsVer = $f[6]
    $computeCipher = $f[7]

    Write-Line "Compute-side view: inet_server_addr=$serverAddr port=$serverPort database=$serverDb user=$serverUser server_version=$serverVer"
    Write-Line "Compute-side pg_stat_ssl: ssl=$computeSsl version=$computeTlsVer cipher=$computeCipher (proxy->compute leg; says nothing about this client)"

    # The URL is parsed client-side; the server's own identity tuple is the proof that
    # this session reached the expected database, role and port.
    $identityProblems = @(Get-NeonServerIdentityMismatch -ServerDatabase $serverDb -ServerPort $serverPort -ServerUser $serverUser -ExpectedDatabase $ExpectedDatabase -ExpectedPort $ExpectedPort -ExpectedUser $script:Target.Username)
    if ($identityProblems.Count -gt 0) {
        Stop-Workflow ('Server identity does not match the expected target: ' + ($identityProblems -join '; ') + '.')
    }
    Write-Line "Server identity confirmed: database $serverDb, role $serverUser, port $serverPort." 'ok'

    # Encryption is enforced, not inferred: every call runs under sslmode=verify-full with
    # a real trust store, so this session exists only if the server certificate validated
    # for $ExpectedHost. The probe reports what the client actually negotiated.
    Write-Line "Effective TLS: sslmode=verify-full (URL asked for '$($script:Target.SslMode)'), trust store: $script:EffectiveCaPath"
    $tls = Test-ClientTls
    if (-not $tls.Reported) {
        Stop-Workflow 'The client did not report a TLS session for this connection; refusing because encryption could not be attested.'
    }
    if ($tls.Protocol -notmatch '^TLSv1\.[23]$') {
        Stop-Workflow "Unexpected TLS protocol '$($tls.Protocol)' for $ExpectedHost."
    }
    Write-Line "Client TLS: $($tls.Protocol), cipher $($tls.Cipher), certificate verified for $ExpectedHost" 'ok'

    # Neon's proxy reaches the compute over a link-local link, so the compute legitimately
    # observes 169.254/16 rather than the public endpoint address. Accept exactly two
    # shapes - a real DNS answer, or an RFC 3927 / IPv6 link-local proxy address - and
    # stop on anything else. This is a hard gate, not a warning.
    $addrInDns       = ($resolved -contains $serverAddr)
    $addrIsLinkLocal = ($serverAddr -match '^(?:169\.254\.\d{1,3}\.\d{1,3}|fe80:[0-9a-fA-F:]+)$')
    if (-not ($addrInDns -or $addrIsLinkLocal)) {
        Stop-Workflow "Compute-side address '$serverAddr' is neither a DNS answer for $ExpectedHost nor an RFC 3927 link-local proxy address. Refusing."
    }
    if ($addrIsLinkLocal) {
        Write-Line "Compute-side address $serverAddr is link-local: this session is proxied, and identity is asserted by the verified certificate, not by this address." 'ok'
    }
    else {
        Write-Line "Compute-side address $serverAddr matches a DNS answer for $ExpectedHost." 'ok'
    }

    $serverMajor = 0
    if ($serverVer -match '^(\d+)\.') { $serverMajor = [int]$Matches[1] }
    if ($serverMajor -lt 16) { Write-Line "Server major version $serverMajor is below 16; restore compatibility is not guaranteed." 'warn' }
    else { Write-Line "Server major version $serverMajor" 'ok' }

    return [pscustomobject]@{ Address = $serverAddr; Version = $serverVer; Major = $serverMajor; User = $serverUser; TlsProtocol = $tls.Protocol; TlsCipher = $tls.Cipher }
}

function Test-TargetEmpty {
    Write-Section 'Empty-target gate'

    $objects = Invoke-Sql -Sql $SqlEmptinessObjects
    if ($objects.Count -gt 0) {
        Write-Line "The target public schema is NOT empty ($($objects.Count) object(s) found):" 'fail'
        foreach ($o in ($objects | Select-Object -First 40)) { Write-Line "  - $o" 'fail' }
        if ($objects.Count -gt 40) { Write-Line "  ... $($objects.Count - 40) more" 'fail' }
        Stop-Workflow 'Stopping: the deployment-validation target is not empty. Nothing was restored.'
    }
    Write-Line 'No relations, functions or types outside the system catalogs.' 'ok'

    $tableCount = [int](Invoke-SqlScalar -Sql $SqlTableCount)
    if ($tableCount -ne 0) { Stop-Workflow "Target reports $tableCount table(s); expected 0." }
    Write-Line 'Target contains 0 tables.' 'ok'
}

# ---------------------------------------------------------------------------
# Backup verification.
# ---------------------------------------------------------------------------
function Get-ArchiveListing {
    param([Parameter(Mandatory = $true)][string] $Path)

    $dir = [System.IO.Path]::GetDirectoryName($Path)
    $script:BackupMountHost = $dir

    $result = Invoke-PgRestoreVerb -RestoreArgs @('-l', $Path)
    if ($result.ExitCode -ne 0) {
        Stop-Workflow ("The backup could not be listed by pg_restore, so it is not a custom/tar archive: " + (($result.Output | Out-String).Trim()) + " . Re-dump with 'pg_dump -Fc' - nothing was changed.")
    }
    return $result.Output
}

function Assert-BackupVerified {
    Write-Section 'Backup verification'

    if ([string]::IsNullOrWhiteSpace($BackupPath)) { Stop-Workflow '-BackupPath is required for this phase.' }
    if ([string]::IsNullOrWhiteSpace($ExpectedSha256)) { Stop-Workflow '-ExpectedSha256 is required; the backup is not restored without a known-good hash.' }
    if (-not (Test-Path -LiteralPath $BackupPath -PathType Leaf)) { Stop-Workflow "Backup not found: $BackupPath" }

    $full = (Resolve-Path -LiteralPath $BackupPath).Path
    if ([System.IO.Path]::GetFileName($full) -ne $PinnedBackupName) {
        Write-Line "Backup file name '$([System.IO.Path]::GetFileName($full))' differs from the pinned artifact '$PinnedBackupName'." 'warn'
    }
    $length = (Get-Item -LiteralPath $full).Length
    if ($length -le 0) { Stop-Workflow "Backup is empty: $full" }

    $actual = (Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash.ToUpperInvariant()
    $expected = ($ExpectedSha256 -replace '[^0-9A-Fa-f]', '').ToUpperInvariant()
    if ($expected -ne $PinnedBackupSha256) {
        Stop-Workflow 'The supplied -ExpectedSha256 is not the pinned hash for this validation run. Refusing to trust it.'
    }
    Write-Line "Backup: $full ($length bytes)"
    Write-Line "SHA-256 actual  : $actual"
    Write-Line "SHA-256 expected: $expected"
    if ($actual -ne $expected) {
        Stop-Workflow 'Backup SHA-256 does not match. Refusing to restore (nothing was changed).'
    }
    Write-Line 'Backup hash verified.' 'ok'

    $toc = Get-ArchiveListing -Path $full
    $text = ($toc | Out-String)

    $dumpDb = ''
    $dumpFormat = ''
    $dumpVersion = ''
    foreach ($line in $toc) {
        if ($line -match ';\s*dbname:\s*(\S+)')        { $dumpDb = $Matches[1] }
        if ($line -match ';\s*Format:\s*(\S+)')        { $dumpFormat = $Matches[1] }
        if ($line -match 'Dumped from database version:\s*(\S+)') { $dumpVersion = $Matches[1] }
    }
    Write-Line "Archive format=$dumpFormat dbname=$dumpDb dumped-from=$dumpVersion"

    if ($dumpDb -and $dumpDb -ne $ExpectedDatabase) {
        Stop-Workflow "The backup was taken from database '$dumpDb', not '$ExpectedDatabase'. Refusing to restore."
    }
    if ($dumpVersion -and $dumpVersion -notmatch '^(\d+)\.') {
        Write-Line "Unrecognised dump source version '$dumpVersion'." 'warn'
    }

    if ($text -notmatch '(?m)\sTABLE public users(\s|$)') {
        Stop-Workflow 'The archive does not contain table public.users; it does not look like a dailyai backup.'
    }
    if ($text -notmatch '(?m)\sTABLE public flyway_schema_history(\s|$)') {
        Stop-Workflow 'The archive has no flyway_schema_history table. Flyway would re-baseline and replay every migration; refusing.'
    }
    if ($text -notmatch '(?m)\sTABLE DATA public flyway_schema_history(\s|$)') {
        Stop-Workflow 'The archive has no flyway_schema_history data, so the applied-migration history would be lost; refusing.'
    }
    if ($text -match '(?m)\sEXTENSION - ') {
        Write-Line 'Archive contains CREATE EXTENSION entries; these may need to be enabled in the Neon console first.' 'warn'
    }
    Write-Line 'Archive contains users + flyway_schema_history schema and data.' 'ok'

    return [pscustomobject]@{ Path = $full; Toc = $toc; Sha256 = $actual }
}

# ---------------------------------------------------------------------------
# Phase: Preflight
# ---------------------------------------------------------------------------
function Invoke-PreflightPhase {
    $identity = Test-Connectivity
    Test-TargetEmpty
    if ($BackupPath -or $ExpectedSha256) {
        $null = Assert-BackupVerified
    } else {
        Write-Line 'Backup not supplied, so only target state was checked.' 'warn'
    }
    Write-Section 'Preflight result'
    Write-Line "Target confirmed: $ExpectedHost / $ExpectedDatabase (server $($identity.Version)), schema empty." 'ok'
    Write-Line 'No data was written. Nothing was restored or migrated.' 'ok'
}

# ---------------------------------------------------------------------------
# Phase: Restore
# ---------------------------------------------------------------------------
function Invoke-RestorePhase {
    if (-not $ApproveRestore) {
        Stop-Workflow 'Restore phase requires -ApproveRestore. Review the Preflight output first.'
    }

    $null = Test-Connectivity
    Test-TargetEmpty
    $backup = Assert-BackupVerified

    Write-Section 'Restore into the verified target'

    # pg_dump always emits an entry for the public schema; Neon already has one.
    # Excluding just that entry keeps --exit-on-error honest without --clean.
    $listDir = Join-Path $env:TEMP ('neon-restore-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $listDir -Force | Out-Null
    $listFile = Join-Path $listDir 'restore.list'
    $script:ListMountHost = $listDir

    try {
        $filtered = @()
        foreach ($line in $backup.Toc) {
            if ($line -match '^(;|$)') { $filtered += $line; continue }
            if ($line -match '\sSCHEMA - public(\s|$)')         { continue }
            if ($line -match '\sCOMMENT - SCHEMA public(\s|$)') { continue }
            if ($line -match '\s(ACL|DEFAULT ACL) - ')         { continue }
            $filtered += $line
        }
        if ($filtered.Count -le 1) { Stop-Workflow 'The filtered restore list is empty; refusing to restore.' }
        # Written without a BOM: pg_restore's -L parser must see ";" as the first byte.
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText($listFile, ($filtered -join "`r`n"), $utf8NoBom)
        Write-Line "Restore list: $($filtered.Count) lines (public schema entry excluded)."

        Write-Line 'Running pg_restore --single-transaction --exit-on-error --no-owner --no-privileges ...'
        $result = Invoke-PgRestoreVerb -RestoreArgs @(
            '--no-owner', '--no-privileges', '--no-comments',
            '--single-transaction', '--exit-on-error',
            '-L', $listFile,
            '-d', $script:Target.Database,
            $backup.Path
        )
        if ($result.ExitCode -ne 0) {
            $tail = ($result.Output | Select-Object -Last 25 | Out-String).Trim()
            Write-Line $tail 'fail'
            Stop-Workflow "pg_restore failed with exit code $($result.ExitCode). The --single-transaction run was rolled back; the target is unchanged."
        }
        foreach ($line in $result.Output) {
            if ($line -match '^(pg_restore: )?error') { Write-Line $line 'warn' }
        }
        Write-Line 'pg_restore completed with --exit-on-error and no rollback.' 'ok'
    }
    finally {
        $script:ListMountHost = $null
        Remove-Item -LiteralPath $listDir -Recurse -Force -ErrorAction SilentlyContinue
    }

    Write-Section 'Post-restore checks'
    $tableCount = [int](Invoke-SqlScalar -Sql $SqlTableCount)
    Write-Line "Application tables in public: $tableCount (expected $PreV12TableCount; Flyway history excluded)"
    if ($tableCount -ne $PreV12TableCount) { Stop-Workflow "Expected $PreV12TableCount tables after restore, found $tableCount." }

    $latest = (Invoke-SqlScalar -Sql $SqlLatestFlyway).Trim()
    Write-Line "Latest Flyway version in restored history: $latest (expected 11)"
    if ($latest -ne '11') { Stop-Workflow "Restored history ends at '$latest', not 11. Refusing to continue." }

    $failures = [int](Invoke-SqlScalar -Sql $SqlFlywayFailures)
    if ($failures -ne 0) { Stop-Workflow "$failures failed migration(s) in the restored history." }
    Write-Line 'All restored Flyway history rows are successful.' 'ok'

    # latest=11 plus zero failures does not prove every version landed: a gap or a
    # duplicate would still pass. Compare the applied order instead.
    $sequence = (Invoke-SqlScalar -Sql (Get-NeonSqlFlywayVersionSequence)).Trim()
    Write-Line "Flyway versions applied, in order: $sequence"
    if ($sequence -ne $ExpectedFlywaySequence) {
        Stop-Workflow "Restored Flyway history is '$sequence', not the expected '$ExpectedFlywaySequence'. Refusing to continue."
    }
    Write-Line 'Flyway V1-V11 are all present and successful, in applied order.' 'ok'

    $rssExists = (Invoke-SqlScalar -Sql $SqlRssTableExists).Trim()
    Write-Line "rss_refresh_runs present: $rssExists (expected f before V12)"
    if ($rssExists -ne 'f') { Stop-Workflow 'rss_refresh_runs already exists; the target is not at pre-V12 state.' }

    $counts = Get-RowCounts
    $counts | ConvertTo-Json | Set-Content -LiteralPath $script:SnapshotPath -Encoding utf8
    Write-Line "Row counts recorded to $($script:SnapshotPath):"
    foreach ($key in ($counts.Keys | Sort-Object)) { Write-Line ("  {0,-22} {1}" -f $key, $counts[$key]) }

    if ($counts['users'] -lt 1) { Stop-Workflow 'users is empty after restore; the backup looks wrong.' }

    # Documented baseline from the source database at dump time. A mismatch means
    # the restore did not land the expected dataset, so stop before V12.
    $baseline = [ordered]@{
        users               = 1
        bookmarks           = 0
        reading_history     = 4
        learning_progress   = 1
        daily_task_progress = 4
    }
    $baselineMismatch = @()
    foreach ($key in $baseline.Keys) {
        $actual = $(if ($counts.ContainsKey($key)) { $counts[$key] } else { $null })
        if ($null -eq $actual -or [int64]$actual -ne [int64]$baseline[$key]) {
            $baselineMismatch += "$key=$actual (documented $($baseline[$key]))"
            Write-Line "Baseline count mismatch: $key is $actual, documented $($baseline[$key])." 'fail'
        }
        else {
            Write-Line "Baseline count matches: $key = $actual." 'ok'
        }
    }
    if ($baselineMismatch.Count -gt 0) {
        Stop-Workflow ('Documented baseline row counts do not match after restore: ' + ($baselineMismatch -join '; ') + '. Flyway V12 was NOT applied.')
    }
    Write-Line 'Documented baseline row counts verified.' 'ok'

    Write-Section 'Restore result'
    Write-Line "Restored $ExpectedDatabase at Flyway V11 into $ExpectedHost." 'ok'
    Write-Line 'Verified against the documented baseline: users=1, bookmarks=0, reading_history=4, learning_progress=1, daily_task_progress=4.' 'ok'
    Write-Line 'Flyway V12 has NOT been applied yet.' 'ok'
}

# ---------------------------------------------------------------------------
# Phase: Migrate (Flyway V12 + Hibernate validate)
# ---------------------------------------------------------------------------
function Get-BackendTooling {
    $jdkDir = Join-Path $RepoRoot '.tools\jdk-17'
    $mvnDir = Join-Path $RepoRoot '.tools\maven'
    $javaHome = $null
    $mvn = $null

    if (Test-Path -LiteralPath $jdkDir) {
        $candidate = Get-ChildItem -LiteralPath $jdkDir -Directory | Select-Object -First 1
        if ($candidate) { $javaHome = $candidate.FullName }
    }
    if (-not $javaHome) {
        $javaCmd = Get-Command java -ErrorAction SilentlyContinue
        if ($javaCmd) { $javaHome = Split-Path -Parent (Split-Path -Parent $javaCmd.Source) }
    }
    if (-not $javaHome -or -not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe'))) {
        Stop-Workflow 'No JDK 17 found (expected .tools\jdk-17 or java on PATH).'
    }

    if (Test-Path -LiteralPath $mvnDir) {
        $candidate = Get-ChildItem -LiteralPath $mvnDir -Directory | Select-Object -First 1
        if ($candidate) { $mvn = Join-Path $candidate.FullName 'bin\mvn.cmd' }
    }
    if (-not $mvn -or -not (Test-Path -LiteralPath $mvn)) {
        $mvnCmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
        if ($mvnCmd) { $mvn = $mvnCmd.Source }
    }
    if (-not $mvn) { Stop-Workflow 'No Maven found (expected .tools\maven or mvn on PATH).' }

    return [pscustomobject]@{ JavaHome = $javaHome; Maven = $mvn }
}

function Invoke-MigratePhase {
    if (-not $ApproveMigrate) {
        Stop-Workflow 'Migrate phase requires -ApproveMigrate. Review the Restore output first.'
    }

    Assert-TargetGuard
    $null = Test-Connectivity

    Write-Section 'Pre-migration gate'
    $latest = (Invoke-SqlScalar -Sql $SqlLatestFlyway).Trim()
    $rssExists = (Invoke-SqlScalar -Sql $SqlRssTableExists).Trim()
    if ($latest -ne '11') { Stop-Workflow "Expected Flyway history at 11 before migrating; found '$latest'." }
    if ($rssExists -ne 'f') { Stop-Workflow 'rss_refresh_runs already exists; V12 appears to be applied.' }
    $tableCount = [int](Invoke-SqlScalar -Sql $SqlTableCount)
    if ($tableCount -ne $PreV12TableCount) { Stop-Workflow "Expected $PreV12TableCount tables before migrating; found $tableCount." }
    Write-Line 'Restored state confirmed: Flyway 11, 12 tables, no rss_refresh_runs.' 'ok'

    $countsBefore = Get-RowCounts

    $tools = Get-BackendTooling
    $backendPom = Join-Path $RepoRoot 'backend\pom.xml'

    Write-Section 'Build backend jar'
    $env:JAVA_HOME = $tools.JavaHome
    $env:Path = (Join-Path $tools.JavaHome 'bin') + ';' + $env:Path
    & $tools.Maven -q -B -f $backendPom -DskipTests -Dmaven.test.skip=true package
    if ($LASTEXITCODE -ne 0) { Stop-Workflow "Maven build failed with exit code $LASTEXITCODE." }

    $jar = Get-ChildItem -LiteralPath (Join-Path $RepoRoot 'backend\target') -Filter 'daily-ai-assistant-*.jar' |
        Where-Object { $_.Name -notlike '*.original' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) { Stop-Workflow 'Backend jar not found after the build.' }
    Write-Line "Built $($jar.Name)" 'ok'

    Write-Section 'Start backend: Flyway migrate + Hibernate validate'
    Write-Line 'AI_ENABLED=false, AI_API_KEY cleared, RSS_FETCH_ON_STARTUP=false, scheduling disabled, BOOTSTRAP_* cleared.'

    $outLog = Join-Path $LogDir ("app-migrate-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + ".out.log")
    $errLog = Join-Path $LogDir ("app-migrate-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + ".err.log")

    # Certificate verification is never inherited from the source URL: force verify-full
    # so the JVM refuses an unverified or wrongly-named certificate, exactly like psql.
    $jdbcUrl = ('jdbc:postgresql://' + $script:Target.Host + ':' + $script:Target.Port + '/' + $script:Target.Database + '?sslmode=verify-full')
    if ($jdbcUrl -notlike ('*' + $ExpectedHost + '*' + $ExpectedDatabase + '*sslmode=verify-full*')) {
        Stop-Workflow 'Refusing to start: the derived DB_URL is not the verified Neon target with verify-full TLS.'
    }

    $javaBin = Join-Path $tools.JavaHome 'bin'
    $appEnvironment = [ordered]@{
        JAVA_HOME                          = $tools.JavaHome
        PATH                               = ($javaBin + ';' + [Environment]::GetEnvironmentVariable('PATH', 'Process'))
        DB_URL                             = $jdbcUrl
        DB_USERNAME                        = $script:Target.Username
        DB_PASSWORD                        = $script:Target.Password
        # Spring Boot binds spring.datasource.* from these and they would override
        # ${DB_URL} in application.yml, so pin them to the same verified target.
        SPRING_DATASOURCE_URL              = $jdbcUrl
        SPRING_DATASOURCE_USERNAME         = $script:Target.Username
        SPRING_DATASOURCE_PASSWORD         = $script:Target.Password
        PORT                               = [string]$AppPort
        SERVER_PORT                        = [string]$AppPort
        SPRING_FLYWAY_ENABLED              = 'true'
        SPRING_FLYWAY_BASELINE_ON_MIGRATE  = 'false'
        SPRING_JPA_HIBERNATE_DDL_AUTO      = 'validate'
        # @EnableScheduling bypasses the auto-configured TaskScheduler, so NewsFetchScheduler
        # is itself @ConditionalOnProperty on this key; false means the bean is not created.
        SPRING_TASK_SCHEDULING_ENABLED     = 'false'
        AI_ENABLED                         = 'false'
        AI_API_KEY                         = ''
        RSS_FETCH_ON_STARTUP               = 'false'
        BOOTSTRAP_EMAIL                    = ''
        BOOTSTRAP_PASSWORD                 = ''
        BOOTSTRAP_ROLE                     = ''
    }

    # The values exist process-wide only around Start-Process and are restored in a
    # finally block, including when startup throws, so nothing leaks to the operator.
    $process = Invoke-WithAppEnvironment -Variables $appEnvironment -Script {
        Start-Process -FilePath (Join-Path $tools.JavaHome 'bin\java.exe') `
            -ArgumentList @('-jar', $jar.FullName) `
            -RedirectStandardOutput $outLog -RedirectStandardError $errLog `
            -PassThru -NoNewWindow
    }

    $deadline = (Get-Date).AddSeconds($AppStartTimeout)
    $started = $false
    $exited = $false
    try {
        while ((Get-Date) -lt $deadline) {
            Start-Sleep -Seconds 2
            if ($process.HasExited) { $exited = $true; break }
            $text = ''
            if (Test-Path -LiteralPath $outLog) { $text = (Get-Content -LiteralPath $outLog -Raw -ErrorAction SilentlyContinue) }
            if ($text -and $text -match 'Started DailyAiAssistantApplication') { $started = $true; break }
        }
    }
    finally {
        # Always reap the JVM: also on Ctrl+C or an exception raised while polling.
        if ($process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            Wait-Process -Id $process.Id -Timeout 30 -ErrorAction SilentlyContinue
            Write-Line 'Backend stopped.'
        }
    }

    $appText = ''
    if (Test-Path -LiteralPath $outLog) { $appText = (Get-Content -LiteralPath $outLog -Raw -ErrorAction SilentlyContinue) }
    $errText = ''
    if (Test-Path -LiteralPath $errLog) { $errText = (Get-Content -LiteralPath $errLog -Raw -ErrorAction SilentlyContinue) }

    if (-not $started) {
        $reason = 'startup did not complete within ' + $AppStartTimeout + 's'
        if ($exited) { $reason = 'the process exited early (code ' + $process.ExitCode + ')' }
        foreach ($pattern in 'Schema-validation', 'SchemaManagementException', 'FlywayException', 'Migration V12', 'Caused by') {
            $hits = @()
            if ($appText) { $hits += ($appText -split "`n" | Where-Object { $_ -match $pattern } | Select-Object -First 5) }
            if ($errText) { $hits += ($errText -split "`n" | Where-Object { $_ -match $pattern } | Select-Object -First 5) }
            foreach ($hit in $hits) { Write-Line $hit.Trim() 'fail' }
        }
        Stop-Workflow "Migration/validation run failed: $reason. See $outLog"
    }

    Write-Section 'Migration and schema-validation assertions'
    $migrated = ($appText -match 'to version "12')
    $validated = ($appText -match 'Started DailyAiAssistantApplication') -and ($appText -notmatch 'Schema-validation:')
    Write-Line ("Flyway applied V12 (log marker 'to version ""12'): " + $migrated)
    Write-Line ("Application started with ddl-auto=validate and no schema-validation error: " + $validated)
    if (-not $migrated) {
        $flywayLines = ($appText -split "`n" | Where-Object { $_ -match 'Flyway|Migrating schema|Successfully applied|Current version' } | Select-Object -First 15)
        foreach ($line in $flywayLines) { Write-Line $line.Trim() }
        Stop-Workflow 'Flyway did not report applying version 12.'
    }
    if (-not $validated) { Stop-Workflow 'Hibernate schema validation did not pass (see the app log).' }

    Write-Section 'Post-migration checks'
    $latest = (Invoke-SqlScalar -Sql $SqlLatestFlyway).Trim()
    Write-Line "Latest Flyway version: $latest (expected 12)"
    if ($latest -ne '12') { Stop-Workflow "Flyway history ends at '$latest', not 12." }

    $rssExists = (Invoke-SqlScalar -Sql $SqlRssTableExists).Trim()
    if ($rssExists -ne 't') { Stop-Workflow 'rss_refresh_runs was not created by V12.' }
    Write-Line 'rss_refresh_runs exists.' 'ok'

    $countsAfter = Get-RowCounts
    $drift = @(Get-NeonRowCountDrift -Before $countsBefore -After $countsAfter)
    if ($drift.Count -gt 0) {
        foreach ($d in $drift) { Write-Line "Row-count drift $d" 'fail' }
    }
    # Hard failure: V12 is additive, so any change means something else wrote to the
    # target during the run and this validation snapshot cannot be trusted.
    try {
        Assert-NeonRowCountsUnchanged -Before $countsBefore -After $countsAfter
        Write-Line 'Row counts unchanged across V12 for all 12 documented tables.' 'ok'
    }
    catch {
        Stop-Workflow ($_.Exception.Message + ' Refusing to accept this validation run; Flyway V12 may still have been applied.')
    }

    $runs = (Invoke-SqlScalar -Sql (Get-NeonSqlRssRefreshCount)).Trim()
    Write-Line "rss_refresh_runs rows after validation: $runs (0 expected; a non-zero value means a scheduled tick fired)"
    Write-Line 'AI was disabled for this run (AI_ENABLED=false, blank AI_API_KEY).' 'ok'
}

# ---------------------------------------------------------------------------
# Phase: Verify
# ---------------------------------------------------------------------------
function Invoke-VerifyPhase {
    Assert-TargetGuard
    Write-Section 'Schema verification (read-only)'

    $rows = Invoke-Sql -Sql (Get-NeonSqlFlywayHistory)
    Write-Line "Flyway history rows: $($rows.Count)"
    foreach ($r in $rows) { Write-Line "  $r" }
    if ($rows.Count -ne $FlywayTarget) { Write-Line "Expected $FlywayTarget history rows." 'warn' }

    $columns = Invoke-Sql -Sql (Get-NeonSqlRssColumns)
    Write-Line "rss_refresh_runs columns ($($columns.Count)):"
    foreach ($c in $columns) { Write-Line "  $c" }

    $indexes = Invoke-Sql -Sql (Get-NeonSqlRssIndexes)
    Write-Line "rss_refresh_runs indexes ($($indexes.Count)):"
    foreach ($i in $indexes) { Write-Line "  $i" }

    $tableCount = [int](Invoke-SqlScalar -Sql $SqlTableCount)
    Write-Line "Application tables in public: $tableCount (expected $($PreV12TableCount + 1) after V12)"
    $counts = Get-RowCounts
    Write-Line 'Row counts:'
    foreach ($key in ($counts.Keys | Sort-Object)) { Write-Line ("  {0,-22} {1}" -f $key, $counts[$key]) }

    Write-Section 'Verify result'
    Write-Line "Flyway at $($rows.Count) rows; rss_refresh_runs present with $($columns.Count) columns and $($indexes.Count) indexes." 'ok'
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
Initialize-Log

Write-Section 'Environment'
$script:Target = Get-NeonTarget
Assert-ExpectedTarget -Target $script:Target
Write-Line "NEON_DATABASE_URL present in this process." 'ok'
Write-Line "Target host     : $($script:Target.Host)"
Write-Line "Target database : $($script:Target.Database)"
Write-Line "Target port     : $($script:Target.Port)"
Write-Line "Target user     : $($script:Target.Username)"
Write-Line "Target sslmode  : $($script:Target.SslMode)"
Write-Line 'Credential      : (present, not displayed)'
Write-Line "Log file        : $($script:LogFile)"

Resolve-PgClient
Write-Line "Client tools    : $($script:ClientLabel)" 'ok'

try {
    foreach ($p in $Phase) {
        switch ($p) {
            'Preflight' { Invoke-PreflightPhase }
            'Restore'   { Invoke-RestorePhase }
            'Migrate'   { Invoke-MigratePhase }
            'Verify'    { Invoke-VerifyPhase }
        }
    }
}
finally {
    Assert-NoSecretInLog
}

Write-Section 'Summary'
Write-Line ("Phases executed: " + ($Phase -join ', ')) 'ok'
Write-Line 'No credential was printed, logged, or persisted.'
Write-Line 'Production and the original local database were never contacted: only the verified Neon target was used.'
