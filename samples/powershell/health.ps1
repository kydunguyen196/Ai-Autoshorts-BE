param(
    [string]$BaseUrl = "http://localhost:8080"
)

Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/health"