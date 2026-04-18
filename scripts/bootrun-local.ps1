$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $projectRoot

$envFile = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $envFile)) {
    throw '.env not found. Copy .env.example to .env first.'
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
        return
    }
    $idx = $line.IndexOf('=')
    if ($idx -lt 1) {
        return
    }
    $key = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1)
    [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
}

Write-Host 'Loaded .env into process environment.'

function Get-JavaMajorVersion {
    param([string]$JavaCommand)

    try {
        $escaped = $JavaCommand.Replace('"', '""')
        $output = & cmd.exe /c "`"$escaped`" -version 2>&1" | Out-String
        $match = [regex]::Match($output, 'version\s+"(?<v>[0-9]+)')
        if ($match.Success) {
            return [int]$match.Groups['v'].Value
        }
        $altMatch = [regex]::Match($output, 'openjdk\s+(?<v>[0-9]+)')
        if ($altMatch.Success) {
            return [int]$altMatch.Groups['v'].Value
        }
    } catch {
        return $null
    }

    return $null
}

function Find-Java17Home {
    if ($env:JAVA_HOME) {
        $javaFromHome = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if ((Test-Path -LiteralPath $javaFromHome) -and (Get-JavaMajorVersion -JavaCommand $javaFromHome) -eq 17) {
            return $env:JAVA_HOME
        }
    }

    try {
        $javaCommand = (Get-Command java -ErrorAction Stop).Source
        if ((Get-JavaMajorVersion -JavaCommand $javaCommand) -eq 17) {
            return Split-Path -Parent (Split-Path -Parent $javaCommand)
        }
    } catch {
    }

    $candidateDirs = @()
    if (Test-Path -LiteralPath 'C:\Program Files\Java') {
        $candidateDirs += Get-ChildItem -LiteralPath 'C:\Program Files\Java' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue
    }
    if (Test-Path -LiteralPath 'C:\Program Files\Eclipse Adoptium') {
        $candidateDirs += Get-ChildItem -LiteralPath 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue
    }

    foreach ($dir in ($candidateDirs | Sort-Object FullName -Descending)) {
        $candidateJava = Join-Path $dir.FullName 'bin\java.exe'
        if ((Test-Path -LiteralPath $candidateJava) -and (Get-JavaMajorVersion -JavaCommand $candidateJava) -eq 17) {
            return $dir.FullName
        }
    }

    return $null
}

function Ensure-GradleJvm17 {
    $gradleVersionOut = & .\gradlew.bat --version 2>&1 | Out-String
    $launcherLine = ($gradleVersionOut -split "`n" | Where-Object { $_ -match '^Launcher JVM:' } | Select-Object -First 1).Trim()
    $match = [regex]::Match($launcherLine, 'Launcher JVM:\s+(?<v>[0-9]+)')
    if (-not $match.Success) {
        throw "Could not parse Gradle Launcher JVM from output. Run .\gradlew.bat --version and verify Java 17."
    }
    $launcherMajor = [int]$match.Groups['v'].Value
    if ($launcherMajor -ne 17) {
        throw "Gradle wrapper is using Java $launcherMajor. Java 17 is required."
    }
}

$java17Home = Find-Java17Home
if (-not $java17Home) {
    throw "Java 17 is required. Set JAVA_HOME to your JDK 17 path and prepend %JAVA_HOME%\bin to PATH."
}

$env:JAVA_HOME = $java17Home
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "JAVA_HOME=$env:JAVA_HOME"
Ensure-GradleJvm17

& .\gradlew.bat bootRun
