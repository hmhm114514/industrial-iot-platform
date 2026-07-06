@echo off
setlocal
cd /d "%~dp0"

echo [industrial-iot] Building backend modules...
call mvn -q -DskipTests package
if errorlevel 1 exit /b %errorlevel%

start "platform-core-service :8081" cmd /k "cd /d "%~dp0" && java -jar platform-core-service\target\platform-core-service-0.0.1-SNAPSHOT.jar"
timeout /t 2 /nobreak >nul
start "visual-video-service :8082" cmd /k "cd /d "%~dp0" && java -jar visual-video-service\target\visual-video-service-0.0.1-SNAPSHOT.jar"
timeout /t 2 /nobreak >nul
start "gateway-service :8080" cmd /k "cd /d "%~dp0" && java -jar gateway-service\target\gateway-service-0.0.1-SNAPSHOT.jar"

echo [industrial-iot] Backend services are starting in separate windows.
echo [industrial-iot] Unified API entry: http://localhost:8080/api/**
endlocal
