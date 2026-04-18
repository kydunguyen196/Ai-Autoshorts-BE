param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$Token
)

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$requestPath = Join-Path $projectRoot 'samples\requests\batch-generate.json'
$body = Get-Content -Raw -LiteralPath $requestPath
$headers = @{ Authorization = "Bearer $Token" }
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/videos/batch-generate" -ContentType "application/json" -Headers $headers -Body $body
