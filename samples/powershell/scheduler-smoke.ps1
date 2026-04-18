param(
    [string]$BaseUrl = 'http://localhost:8080',
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [int]$WaitSeconds = 12,
    [string]$ContentStyle = 'motivation'
)

$ErrorActionPreference = 'Stop'

$topic = "Scheduler smoke $(Get-Date -Format yyyyMMddHHmmss)"
$body = @{
    topic = $topic
    contentStyle = $ContentStyle
    priority = 999
    source = 'scheduler-smoke'
} | ConvertTo-Json
$headers = @{ Authorization = "Bearer $Token" }

$created = Invoke-RestMethod -Uri "$BaseUrl/api/topics" -Method Post -ContentType 'application/json' -Headers $headers -Body $body
Write-Host "Created topic $($created.id) ($topic). Waiting $WaitSeconds seconds for scheduler..."

Start-Sleep -Seconds $WaitSeconds

$topics = Invoke-RestMethod -Uri "$BaseUrl/api/topics?limit=200" -Method Get -Headers $headers
$current = $topics | Where-Object { $_.id -eq $created.id } | Select-Object -First 1

if (-not $current) {
    throw "Created topic not found in list response."
}

$current | ConvertTo-Json -Depth 10

if ($current.status -ne 'USED' -and $current.status -ne 'PROCESSING') {
    throw "Scheduler smoke failed. Expected status USED/PROCESSING but got $($current.status)."
}

Write-Host "Scheduler smoke passed with status=$($current.status)."
