param(
    [Parameter(Mandatory = $true)]
    [string]$JobId,
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$Token
)

$headers = @{ Authorization = "Bearer $Token" }
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/videos/$JobId/retry" -ContentType "application/json" -Headers $headers
