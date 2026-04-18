param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$Token
)

$headers = @{ Authorization = "Bearer $Token" }
Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/frontend/bootstrap" -Headers $headers
