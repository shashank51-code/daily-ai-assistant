#Requires -Version 5.1
<#
.SYNOPSIS
    Offline tests for the migration-safety safeguards.

.DESCRIPTION
    Covers the helpers that the Migrate phase actually calls, plus static wiring checks
    on the workflow script and the application configuration. Nothing here connects to a
    database, starts the application, runs Flyway, or touches a credential.

    The environment tests use throw-away DAILYAI_TEST_* variables only, and every test
    restores whatever it changed.

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\Test-NeonValidationSafeguards.ps1
#>

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$root        = Split-Path -Parent $PSScriptRoot
$helperPath  = Join-Path $root 'NeonValidationAppRun.ps1'
$scriptPath  = Join-Path $root 'Invoke-NeonTargetValidation.ps1'
$appYmlPath  = Join-Path $root '..\..\backend\src\main\resources\application.yml'
$schedulerPath = Join-Path $root '..\..\backend\src\main\java\com\dailyai\assistant\scheduler\NewsFetchScheduler.java'
$javaRoot    = Join-Path $root '..\..\backend\src\main\java'

foreach ($p in @($helperPath, $scriptPath, $appYmlPath, $schedulerPath)) {
    if (-not (Test-Path -LiteralPath $p -PathType Leaf)) { throw "Missing file: $p" }
}

. $helperPath

$script:Passed   = 0
$script:Failed   = 0
$script:Failures = New-Object System.Collections.Generic.List[string]

function Write-Group {
    param([string] $Title)
    Write-Host ''
    Write-Host ("-- " + $Title)
}

function Assert-True {
    param($Condition, [string] $Because)
    if ($Condition) {
        $script:Passed++
        Write-Host ("   PASS  " + $Because)
    }
    else {
        $script:Failed++
        $script:Failures.Add($Because)
        Write-Host ("   FAIL  " + $Because)
    }
}

function Clear-TestVariable {
    param([string] $Name)
    [Environment]::SetEnvironmentVariable($Name, $null, 'Process')
}

function Get-TestVariable {
    param([string] $Name)
    return [Environment]::GetEnvironmentVariable($Name, 'Process')
}

# ---------------------------------------------------------------------------
# Environment application and restoration.
# ---------------------------------------------------------------------------
Write-Group 'Invoke-WithAppEnvironment: values and restoration'

$alpha = 'DAILYAI_TEST_ALPHA'
$beta = 'DAILYAI_TEST_BETA'
$gamma = 'DAILYAI_TEST_GAMMA'
foreach ($n in @($alpha, $beta, $gamma)) { Clear-TestVariable $n }

[Environment]::SetEnvironmentVariable($alpha, 'original', 'Process')

$seenInside = Invoke-WithAppEnvironment -Variables @{ $alpha = 'temporary' } -Script { Get-TestVariable $alpha }
Assert-True ($seenInside -eq 'temporary') 'applies the override for the duration of the block'
Assert-True ((Get-TestVariable $alpha) -eq 'original') 'restores the previous value afterwards'

$nullInside = Invoke-WithAppEnvironment -Variables @{ $beta = 'created' } -Script { Get-TestVariable $beta }
Assert-True ($nullInside -eq 'created') 'creates a value that was unset'
Assert-True ($null -eq (Get-TestVariable $beta)) 'removes a value that did not exist before'

$childSaw = Invoke-WithAppEnvironment -Variables @{ $gamma = 'child-visible' } -Script {
    (& powershell -NoProfile -Command 'if ($env:DAILYAI_TEST_GAMMA) { $env:DAILYAI_TEST_GAMMA }') | Out-String
}
Assert-True ($childSaw.Trim() -eq 'child-visible') 'a child process inherits the applied value (real process environment)'
Assert-True ($null -eq (Get-TestVariable $gamma)) 'the child-visible value is removed afterwards'

$passthrough = Invoke-WithAppEnvironment -Variables @{ $alpha = 'x' } -Script { 42 }
Assert-True ($passthrough -eq 42) 'passes the block output through'

$closureProbe = 'closure-ok'
$closureResult = Invoke-WithAppEnvironment -Variables @{ $alpha = 'y' } -Script { $closureProbe }
Assert-True ($closureResult -eq 'closure-ok') 'the block can read variables from the calling scope (used by the Migrate phase)'

# The Migrate phase passes a block that reads $tools / $jar, which are locals of a
# function. Pin that closure behaviour rather than assuming it.
function Invoke-LocalCaptureProbe {
    $probeLocal = 'local-ok'
    return Invoke-WithAppEnvironment -Variables @{ $alpha = 'z' } -Script { $probeLocal }
}
$localCapture = Invoke-LocalCaptureProbe
Assert-True ($localCapture -eq 'local-ok') 'the block can read variables local to the calling function (Migrate phase pattern)'

Write-Group 'Invoke-WithAppEnvironment: failure paths'

$thrown = $null
try {
    Invoke-WithAppEnvironment -Variables @{ $alpha = 'temporary'; $beta = 'created' } -Script {
        throw 'simulated startup failure'
    } | Out-Null
}
catch { $thrown = $_ }

Assert-True ($thrown -and $thrown.Exception.Message -eq 'simulated startup failure') 'propagates the failure from the block'
Assert-True ((Get-TestVariable $alpha) -eq 'original') 'restores the previous value when the block throws'
Assert-True ($null -eq (Get-TestVariable $beta)) 'removes a newly created value when the block throws'

# A block that mutates a different variable before failing must still leave the
# variables the helper owns exactly as they were.
[Environment]::SetEnvironmentVariable($beta, 'pre-existing', 'Process')
$thrown = $null
try {
    Invoke-WithAppEnvironment -Variables @{ $alpha = 'temporary'; $beta = 'overwritten' } -Script {
        throw 'second failure'
    } | Out-Null
}
catch { $thrown = $_ }
Assert-True ((Get-TestVariable $beta) -eq 'pre-existing') 'restores a pre-existing value even after a mid-block failure'
Assert-True ($thrown.Exception.Message -eq 'second failure') 'second failure propagated unchanged'

$thrown = $null
try {
    Invoke-WithAppEnvironment -Variables @{ $alpha = 'temporary' } -Script { Get-Command 'definitely-not-a-command-xyz' -ErrorAction Stop | Out-Null } | Out-Null
}
catch { $thrown = $_ }
Assert-True ($null -ne $thrown) 'propagates a real command error from the block'
Assert-True ((Get-TestVariable $alpha) -eq 'original') 'restores the value when the block fails on a missing command'

foreach ($n in @($alpha, $beta, $gamma)) { Clear-TestVariable $n }

# ---------------------------------------------------------------------------
# Row-count drift must be a hard failure.
# ---------------------------------------------------------------------------
Write-Group 'Row-count drift'

$before = @{ users = 1; articles = 10; bookmarks = 0; reading_history = 4 }
$same   = @{ users = 1; articles = 10; bookmarks = 0; reading_history = 4 }
$changed = @{ users = 2; articles = 10; bookmarks = 0; reading_history = 4 }
$missing = @{ users = 1; articles = 10; bookmarks = 0 }

$noDrift = @(Get-NeonRowCountDrift -Before $before -After $same)
Assert-True ($noDrift.Count -eq 0) 'identical snapshots produce no drift'

$oneDrift = @(Get-NeonRowCountDrift -Before $before -After $changed)
Assert-True ($oneDrift.Count -eq 1) 'a single changed table produces one drift entry'
Assert-True ($oneDrift[0] -match 'users: 1 -> 2') 'the drift entry names the table and both counts'

$decrease = @(Get-NeonRowCountDrift -Before $before -After @{ users = 0; articles = 10; bookmarks = 0; reading_history = 4 })
Assert-True ($decrease.Count -eq 1 -and $decrease[0] -match 'users: 1 -> 0') 'a decrease is drift too'

$missingDrift = @(Get-NeonRowCountDrift -Before $before -After $missing)
Assert-True ($missingDrift.Count -eq 1) 'a table missing from the after-snapshot counts as drift'
Assert-True ($missingDrift[0] -match 'reading_history: 4 -> missing') 'the missing table is reported explicitly'

$multiple = @(Get-NeonRowCountDrift -Before $before -After @{ users = 9; articles = 10; bookmarks = 3; reading_history = 4 })
Assert-True ($multiple.Count -eq 2) 'every drifted table is reported, not just the first'

$noThrow = $true
try { Assert-NeonRowCountsUnchanged -Before $before -After $same } catch { $noThrow = $false }
Assert-True $noThrow 'unchanged counts pass the assertion'

$threw = $false
try { Assert-NeonRowCountsUnchanged -Before $before -After $changed } catch { $threw = $true }
Assert-True $threw 'changed counts throw, which aborts the Migrate phase with a non-zero exit'

$threw = $false
$message = ''
try { Assert-NeonRowCountsUnchanged -Before $before -After $changed } catch { $threw = $true; $message = $_.Exception.Message }
Assert-True ($threw -and $message -match 'users: 1 -> 2') 'the failure message names the drifted table'
Assert-True ($threw -and $message -match 'additive') 'the failure explains why drift is not acceptable'

# ---------------------------------------------------------------------------
# Server-reported identity.
# ---------------------------------------------------------------------------
Write-Group 'Server identity assertions'

$ok = @(Get-NeonServerIdentityMismatch -ServerDatabase 'dailyai' -ServerPort '5432' -ServerUser 'neondb_owner' `
        -ExpectedDatabase 'dailyai' -ExpectedPort 5432 -ExpectedUser 'neondb_owner')
Assert-True ($ok.Count -eq 0) 'a matching server identity produces no mismatch'

$badDb = @(Get-NeonServerIdentityMismatch -ServerDatabase 'postgres' -ServerPort '5432' -ServerUser 'neondb_owner' `
        -ExpectedDatabase 'dailyai' -ExpectedPort 5432 -ExpectedUser 'neondb_owner')
Assert-True ($badDb.Count -eq 1 -and $badDb[0] -match 'postgres') 'a different database is reported'

$badPort = @(Get-NeonServerIdentityMismatch -ServerDatabase 'dailyai' -ServerPort '6432' -ServerUser 'neondb_owner' `
        -ExpectedDatabase 'dailyai' -ExpectedPort 5432 -ExpectedUser 'neondb_owner')
Assert-True ($badPort.Count -eq 1 -and $badPort[0] -match '6432.*5432') 'a different port is reported'

$badUser = @(Get-NeonServerIdentityMismatch -ServerDatabase 'dailyai' -ServerPort '5432' -ServerUser 'postgres' `
        -ExpectedDatabase 'dailyai' -ExpectedPort 5432 -ExpectedUser 'neondb_owner')
Assert-True ($badUser.Count -eq 1 -and $badUser[0] -match 'postgres') 'a different role is reported'

$allBad = @(Get-NeonServerIdentityMismatch -ServerDatabase 'other' -ServerPort '1' -ServerUser 'x' `
        -ExpectedDatabase 'dailyai' -ExpectedPort 5432 -ExpectedUser 'neondb_owner')
Assert-True ($allBad.Count -eq 3) 'every mismatch is reported at once'

$caseInsensitive = @(Get-NeonServerIdentityMismatch -ServerDatabase 'DAILYAI' -ServerPort '5432' -ServerUser 'NEONDB_OWNER' `
        -ExpectedDatabase 'dailyai' -ExpectedPort 5432 -ExpectedUser 'neondb_owner')
Assert-True ($caseInsensitive.Count -eq 0) 'database and role comparison is case-insensitive (PostgreSQL identifiers fold)'

# ---------------------------------------------------------------------------
# Workflow wiring.
# ---------------------------------------------------------------------------
Write-Group 'Workflow wiring (Invoke-NeonTargetValidation.ps1)'

$workflow = Get-Content -LiteralPath $scriptPath -Raw

Assert-True ($workflow -match "SPRING_FLYWAY_BASELINE_ON_MIGRATE\s*=\s*'false'") 'Migrate phase forces SPRING_FLYWAY_BASELINE_ON_MIGRATE=false'
Assert-True ($workflow -match "SPRING_TASK_SCHEDULING_ENABLED\s*=\s*'false'") 'Migrate phase forces SPRING_TASK_SCHEDULING_ENABLED=false'
Assert-True ($workflow -match "\?sslmode=verify-full") 'the JVM JDBC URL is built with sslmode=verify-full'
Assert-True ($workflow -notmatch "\?sslmode='\s*\+") 'the JVM JDBC URL no longer copies the source URL sslmode'
Assert-True ($workflow -match '\*sslmode=verify-full\*') 'startup is refused unless the derived URL carries verify-full'
Assert-True ($workflow -match 'Invoke-WithAppEnvironment -Variables \$appEnvironment') 'the app-start environment goes through the tested save/restore helper'
Assert-True ($workflow -notmatch 'Restore-AppEnvironment|Save-AppEnvironment') 'the hand-rolled env save/restore is gone'
Assert-True ($workflow -match 'Assert-NeonRowCountsUnchanged -Before \$countsBefore -After \$countsAfter') 'post-migration drift uses the tested assertion'
Assert-True ($workflow -match 'Get-NeonRowCountDrift -Before \$countsBefore -After \$countsAfter') 'drifted tables are still listed for the operator'
Assert-True ($workflow -match 'Write-Line "Row-count drift \$d" ''fail''') 'drift lines are logged as failures, not warnings'
Assert-True ($workflow -notmatch 'Row-count drift \$d" ''warn''') 'drift is no longer warning-only'
Assert-True ($workflow -match 'Get-NeonServerIdentityMismatch') 'Test-Connectivity uses the tested identity comparison'
Assert-True ($workflow -match 'Server identity confirmed') 'the confirmed identity is logged'

# ---------------------------------------------------------------------------
# Application configuration and scheduler gating.
# ---------------------------------------------------------------------------
Write-Group 'Application configuration'

$appYml = Get-Content -LiteralPath $appYmlPath -Raw
Assert-True ($appYml -match '(?m)^\s*baseline-on-migrate:\s*false\s*$') 'application.yml sets baseline-on-migrate: false'
Assert-True ($appYml -notmatch '(?m)^\s*baseline-on-migrate:\s*true\s*$') 'no permissive baseline-on-migrate remains'
Assert-True ($appYml -notmatch 'validate-on-migrate') 'checksum validation is left at the Flyway default (true)'
Assert-True ($appYml -match '(?m)^\s*ddl-auto:\s*validate\s*$') 'Hibernate remains in validate mode'
Assert-True ($appYml -match 'RSS_FETCH_ON_STARTUP:true') 'startup RSS fetching stays enabled by default for real deployments'

Write-Group 'Scheduler gating (NewsFetchScheduler.java)'

$scheduler = Get-Content -LiteralPath $schedulerPath -Raw
Assert-True ($scheduler -match '@ConditionalOnProperty\(name = "spring\.task\.scheduling\.enabled", havingValue = "true", matchIfMissing = true\)') 'the scheduler bean is conditional on spring.task.scheduling.enabled'
Assert-True ($scheduler -match 'matchIfMissing = true') 'the scheduler stays enabled when the property is unset (production default)'
Assert-True ($scheduler -match 'ScheduledAnnotationBeanPostProcessor') 'the code documents why the auto-config switch alone is insufficient'

$scheduledOwners = @(Get-ChildItem -LiteralPath $javaRoot -Recurse -Filter '*.java' |
    Where-Object { (Get-Content -LiteralPath $_.FullName -Raw) -match '@Scheduled' } |
    ForEach-Object { $_.Name })
Assert-True ($scheduledOwners.Count -eq 1) "exactly one class owns @Scheduled methods (found: $($scheduledOwners -join ', '))"
Assert-True ($scheduledOwners -contains 'NewsFetchScheduler.java') 'the gated class is the only @Scheduled owner, so false disables all scheduled work'

$parseErrors = $null
$tokens = $null
$null = [System.Management.Automation.Language.Parser]::ParseFile($scriptPath, [ref]$tokens, [ref]$parseErrors)
Assert-True ($parseErrors.Count -eq 0) 'the workflow script still parses'

$parseErrors = $null
$null = [System.Management.Automation.Language.Parser]::ParseFile($helperPath, [ref]$tokens, [ref]$parseErrors)
Assert-True ($parseErrors.Count -eq 0) 'the helper module parses'

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
Write-Host ''
Write-Host ('=' * 72)
Write-Host ("Result: {0} passed, {1} failed, {2} assertions" -f $script:Passed, $script:Failed, ($script:Passed + $script:Failed))
if ($script:Failed -gt 0) {
    Write-Host 'Failed assertions:'
    foreach ($f in $script:Failures) { Write-Host ('  - ' + $f) }
    exit 1
}
Write-Host 'All safeguard tests passed. No database was contacted and no migration was run.'
exit 0
