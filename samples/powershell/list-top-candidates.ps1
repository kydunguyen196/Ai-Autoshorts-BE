param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [Parameter(Mandatory = $true)]
    [string]$GroupId,
    [int]$Page = 0,
    [int]$Limit = 10
)

$uri = "$BaseUrl/api/videos/group/$GroupId/top-candidates?page=$Page&limit=$Limit"
$headers = @{ Authorization = "Bearer $Token" }
Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
