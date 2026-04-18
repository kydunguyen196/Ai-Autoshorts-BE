param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Email = "creator1@example.com",
    [string]$Password = "ChangeMe123!"
)

$body = @{
    email = $Email
    password = $Password
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $body
