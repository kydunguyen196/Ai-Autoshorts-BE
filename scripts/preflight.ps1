param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'

function Write-Check {
    param(
        [string]$Name,
        [bool]$Ok,
        [string]$Details
    )

    if ($Ok) {
        Write-Host "[PASS] $Name - $Details" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $Name - $Details" -ForegroundColor Red
    }
}

function Read-DotEnv {
    param([string]$Path)

    $result = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $result
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
            return
        }

        $index = $line.IndexOf('=')
        if ($index -lt 1) {
            return
        }

        $key = $line.Substring(0, $index).Trim()
        $value = $line.Substring($index + 1).Trim()
        $result[$key] = $value
    }

    return $result
}

function Get-JavaMajorVersionFromOutput {
    param([string]$Output)

    if (-not $Output) {
        return $null
    }

    $match = [regex]::Match($Output, 'version\s+"(?<v>[0-9]+)')
    if ($match.Success) {
        return [int]$match.Groups['v'].Value
    }

    $altMatch = [regex]::Match($Output, 'openjdk\s+(?<v>[0-9]+)')
    if ($altMatch.Success) {
        return [int]$altMatch.Groups['v'].Value
    }

    return $null
}

function Get-JavaInfo {
    param([string]$JavaCommand)

    $escaped = $JavaCommand.Replace('"', '""')
    $output = & cmd.exe /c "`"$escaped`" -version 2>&1" | Out-String
    $firstLine = ($output -split "`n" | Select-Object -First 1).Trim()
    $major = Get-JavaMajorVersionFromOutput -Output $output

    [PSCustomObject]@{
        command = $JavaCommand
        firstLine = $firstLine
        major = $major
    }
}

function Find-ProjectFfmpegBinary {
    param([string]$ProjectPath)

    $candidates = @(
        (Join-Path $ProjectPath 'ffmpeg.exe'),
        (Join-Path $ProjectPath 'ffmpeg\\bin\\ffmpeg.exe'),
        (Join-Path $ProjectPath 'tools\\ffmpeg\\bin\\ffmpeg.exe')
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    $found = Get-ChildItem -LiteralPath $ProjectPath -Recurse -File -Filter 'ffmpeg.exe' -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($found) {
        return $found.FullName
    }

    return $null
}

function Test-BinaryExecutable {
    param([string]$BinaryPath)

    try {
        $escaped = $BinaryPath.Replace('"', '""')
        $output = & cmd.exe /c "`"$escaped`" -version 2>&1" | Out-String
        $firstLine = ($output -split "`n" | Select-Object -First 1).Trim()
        $exitCode = $LASTEXITCODE
        return [PSCustomObject]@{
            ok = ($exitCode -eq 0)
            firstLine = $firstLine
            exitCode = $exitCode
            error = $null
        }
    } catch {
        return [PSCustomObject]@{
            ok = $false
            firstLine = $null
            exitCode = $null
            error = $_.Exception.Message
        }
    }
}

$failures = 0

$projectPath = (Resolve-Path -LiteralPath $ProjectRoot).Path
$envPath = Join-Path $projectPath '.env'
$composePath = Join-Path $projectPath 'docker-compose.yml'
$gradlewBatPath = Join-Path $projectPath 'gradlew.bat'

$javaInfo = $null
$javaHomeInfo = $null

# Java 17 check (active shell runtime)
$javaOk = $false
$javaDetails = ''
try {
    $javaCommand = (Get-Command java -ErrorAction Stop).Source
    $javaInfo = Get-JavaInfo -JavaCommand $javaCommand
    $javaMajor = $null
    if ($null -ne $javaInfo.major) {
        $javaMajor = [int]$javaInfo.major
    }
    $javaOk = ($javaMajor -eq 17) -or ($javaInfo.firstLine -match 'version\s+"17')
    $javaDetails = "$($javaInfo.firstLine) [$($javaInfo.command)]"
} catch {
    $javaDetails = $_.Exception.Message
}
Write-Check -Name 'Java 17 (shell)' -Ok $javaOk -Details $javaDetails
if (-not $javaOk) { $failures++ }

# JAVA_HOME check
$javaHomeOk = $false
$javaHomeDetails = ''
if ($env:JAVA_HOME) {
    $javaFromHome = Join-Path $env:JAVA_HOME 'bin\\java.exe'
    if (Test-Path -LiteralPath $javaFromHome) {
        try {
            $javaHomeInfo = Get-JavaInfo -JavaCommand $javaFromHome
            $javaHomeMajor = $null
            if ($null -ne $javaHomeInfo.major) {
                $javaHomeMajor = [int]$javaHomeInfo.major
            }
            $javaHomeOk = ($javaHomeMajor -eq 17) -or ($javaHomeInfo.firstLine -match 'version\s+"17')
            $javaHomeDetails = "$($javaHomeInfo.firstLine) [$javaFromHome]"
        } catch {
            $javaHomeDetails = $_.Exception.Message
        }
    } else {
        $javaHomeDetails = "JAVA_HOME set but java.exe missing: $javaFromHome"
    }
} else {
    $javaHomeDetails = 'JAVA_HOME is not set'
}
Write-Check -Name 'JAVA_HOME -> Java 17' -Ok $javaHomeOk -Details $javaHomeDetails
if (-not $javaHomeOk) { $failures++ }

# PATH vs JAVA_HOME mismatch check
$mismatchOk = $true
$mismatchDetails = 'Shell java and JAVA_HOME java are aligned'
if ($javaInfo -and $javaHomeInfo) {
    $shellJavaPath = (Resolve-Path -LiteralPath $javaInfo.command).Path
    $homeJavaPath = (Resolve-Path -LiteralPath $javaHomeInfo.command).Path
    if ($javaInfo.major -ne $javaHomeInfo.major -or $shellJavaPath -ne $homeJavaPath) {
        $mismatchOk = $false
        $mismatchDetails = "Shell java uses $shellJavaPath, JAVA_HOME points to $homeJavaPath"
    }
}
Write-Check -Name 'JAVA_HOME/PATH alignment' -Ok $mismatchOk -Details $mismatchDetails
if (-not $mismatchOk) { $failures++ }

# Docker check
$dockerOk = $false
$dockerDetails = ''
try {
    $dockerVersion = (& docker --version 2>&1 | Out-String).Trim()
    $composeVersion = (& docker compose version 2>&1 | Out-String).Trim()
    $dockerOk = $LASTEXITCODE -eq 0
    $dockerDetails = "$dockerVersion | $composeVersion"
} catch {
    $dockerDetails = 'docker or docker compose not available'
}
Write-Check -Name 'Docker + Compose' -Ok $dockerOk -Details $dockerDetails
if (-not $dockerOk) { $failures++ }

# Gradle wrapper check
$wrapperOk = Test-Path -LiteralPath $gradlewBatPath
$wrapperDetails = if ($wrapperOk) { 'gradlew.bat found' } else { 'gradlew.bat missing' }
Write-Check -Name 'Gradle wrapper' -Ok $wrapperOk -Details $wrapperDetails
if (-not $wrapperOk) { $failures++ }

# Gradle JVM check
$gradleJvmOk = $false
$gradleJvmDetails = ''
if ($wrapperOk) {
    try {
        $gradleVersionOut = & $gradlewBatPath --version 2>&1 | Out-String
        $launcherLine = ($gradleVersionOut -split "`n" | Where-Object { $_ -match '^Launcher JVM:' } | Select-Object -First 1).Trim()
        $gradleJvmMatch = [regex]::Match($launcherLine, 'Launcher JVM:\s+(?<v>[0-9]+)')
        if ($gradleJvmMatch.Success) {
            $launcherMajor = [int]$gradleJvmMatch.Groups['v'].Value
            $gradleJvmOk = $launcherMajor -eq 17
            $gradleJvmDetails = $launcherLine
        } else {
            $gradleJvmDetails = 'Could not parse Launcher JVM from gradlew --version output'
        }
    } catch {
        $gradleJvmDetails = $_.Exception.Message
    }
} else {
    $gradleJvmDetails = 'gradlew.bat missing'
}
Write-Check -Name 'Gradle JVM = 17' -Ok $gradleJvmOk -Details $gradleJvmDetails
if (-not $gradleJvmOk) { $failures++ }

# .env check
$envOk = Test-Path -LiteralPath $envPath
$envDetails = if ($envOk) { '.env found' } else { '.env missing (copy from .env.example)' }
Write-Check -Name '.env file' -Ok $envOk -Details $envDetails
if (-not $envOk) { $failures++ }
$envValues = if ($envOk) { Read-DotEnv -Path $envPath } else { @{} }

# JWT secret check
$jwtSecretOk = $true
$jwtSecretDetails = 'APP_JWT_SECRET is not set in .env (application.yml fallback will be used for local dev)'
if ($envOk) {
    $jwtSecret = $envValues['APP_JWT_SECRET']
    if ([string]::IsNullOrWhiteSpace($jwtSecret)) {
        $jwtSecretOk = $true
    } elseif ($jwtSecret.Length -lt 32) {
        $jwtSecretOk = $false
        $jwtSecretDetails = "APP_JWT_SECRET is too short ($($jwtSecret.Length) chars). Minimum is 32."
    } else {
        $jwtSecretDetails = "APP_JWT_SECRET looks valid ($($jwtSecret.Length) chars)."
    }
}
Write-Check -Name 'JWT Secret' -Ok $jwtSecretOk -Details $jwtSecretDetails
if (-not $jwtSecretOk) { $failures++ }

# Datasource/compose port consistency check
$dbConfigOk = $true
$dbConfigDetails = 'Datasource URL and POSTGRES_PORT look consistent'
if ($envOk) {
    $datasourceUrl = $envValues['SPRING_DATASOURCE_URL']
    $postgresPort = $envValues['POSTGRES_PORT']
    if (-not [string]::IsNullOrWhiteSpace($datasourceUrl) -and -not [string]::IsNullOrWhiteSpace($postgresPort)) {
        $match = [regex]::Match($datasourceUrl, 'jdbc:postgresql://[^:]+:(?<port>[0-9]+)/')
        if ($match.Success) {
            $urlPort = $match.Groups['port'].Value
            if ($urlPort -ne $postgresPort) {
                $dbConfigOk = $false
                $dbConfigDetails = "SPRING_DATASOURCE_URL port ($urlPort) != POSTGRES_PORT ($postgresPort)"
            }
        }
    }
}
Write-Check -Name 'DB Port Config' -Ok $dbConfigOk -Details $dbConfigDetails
if (-not $dbConfigOk) { $failures++ }

# docker-compose check
$composeOk = Test-Path -LiteralPath $composePath
$composeDetails = if ($composeOk) { 'docker-compose.yml found' } else { 'docker-compose.yml missing' }
Write-Check -Name 'Compose file' -Ok $composeOk -Details $composeDetails
if (-not $composeOk) { $failures++ }

# RabbitMQ queue/compose checks
$queueConfigOk = $true
$queueConfigDetails = 'Queue config disabled'
if ($envOk) {
    $queueEnabledRaw = $envValues['APP_QUEUE_ENABLED']
    $queueEnabled = $true
    if (-not [string]::IsNullOrWhiteSpace($queueEnabledRaw)) {
        $queueEnabled = $queueEnabledRaw.Trim().ToLower() -ne 'false'
    }

    if ($queueEnabled) {
        $rabbitHost = $envValues['SPRING_RABBITMQ_HOST']
        $rabbitPort = $envValues['SPRING_RABBITMQ_PORT']
        if ([string]::IsNullOrWhiteSpace($rabbitHost)) { $rabbitHost = 'localhost' }
        if ([string]::IsNullOrWhiteSpace($rabbitPort)) { $rabbitPort = '5672' }

        $queueConfigDetails = "Queue enabled. RabbitMQ target=$rabbitHost`:$rabbitPort"

        if ($composeOk) {
            try {
                $composeServices = (& docker compose config --services 2>&1 | Out-String)
                if (-not ($composeServices -match '(^|\r?\n)rabbitmq(\r?\n|$)')) {
                    $queueConfigOk = $false
                    $queueConfigDetails = "APP_QUEUE_ENABLED=true but docker-compose has no 'rabbitmq' service"
                }
            } catch {
                $queueConfigOk = $false
                $queueConfigDetails = "Failed to inspect docker compose services: $($_.Exception.Message)"
            }
        }
    }
}
Write-Check -Name 'RabbitMQ Queue Config' -Ok $queueConfigOk -Details $queueConfigDetails
if (-not $queueConfigOk) { $failures++ }

# FFmpeg check
$ffmpegConfigured = $null
if ($envValues.ContainsKey('APP_FFMPEG_BINARY') -and -not [string]::IsNullOrWhiteSpace($envValues['APP_FFMPEG_BINARY'])) {
    $ffmpegConfigured = $envValues['APP_FFMPEG_BINARY']
} elseif ($envValues.ContainsKey('FFMPEG_PATH') -and -not [string]::IsNullOrWhiteSpace($envValues['FFMPEG_PATH'])) {
    $ffmpegConfigured = $envValues['FFMPEG_PATH']
}
if (-not $ffmpegConfigured) {
    $ffmpegConfigured = 'ffmpeg'
}

$ffmpegOk = $false
$ffmpegDetails = ''
try {
    $isPathLike = $ffmpegConfigured.Contains('\\') -or $ffmpegConfigured.Contains('/') -or $ffmpegConfigured.Contains(':')
    if ($isPathLike) {
        $candidate = $ffmpegConfigured
        if (-not [System.IO.Path]::IsPathRooted($candidate)) {
            $candidate = Join-Path $projectPath $candidate
        }
        if (Test-Path -LiteralPath $candidate) {
            $probe = Test-BinaryExecutable -BinaryPath $candidate
            if ($probe.ok) {
                $ffmpegOk = $true
                $ffmpegDetails = "configured binary executable: $candidate | $($probe.firstLine)"
            } else {
                $ffmpegDetails = "configured binary found but not executable: $candidate (exitCode=$($probe.exitCode))"
            }
        } else {
            $ffmpegDetails = "configured binary not found: $candidate"
        }
    } else {
        $cmd = Get-Command $ffmpegConfigured -ErrorAction Stop
        $probe = Test-BinaryExecutable -BinaryPath $cmd.Source
        if ($probe.ok) {
            $ffmpegOk = $true
            $ffmpegDetails = "command executable: $($cmd.Source) | $($probe.firstLine)"
        } else {
            $ffmpegDetails = "command found but not executable: $($cmd.Source) (exitCode=$($probe.exitCode))"
        }
    }
} catch {
    if (-not $ffmpegDetails) {
        $ffmpegDetails = "ffmpeg not found (configured value: $ffmpegConfigured)"
    }
}

if (-not $ffmpegOk) {
    $projectFfmpeg = Find-ProjectFfmpegBinary -ProjectPath $projectPath
    if ($projectFfmpeg) {
        $ffmpegDetails = "$ffmpegDetails. Found project binary candidate: $projectFfmpeg. Set APP_FFMPEG_BINARY=$projectFfmpeg"
    } elseif (Test-Path -LiteralPath (Join-Path $projectPath 'ffmpeg-8.1')) {
        $ffmpegDetails = "$ffmpegDetails. Detected ffmpeg source folder (ffmpeg-8.1) but no ffmpeg.exe binary. Download a Windows build and set APP_FFMPEG_BINARY to ...\\ffmpeg.exe"
    }
}

Write-Check -Name 'FFmpeg' -Ok $ffmpegOk -Details $ffmpegDetails
if (-not $ffmpegOk) { $failures++ }

Write-Host ""
if ($failures -eq 0) {
    Write-Host 'Preflight result: READY for local run.' -ForegroundColor Green
    exit 0
}

Write-Host "Preflight result: $failures check(s) failed. Fix these before running the app." -ForegroundColor Red
exit 1
