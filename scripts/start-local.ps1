param(
    [switch]$WithMinio
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $projectRoot

if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item .env.example .env
    Write-Host '.env created from .env.example'
}

& "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -ExecutionPolicy Bypass -File "$projectRoot\scripts\preflight.ps1"
if ($LASTEXITCODE -ne 0) {
    throw "Preflight checks failed. Resolve issues before startup."
}

docker compose up -d postgres redis rabbitmq
if ($WithMinio) {
    docker compose up -d minio minio-init
}

& "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -ExecutionPolicy Bypass -File "$projectRoot\scripts\generate-default-background.ps1"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to generate default background asset."
}

Write-Host ''
Write-Host 'Dependencies are ready. Start application with:'
Write-Host '  powershell -ExecutionPolicy Bypass -File .\scripts\bootrun-local.ps1'
