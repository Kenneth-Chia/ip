[CmdletBinding()]
param(
    [switch]$ShowOutput
)

$ErrorActionPreference = 'Stop'
$separator = '____________________________________________________________'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$planPath = Join-Path $PSScriptRoot 'ui-test-plan.md'

Set-Location $repositoryRoot

function Remove-MarkdownIndent {
    param([Parameter(Mandatory)][string]$Text)

    $normalized = $Text -replace "`r`n", "`n"
    $lines = @($normalized -split "`n")
    $nonEmptyLines = @($lines | Where-Object { $_.Length -gt 0 })
    if ($nonEmptyLines.Count -eq 0) {
        return ''
    }

    $minimumIndent = ($nonEmptyLines | ForEach-Object {
            if ($_ -match '^( *)') {
                $Matches[1].Length
            }
        } | Measure-Object -Minimum).Minimum

    return (($lines | ForEach-Object {
                if ($_.Length -ge $minimumIndent) {
                    $_.Substring($minimumIndent)
                } else {
                    $_
                }
            }) -join "`n").TrimEnd("`n")
}

function Get-InlineCommand {
    param(
        [Parameter(Mandatory)][string]$Markdown,
        [Parameter(Mandatory)][string]$Label
    )

    $pattern = '(?m)^- ' + [regex]::Escape($Label) + ': `(?<command>[^`]+)`$'
    $match = [regex]::Match($Markdown, $pattern)
    if (-not $match.Success) {
        throw "The plan does not define '$Label'."
    }
    return $match.Groups['command'].Value
}

function ConvertTo-ArgumentList {
    param([Parameter(Mandatory)][string]$Command)

    # The documented javac and java commands currently contain no quoted arguments.
    # Fail clearly if that changes instead of silently splitting them incorrectly.
    if ($Command.Contains('"') -or $Command.Contains("'")) {
        throw "Quoted arguments are not supported in documented commands: $Command"
    }
    return @($Command -split '\s+' | Where-Object { $_.Length -gt 0 })
}

function Invoke-DocumentedCommand {
    param([Parameter(Mandatory)][string]$Command)

    $parts = @(ConvertTo-ArgumentList $Command)
    if ($parts.Count -eq 0) {
        throw 'The documented command is empty.'
    }

    $executable = $parts[0]
    $arguments = if ($parts.Count -gt 1) { @($parts[1..($parts.Count - 1)]) } else { @() }
    & $executable @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Documented command failed with exit code ${LASTEXITCODE}: $Command"
    }
}

function Get-TestCases {
    param([Parameter(Mandatory)][string]$Markdown)

    $casePattern = '(?ms)^### (?<id>UI-\d+) \S+ (?<name>[^\r\n]+)\r?\n(?<body>.*?)(?=^### UI-|\z)'
    $commandPattern = '(?m)^\s+\d+\. Command/input: `(?<command>[^`]+)`\s*$'
    $outputPattern = '(?ms)Expected output:\s*\r?\n\s*```text\r?\n(?<content>.*?)^\s*```\s*$'
    $filePattern = '(?ms)Expected `(?<path>[^`]+)` content immediately after the command:\s*\r?\n\s*```text\r?\n(?<content>.*?)^\s*```\s*$'
    $cases = [System.Collections.Generic.List[object]]::new()

    foreach ($caseMatch in [regex]::Matches($Markdown, $casePattern)) {
        $body = $caseMatch.Groups['body'].Value
        $secondSessionPosition = $body.IndexOf('- Second session inputs')
        $commandMatches = @([regex]::Matches($body, $commandPattern))
        if ($commandMatches.Count -eq 0) {
            throw "$($caseMatch.Groups['id'].Value) does not contain any command inputs."
        }

        $steps = [System.Collections.Generic.List[object]]::new()
        for ($index = 0; $index -lt $commandMatches.Count; $index++) {
            $commandMatch = $commandMatches[$index]
            $sectionStart = $commandMatch.Index + $commandMatch.Length
            $sectionEnd = if ($index + 1 -lt $commandMatches.Count) {
                $commandMatches[$index + 1].Index
            } else {
                $body.Length
            }
            $section = $body.Substring($sectionStart, $sectionEnd - $sectionStart)
            $outputMatch = [regex]::Match($section, $outputPattern)
            if (-not $outputMatch.Success) {
                throw "$($caseMatch.Groups['id'].Value) has no expected output for '$($commandMatch.Groups['command'].Value)'."
            }

            $fileMatch = [regex]::Match($section, $filePattern)
            $session = if ($secondSessionPosition -ge 0 -and $commandMatch.Index -gt $secondSessionPosition) { 2 } else { 1 }
            $steps.Add([pscustomobject]@{
                    Command      = $commandMatch.Groups['command'].Value
                    Expected     = Remove-MarkdownIndent $outputMatch.Groups['content'].Value
                    Session      = $session
                    FilePath     = if ($fileMatch.Success) { $fileMatch.Groups['path'].Value } else { $null }
                    ExpectedFile = if ($fileMatch.Success) { Remove-MarkdownIndent $fileMatch.Groups['content'].Value } else { $null }
                })
        }

        $cases.Add([pscustomobject]@{
                Id                  = $caseMatch.Groups['id'].Value
                Name                = $caseMatch.Groups['name'].Value
                Steps               = @($steps)
                RemoveDataDirectory = $body -match '(?m)^- Setup:.*Remove-Item -LiteralPath data -Recurse'
                RequireDataDirectory = $body -match 'Confirm that `data` exists after startup'
            })
    }
    return @($cases)
}

function Start-DocumentedProgram {
    param([Parameter(Mandatory)][string]$LaunchCommand)

    $parts = @(ConvertTo-ArgumentList $LaunchCommand)
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $parts[0]
    $startInfo.Arguments = if ($parts.Count -gt 1) { $parts[1..($parts.Count - 1)] -join ' ' } else { '' }
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.UseShellExecute = $false

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    [void]$process.Start()
    return $process
}

function Read-OutputBlock {
    param([Parameter(Mandatory)][System.Diagnostics.Process]$Process)

    $lines = [System.Collections.Generic.List[string]]::new()
    $separatorCount = 0
    while ($separatorCount -lt 2) {
        $line = $Process.StandardOutput.ReadLine()
        if ($null -eq $line) {
            $errorOutput = $Process.StandardError.ReadToEnd()
            throw "The application ended before a complete output block was received.`n$errorOutput"
        }
        $lines.Add($line)
        if ($line -ceq $separator) {
            $separatorCount++
        }
    }
    return ($lines -join "`n")
}

function Stop-TestProcess {
    param([System.Diagnostics.Process]$Process)

    if ($null -ne $Process -and -not $Process.HasExited) {
        $Process.Kill()
        $Process.WaitForExit()
    }
}

$plan = ([System.IO.File]::ReadAllText($planPath) -replace "`r`n", "`n")
$recordsHeading = $plan.IndexOf('## Test-session records')
if ($recordsHeading -ge 0) {
    $plan = $plan.Substring(0, $recordsHeading)
}

$requiredJavaVersion = [regex]::Match($plan, '(?m)^- Java version: (?<version>\d+)\s*$').Groups['version'].Value
$compileCommand = Get-InlineCommand $plan 'Setup/compile command'
$launchCommand = Get-InlineCommand $plan 'Program launch command'
$testCases = @(Get-TestCases $plan)
if ($testCases.Count -eq 0) {
    throw 'No UI test cases were parsed from the plan.'
}
Write-Output "Parsed $($testCases.Count) UI test cases from the plan."

$versionStartInfo = [System.Diagnostics.ProcessStartInfo]::new()
$versionStartInfo.FileName = 'java'
$versionStartInfo.Arguments = '-version'
$versionStartInfo.RedirectStandardOutput = $true
$versionStartInfo.RedirectStandardError = $true
$versionStartInfo.UseShellExecute = $false
$versionProcess = [System.Diagnostics.Process]::Start($versionStartInfo)
$javaVersionOutput = ($versionProcess.StandardOutput.ReadToEnd() + $versionProcess.StandardError.ReadToEnd()).Trim()
$versionProcess.WaitForExit()
if ($versionProcess.ExitCode -ne 0 -or $javaVersionOutput -notmatch "version `"$requiredJavaVersion\.") {
    throw "Java $requiredJavaVersion is required.`nSelected Java:`n$javaVersionOutput"
}
Write-Output "`$ java -version -- Java $requiredJavaVersion confirmed"

$compileWatch = [System.Diagnostics.Stopwatch]::StartNew()
Invoke-DocumentedCommand $compileCommand
$compileWatch.Stop()
Write-Output "`$ $compileCommand"
Write-Output "Compilation passed in $([math]::Round($compileWatch.Elapsed.TotalSeconds, 2)) s."

$suiteWatch = [System.Diagnostics.Stopwatch]::StartNew()
foreach ($testCase in $testCases) {
    Write-Output "Starting $($testCase.Id) -- $($testCase.Name)"
    if ($testCase.RemoveDataDirectory) {
        Remove-Item -LiteralPath (Join-Path $repositoryRoot 'data') -Recurse -Force -ErrorAction SilentlyContinue
    } else {
        Remove-Item -LiteralPath (Join-Path $repositoryRoot 'data/sumo.txt') -ErrorAction SilentlyContinue
    }

    $caseWatch = [System.Diagnostics.Stopwatch]::StartNew()
    $currentSession = 0
    $process = $null
    try {
        foreach ($step in $testCase.Steps) {
            if ($step.Session -ne $currentSession) {
                Stop-TestProcess $process
                $process = Start-DocumentedProgram $launchCommand
                $startupOutput = Read-OutputBlock $process
                if ($ShowOutput) {
                    Write-Output $startupOutput
                }
                $currentSession = $step.Session
            }

            if ([string]::IsNullOrEmpty($step.Command)) {
                throw "$($testCase.Id) contains an empty command."
            }
            $process.StandardInput.WriteLine($step.Command)
            $actual = Read-OutputBlock $process
            if ($actual -cne $step.Expected) {
                Stop-TestProcess $process
                throw "$($testCase.Id) failed at > $($step.Command)`nActual output:`n$actual`nExpected output:`n$($step.Expected)"
            }

            Write-Output "$($testCase.Id) > $($step.Command) -- PASS"
            if ($ShowOutput) {
                Write-Output $actual
            }

            if ($null -ne $step.FilePath) {
                $assertedPath = Join-Path $repositoryRoot $step.FilePath
                if (-not (Test-Path -LiteralPath $assertedPath -PathType Leaf)) {
                    throw "$($testCase.Id) expected '$($step.FilePath)' after > $($step.Command), but the file does not exist."
                }
                $actualFile = ([System.IO.File]::ReadAllText($assertedPath) -replace "`r`n", "`n").TrimEnd("`n")
                if ($actualFile -cne $step.ExpectedFile) {
                    throw "$($testCase.Id) file assertion failed after > $($step.Command)`nActual file content:`n$actualFile`nExpected file content:`n$($step.ExpectedFile)"
                }
                Write-Output "$($testCase.Id) `$ Get-Content $($step.FilePath) -- PASS"
            }
        }

        if ($testCase.RequireDataDirectory -and -not (Test-Path -LiteralPath (Join-Path $repositoryRoot 'data') -PathType Container)) {
            throw "$($testCase.Id) expected the data directory to exist after startup."
        }
    } finally {
        Stop-TestProcess $process
    }
    $caseWatch.Stop()
    Write-Output "$($testCase.Id) passed in $([math]::Round($caseWatch.Elapsed.TotalSeconds, 2)) s."
}

$suiteWatch.Stop()
Write-Output "All $($testCases.Count) UI test cases passed in $([math]::Round($suiteWatch.Elapsed.TotalSeconds, 2)) s, excluding compilation."
