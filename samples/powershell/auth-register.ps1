param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Email = "creator1@example.com",
    [string]$Password = "ChangeMe123!",
    [string]$DisplayName = "Creator One"
)

$body = @{
    email = $Email
    password = $Password
    displayName = $DisplayName
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body $body
