param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [Parameter(Mandatory = $true)]
    [string]$BatchId,
    [int]$Page = 0,
    [int]$Limit = 20,
    [string]$Status
)

$uri = "$BaseUrl/api/videos/batch/$BatchId?page=$Page&limit=$Limit"
if ($Status) {
    $uri += "&status=$Status"
}
$headers = @{ Authorization = "Bearer $Token" }
Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
