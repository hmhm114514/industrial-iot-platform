#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo "[industrial-iot] Building backend modules..."
mvn -q -DskipTests package

mkdir -p logs

start_service() {
  local module="$1"
  local port="$2"
  echo "[industrial-iot] Starting ${module} on port ${port}..."
  (java -jar "${module}/target/${module}-0.0.1-SNAPSHOT.jar" > "logs/${module}.log" 2>&1 & echo $! > "logs/${module}.pid")
}

start_service platform-core-service 8081
sleep 2
start_service visual-video-service 8082
sleep 2
start_service gateway-service 8080

echo "[industrial-iot] Backend services are starting in the background."
echo "[industrial-iot] Logs: backend/logs/*.log"
echo "[industrial-iot] Unified API entry: http://localhost:8080/api/**"
