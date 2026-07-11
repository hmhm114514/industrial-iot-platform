# MQTTX 接入说明

本文档用于通过 MQTTX 将模拟设备数据发布到平台，并验证数据采集、数据上传、规则审计和告警生成流程。

## 1. 接入结构

```text
MQTTX
-> MQTT Broker(localhost:1883)
-> platform-core-service MQTT 订阅端
-> 遥测入库
-> 规则判断
-> 规则审计
-> 告警生成
```

后端核心服务启动后会订阅：

```text
/iot/+/telemetry
/iot/telemetry
```

其中推荐使用：

```text
/iot/key-th-001/telemetry
```

`key-th-001` 是系统初始化设备“注塑机温度传感器01”的 `deviceKey`。

## 2. 启动 MQTT Broker

如果电脑安装了 Docker，可在项目根目录启动 Mosquitto：

```powershell
cd D:\作业\工业互联网平台开发实践\industrial-iot-platform
docker compose -f .\docker-compose.mqtt.yml up -d
```

检查容器：

```powershell
docker ps
```

停止 Broker：

```powershell
docker compose -f .\docker-compose.mqtt.yml down
```

如果不使用 Docker，也可以单独安装 EMQX 或 Mosquitto，只要保证 Broker 地址为：

```text
localhost:1883
```

## 3. 启动后端服务

后端启动：

```powershell
cd D:\作业\工业互联网平台开发实践\industrial-iot-platform\backend
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

核心服务日志中如果出现下面内容，说明 MQTT 订阅已连接：

```text
MQTT telemetry subscriber connected: tcp://localhost:1883, topic=/iot/+/telemetry
```

如果先启动后端、后启动 Broker，后端可能已经错过首次连接。此时重启 `platform-core-service` 或重新执行后端启动脚本即可。

## 4. MQTTX 连接参数

在 MQTTX 中新建连接：

| 参数 | 值 |
| --- | --- |
| Name | PandaX Local MQTT |
| Host | `localhost` |
| Port | `1883` |
| Client ID | 任意，例如 `mqttx-device-001` |
| Username | 留空 |
| Password | 留空 |
| SSL/TLS | 关闭 |

连接成功后，状态应显示为已连接。

## 5. 发布正常遥测数据

Topic：

```text
/iot/key-th-001/telemetry
```

Payload：

```json
{
  "temperature": 35.5,
  "humidity": 58,
  "pressure": 101.2
}
```

发布后，平台会根据 Topic 中的 `key-th-001` 找到设备，并写入遥测数据。该温度低于规则阈值，规则审计结果应为 `PASS`，不会新增告警。

## 6. 发布高温告警数据

Topic：

```text
/iot/key-th-001/telemetry
```

Payload：

```json
{
  "temperature": 91.2,
  "humidity": 51,
  "pressure": 102.4
}
```

发布后，平台会执行 `temperature > 80` 规则，规则审计结果应为 `HIT`，并生成 `HIGH` 等级告警。

## 7. 另一种 Topic 写法

也可以使用固定 Topic：

```text
/iot/telemetry
```

此时 Payload 必须包含 `deviceKey`：

```json
{
  "deviceKey": "key-th-001",
  "temperature": 88.6,
  "humidity": 49,
  "pressure": 101.7
}
```

## 8. 页面检查点

发布 MQTT 消息后，进入前端页面检查：

| 页面 | 检查内容 |
| --- | --- |
| 设备管理 / 设备管理 | 设备状态变为 ONLINE，最后上线时间更新 |
| 数据中心 / 历史数据 | 出现 MQTTX 发布的温度、湿度、压力数据 |
| 规则引擎 / 规则审计 | 正常数据为 PASS，高温数据为 HIT |
| 数据中心 / 历史告警 | 高温数据生成 HIGH 等级 OPEN 告警 |
| 控制台 | 今日消息、告警数量和趋势图更新 |

## 9. 常见问题

### MQTTX 连接失败

先确认 Broker 是否启动：

```powershell
docker ps
```

如果没有容器，重新启动：

```powershell
docker compose -f .\docker-compose.mqtt.yml up -d
```

### MQTTX 能连接，但平台没有数据

检查后端核心服务是否在 Broker 启动之后启动。核心服务日志应出现：

```text
MQTT telemetry subscriber connected
```

如果没有，重启后端核心服务。

### 规则没有生成告警

检查规则是否启用：

```text
规则名称：温度超过80℃自动告警
指标：temperature
运算符：>
阈值：80
启用状态：启用
```

### 设备不存在

检查 Topic 中的设备 Key：

```text
/iot/key-th-001/telemetry
```

或 Payload 中的：

```json
{
  "deviceKey": "key-th-001"
}
```
