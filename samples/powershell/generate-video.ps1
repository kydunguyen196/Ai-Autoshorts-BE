param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$Token
)

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$requestPath = Join-Path $projectRoot 'samples\requests\generate-video.json'
$body = Get-Content -Raw -LiteralPath $requestPath
$headers = @{ Authorization = "Bearer $Token" }
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/videos/generate" -ContentType "application/json" -Headers $headers -Body $body
