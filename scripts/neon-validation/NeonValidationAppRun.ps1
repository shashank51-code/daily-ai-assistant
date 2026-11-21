#Requires -Version 5.1
<#
.SYNOPSIS
    Environment and assertion helpers used by Invoke-NeonTargetValidation.ps1.

.DESCRIPTION
    Dot-sourceable and side-effect free: this file only defines functions, so it can be
    loaded by tests without connecting to anything.

    Why the environment helper exists: the Migrate phase starts a short-lived JVM with
    credentials and target overrides in process-scope variables. Every one of those
    values must come back exactly as it was - including when the phase fails, the
    operator presses Ctrl+C, or the child process cannot start. Doing that by hand is
    easy to get wrong, so the save/set/run/restore cycle lives in one function and is
    covered by tests/Test-NeonValidationSafeguards.ps1.
#>

function Invoke-WithAppEnvironment {
    <#
    .SYNOPSIS
        Runs a script block with the given process environment values applied, then
        restores every previous value (a previously unset value is removed again).

    .DESCRIPTION
        Restoration happens in a finally block, so it also runs when the script block
        throws. The script block's output is passed through.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][System.Collections.IDictionary] $Variables,
        [Parameter(Mandatory = $true)][scriptblock] $Script
    )

    $saved = @{}
    foreach ($name in $Variables.Keys) {
        $saved[[string]$name] = [Environment]::GetEnvironmentVariable([string]$name, 'Process')
    }

    try {
        foreach ($name in $Variables.Keys) {
            [Environment]::SetEnvironmentVariable([string]$name, [string]$Variables[$name], 'Process')
        }
        & $Script
    }
    finally {
        foreach ($name in $saved.Keys) {
            $previous = $saved[$name]
            if ($null -eq $previous) {
                [Environment]::SetEnvironmentVariable([string]$name, $null, 'Process')
            }
            else {
                [Environment]::SetEnvironmentVariable([string]$name, [string]$previous, 'Process')
            }
        }
    }
}

function Get-NeonRowCountDrift {
    <#
    .SYNOPSIS
        Returns one description per table whose row count changed between two snapshots.

    .DESCRIPTION
        A table that is missing from the "after" snapshot counts as drift, so a dropped
        or unreadable table can never look like "no change".
    #>
    param(
        [Parameter(Mandatory = $true)][System.Collections.IDictionary] $Before,
        [Parameter(Mandatory = $true)][System.Collections.IDictionary] $After
    )

    $drift = New-Object System.Collections.Generic.List[string]
    foreach ($key in $Before.Keys) {
        $expected = $Before[$key]
        $actual = if ($After.Contains($key)) { $After[$key] } else { $null }
        if ($null -eq $actual) {
            $drift.Add(('{0}: {1} -> missing' -f $key, $expected))
            continue
        }
        if ([int64]$actual -ne [int64]$expected) {
            $drift.Add(('{0}: {1} -> {2}' -f $key, $expected, $actual))
        }
    }
    return $drift.ToArray()
}

function Assert-NeonRowCountsUnchanged {
    <#
    .SYNOPSIS
        Throws when any documented table's row count differs across the additive step.
    #>
    param(
        [Parameter(Mandatory = $true)][System.Collections.IDictionary] $Before,
        [Parameter(Mandatory = $true)][System.Collections.IDictionary] $After
    )

    $drift = @(Get-NeonRowCountDrift -Before $Before -After $After)
    if ($drift.Count -gt 0) {
        throw ('Row counts changed across the additive migration: ' + ($drift -join '; ') + '.')
    }
}

function Get-NeonServerIdentityMismatch {
    <#
    .SYNOPSIS
        Compares the identity the server reports with the expected target identity.

    .DESCRIPTION
        The URL is parsed client-side, so the server's own answer is the only proof that
        this session landed on the expected database, role and port. Returns one
        description per mismatch; an empty array means the server agrees.
    #>
    param(
        [string] $ServerDatabase,
        [string] $ServerPort,
        [string] $ServerUser,
        [Parameter(Mandatory = $true)][string] $ExpectedDatabase,
        [Parameter(Mandatory = $true)][int] $ExpectedPort,
        [Parameter(Mandatory = $true)][string] $ExpectedUser
    )

    $problems = New-Object System.Collections.Generic.List[string]
    if ($ServerDatabase -ne $ExpectedDatabase) {
        $problems.Add("database '$ServerDatabase' is not '$ExpectedDatabase'")
    }
    if ($ServerPort -ne [string]$ExpectedPort) {
        $problems.Add("server port '$ServerPort' is not '$ExpectedPort'")
    }
    if ($ServerUser -ne $ExpectedUser) {
        $problems.Add("server role '$ServerUser' is not '$ExpectedUser'")
    }
    return $problems.ToArray()
}
