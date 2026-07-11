# MQTT 持续数据发送命令

用于通过 MQTTX / Mosquitto 持续发送设备遥测数据，配合平台完成实时检测、历史数据写入、规则判断和历史告警联动。

## 前提

- Docker 中 Mosquitto 容器已启动：`pandax-mqtt-broker`
- 后端核心服务已启动
- 网络服务中的 `MQTT接入服务` 为运行中
- 设备管理中存在设备 Key：`key-th-001`
- 规则设计中存在并启用了对应指标规则，例如 `temperature > 80`

## Topic

```text
/iot/key-th-001/telemetry
```

## 单条测试 Payload

```json
{
  "temperature": 91.2,
  "humidity": 51,
  "pressure": 102.4
}
```

## 单条测试命令

使用 `ConvertTo-Json -Compress` 生成合法 JSON，避免 PowerShell 把双引号吃掉。

```powershell
$payload = @{
  temperature = 91.2
  humidity = 51
  pressure = 102.4
} | ConvertTo-Json -Compress

docker exec pandax-mqtt-broker mosquitto_pub `
  -h localhost `
  -p 1883 `
  -t /iot/key-th-001/telemetry `
  -m "$payload"
```

## 持续随机发送数据

每 2 秒发送一次随机温度、湿度、压力数据。

```powershell
while ($true) {
  $payload = @{
    temperature = Get-Random -Minimum 70 -Maximum 100
    humidity = Get-Random -Minimum 40 -Maximum 80
    pressure = Get-Random -Minimum 95 -Maximum 120
  } | ConvertTo-Json -Compress

  docker exec pandax-mqtt-broker mosquitto_pub `
    -h localhost `
    -p 1883 `
    -t /iot/key-th-001/telemetry `
    -m "$payload"

  Start-Sleep -Seconds 2
}
```

## 固定趋势数据测试规则

温度从 70 逐步升到 100，适合演示达到阈值后触发告警。

```powershell
for ($temp = 70; $temp -le 100; $temp += 2) {
  $payload = @{
    temperature = $temp
    humidity = 55
    pressure = 101.3
  } | ConvertTo-Json -Compress

  docker exec pandax-mqtt-broker mosquitto_pub `
    -h localhost `
    -p 1883 `
    -t /iot/key-th-001/telemetry `
    -m "$payload"

  Start-Sleep -Seconds 1
}
```

## 如果 PowerShell 仍然丢失双引号

先进入 Mosquitto 容器：

```powershell
docker exec -it pandax-mqtt-broker sh
```

然后在容器内执行：

```sh
mosquitto_pub -h localhost -p 1883 -t /iot/key-th-001/telemetry -m '{"temperature":91.2,"humidity":51,"pressure":102.4}'
```

## CMD 单条测试命令

在 `cmd.exe` 中执行，不要在 PowerShell 中执行。

```cmd
docker exec pandax-mqtt-broker mosquitto_pub -h localhost -p 1883 -t /iot/key-th-001/telemetry -m "{\"temperature\":91.2,\"humidity\":51,\"pressure\":102.4}"
```

后端日志中应该看到：

```text
payload={"temperature":91.2,"humidity":51,"pressure":102.4}
MQTT telemetry processed: ...
```

## CMD 持续随机发送数据

如果只是在 `cmd.exe` 里临时发一条随机数据，取模符号用单个 `%`：

```cmd
set /a "temperature=%random% % 31 + 70"
set /a "humidity=%random% % 41 + 40"
set /a "pressure=%random% % 26 + 95"
docker exec pandax-mqtt-broker mosquitto_pub -h localhost -p 1883 -t /iot/key-th-001/telemetry -m "{\"temperature\":%temperature%,\"humidity\":%humidity%,\"pressure\":%pressure%}"
```

持续发送不要直接一行行粘贴到 CMD 窗口，建议保存为 `send-mqtt-loop.cmd` 后运行。保存成 `.cmd` 文件时，取模符号用 `%%`：

```cmd
@echo off
:loop
set /a "temperature=%random% %% 31 + 70"
set /a "humidity=%random% %% 41 + 40"
set /a "pressure=%random% %% 26 + 95"

docker exec pandax-mqtt-broker mosquitto_pub -h localhost -p 1883 -t /iot/key-th-001/telemetry -m "{\"temperature\":%temperature%,\"humidity\":%humidity%,\"pressure\":%pressure%}"

timeout /t 2 /nobreak >nul
goto loop
```

## CMD 趋势升温测试

如果直接粘贴到 `cmd.exe` 中执行：

```cmd
@echo off
for /l %t in (70,2,100) do (
  docker exec pandax-mqtt-broker mosquitto_pub -h localhost -p 1883 -t /iot/key-th-001/telemetry -m "{\"temperature\":%t,\"humidity\":55,\"pressure\":101.3}"
  timeout /t 1 /nobreak >nul
)
```

如果保存到 `.cmd` 文件中执行，变量要写成 `%%t`：

```cmd
@echo off
for /l %%t in (70,2,100) do (
  docker exec pandax-mqtt-broker mosquitto_pub -h localhost -p 1883 -t /iot/key-th-001/telemetry -m "{\"temperature\":%%t,\"humidity\":55,\"pressure\":101.3}"
  timeout /t 1 /nobreak >nul
)
```

## CMD 仍然不行时的稳定方案

先进入 Mosquitto 容器：

```cmd
docker exec -it pandax-mqtt-broker sh
```

进入容器后执行单条发送：

```sh
mosquitto_pub -h localhost -p 1883 -t /iot/key-th-001/telemetry -m '{"temperature":91.2,"humidity":51,"pressure":102.4}'
```

进入容器后执行持续发送：

```sh
while true; do
  temperature=$((70 + RANDOM % 31))
  humidity=$((40 + RANDOM % 41))
  pressure=$((95 + RANDOM % 26))
  mosquitto_pub -h localhost -p 1883 -t /iot/key-th-001/telemetry -m "{\"temperature\":$temperature,\"humidity\":$humidity,\"pressure\":$pressure}"
  sleep 2
done
```

## 预期效果

- 后端日志出现 MQTT 接收和处理记录
- 历史数据自动新增记录
- 满足规则时历史告警自动新增记录
- 顶部消息图标出现未读告警
- 高等级告警会在页面中上方弹出提示
- WebSocket 生效后，页面不需要手动刷新

## 停止发送

在 PowerShell 窗口中按：

```text
Ctrl + C
```
