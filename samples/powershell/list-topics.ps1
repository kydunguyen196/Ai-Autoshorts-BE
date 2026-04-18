param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [int]$Limit = 20,
    [string]$Status
)

$uri = "$BaseUrl/api/topics?limit=$Limit"
if ($Status) {
    $uri += "&status=$Status"
}
$headers = @{ Authorization = "Bearer $Token" }
Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
