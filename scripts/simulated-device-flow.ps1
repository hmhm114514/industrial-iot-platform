param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "123456",
    [long]$DeviceId = 0,
    [string]$DeviceKey = "",
    [int]$IntervalSeconds = 2,
    [switch]$CloseAlarm
)

$ErrorActionPreference = "Stop"

function Invoke-PlatformApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $uri = "$BaseUrl$Path"
    $params = @{
        Method = $Method
        Uri = $uri
        Headers = $Headers
    }

    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }

    Invoke-RestMethod @params
}

function New-TelemetryPayload {
    param(
        [long]$Id,
        [string]$Key,
        [double]$Temperature,
        [double]$Humidity,
        [double]$Pressure
    )

    @{
        deviceId = $Id
        deviceKey = $Key
        temperature = [math]::Round($Temperature, 1)
        humidity = [math]::Round($Humidity, 1)
        pressure = [math]::Round($Pressure, 1)
        timestamp = (Get-Date).ToString("s")
        source = "external-simulated-device"
    }
}

Write-Host "== PandaX simulated device flow =="
Write-Host "API base URL: $BaseUrl"

$loginResponse = Invoke-PlatformApi -Method "POST" -Path "/api/auth/login" -Body @{
    username = $Username
    password = $Password
}

$token = $loginResponse.data.token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login failed: token is missing"
}

$headers = @{ Authorization = "Bearer $token" }
Write-Host "Login succeeded: $Username"

if ($DeviceId -le 0 -and [string]::IsNullOrWhiteSpace($DeviceKey)) {
    $deviceResponse = Invoke-PlatformApi -Method "GET" -Path "/api/devices" -Headers $headers
    $firstDevice = @($deviceResponse.data)[0]
    if ($null -eq $firstDevice) {
        throw "No device found. Create a device first."
    }
    $DeviceId = [long]$firstDevice.id
    if ([string]::IsNullOrWhiteSpace($DeviceKey)) {
        $DeviceKey = [string]$firstDevice.deviceKey
    }
    Write-Host "Selected device: $($firstDevice.name) / ID=$DeviceId / Key=$DeviceKey"
} else {
    Write-Host "Using device: ID=$DeviceId / Key=$DeviceKey"
}

if ([string]::IsNullOrWhiteSpace($DeviceKey)) {
    Write-Host "DeviceKey is empty. DeviceId will be used."
}

$normalPayload = New-TelemetryPayload -Id $DeviceId -Key $DeviceKey -Temperature (35 + (Get-Random -Minimum 0 -Maximum 10)) -Humidity (45 + (Get-Random -Minimum 0 -Maximum 20)) -Pressure (100 + (Get-Random -Minimum 0 -Maximum 5))
Write-Host "`n[1/4] Collect and upload normal telemetry"
Write-Host ($normalPayload | ConvertTo-Json -Compress)
$normalResult = Invoke-PlatformApi -Method "POST" -Path "/api/telemetry/report" -Body $normalPayload -Headers $headers
Write-Host "Normal telemetry uploaded. Created alarms: $(@($normalResult.data.alarms).Count)"

Start-Sleep -Seconds $IntervalSeconds

$alarmPayload = New-TelemetryPayload -Id $DeviceId -Key $DeviceKey -Temperature (85 + (Get-Random -Minimum 0 -Maximum 10)) -Humidity (50 + (Get-Random -Minimum 0 -Maximum 15)) -Pressure (101 + (Get-Random -Minimum 0 -Maximum 5))
Write-Host "`n[2/4] Collect and upload high-temperature telemetry"
Write-Host ($alarmPayload | ConvertTo-Json -Compress)
$alarmResult = Invoke-PlatformApi -Method "POST" -Path "/api/telemetry/report" -Body $alarmPayload -Headers $headers
$createdAlarms = @($alarmResult.data.alarms)
Write-Host "High-temperature telemetry uploaded. Created alarms: $($createdAlarms.Count)"

Write-Host "`n[3/4] Query telemetry, rule audits, and alarms"
$telemetryResponse = Invoke-PlatformApi -Method "GET" -Path "/api/telemetry" -Headers $headers
$auditResponse = Invoke-PlatformApi -Method "GET" -Path "/api/rule-audits" -Headers $headers
$alarmResponse = Invoke-PlatformApi -Method "GET" -Path "/api/alarms" -Headers $headers

$latestTelemetry = @($telemetryResponse.data) | Select-Object -First 3
$latestAudits = @($auditResponse.data) | Select-Object -First 5
$openAlarms = @(@($alarmResponse.data) | Where-Object { $_.status -eq "OPEN" })

Write-Host "Telemetry count: $(@($telemetryResponse.data).Count)"
foreach ($item in $latestTelemetry) {
    Write-Host "Telemetry: device=$($item.deviceName), temp=$($item.temperature), humidity=$($item.humidity), pressure=$($item.pressure)"
}

Write-Host "Rule audit count: $(@($auditResponse.data).Count)"
foreach ($item in $latestAudits) {
    Write-Host "Audit: ruleId=$($item.ruleId), deviceId=$($item.deviceId), result=$($item.result), detail=$($item.detail)"
}

Write-Host "OPEN alarm count: $($openAlarms.Count)"
foreach ($item in ($openAlarms | Select-Object -First 3)) {
    Write-Host "Alarm: id=$($item.id), level=$($item.level), device=$($item.deviceName), content=$($item.content)"
}

if ($CloseAlarm -and @($openAlarms).Count -gt 0) {
    Write-Host "`n[4/4] Close latest OPEN alarm"
    $targetAlarm = $openAlarms | Select-Object -First 1
    $handleResponse = Invoke-PlatformApi -Method "POST" -Path "/api/alarms/$($targetAlarm.id)/handle" -Body @{
        handler = $Username
        remark = "Simulated device flow test completed"
    } -Headers $headers
    Write-Host "Alarm closed: id=$($handleResponse.data.id), status=$($handleResponse.data.status)"
} else {
    Write-Host "`n[4/4] Alarm was not closed. Add -CloseAlarm to close it automatically."
}

Write-Host "`nFlow completed: device access, data collection, upload, rule audit, and alarm generation verified."
