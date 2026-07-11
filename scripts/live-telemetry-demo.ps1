param(
    [int]$DeviceId = 1,
    [int]$IntervalSeconds = 2,
    [int]$DurationMinutes = 10
)

$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8080/api'
$loginBody = @{ username = 'admin'; password = '123456' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/auth/login" -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.data.token)" }
$iterations = [Math]::Max(1, [Math]::Floor($DurationMinutes * 60 / $IntervalSeconds))

for ($index = 0; $index -lt $iterations; $index++) {
    $temperature = [Math]::Round(27 + 5 * [Math]::Sin($index / 5) + (Get-Random -Minimum -30 -Maximum 31) / 100, 1)
    $humidity = [Math]::Round(52 + 8 * [Math]::Cos($index / 7) + (Get-Random -Minimum -40 -Maximum 41) / 100, 1)
    $pressure = [Math]::Round(97 + 1.5 * [Math]::Sin($index / 9) + (Get-Random -Minimum -20 -Maximum 21) / 100, 1)
    $body = @{
        deviceId = $DeviceId
        temperature = $temperature
        humidity = $humidity
        pressure = $pressure
    } | ConvertTo-Json

    $result = Invoke-RestMethod -Method Post -Uri "$baseUrl/telemetry/simulate" -Headers $headers -ContentType 'application/json' -Body $body
    $telemetry = $result.data.telemetry
    "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') device=$DeviceId temperature=$($telemetry.temperature) humidity=$($telemetry.humidity) pressure=$($telemetry.pressure)"
    Start-Sleep -Seconds $IntervalSeconds
}
