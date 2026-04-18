param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$Token
)

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$requestPath = Join-Path $projectRoot 'samples\requests\channel-create.json'
$body = Get-Content -Raw -LiteralPath $requestPath
$headers = @{ Authorization = "Bearer $Token" }
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/channels" -ContentType "application/json" -Headers $headers -Body $body
