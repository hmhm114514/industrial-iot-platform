$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$SafeRoot = $Root.Replace("'", "''")

Write-Host "[industrial-iot] Building backend modules..." -ForegroundColor Cyan
Push-Location $Root
try {
    mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}

$services = @(
    @{ Name = "platform-core-service"; Port = 8081 },
    @{ Name = "visual-video-service"; Port = 8082 },
    @{ Name = "gateway-service"; Port = 8080 }
)

foreach ($service in $services) {
    $name = $service.Name
    $port = $service.Port
    $jar = Join-Path $Root "$name\target\$name-0.0.1-SNAPSHOT.jar"
    $safeJar = $jar.Replace("'", "''")
    $command = "Set-Location -LiteralPath '$SafeRoot'; Write-Host '[industrial-iot] Starting $name on port $port...' -ForegroundColor Cyan; java -jar '$safeJar'"
    Start-Process -FilePath "powershell" -ArgumentList @("-NoExit", "-Command", $command) -WindowStyle Normal
    Start-Sleep -Seconds 2
}

Write-Host "[industrial-iot] Backend services are starting in separate PowerShell windows." -ForegroundColor Green
Write-Host "[industrial-iot] Unified API entry: http://localhost:8080/api/**" -ForegroundColor Green
