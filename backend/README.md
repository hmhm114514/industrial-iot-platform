# Industrial IoT Platform Backend

Spring Boot 3 + Java 17 + Maven 的工业互联网/物联网平台微服务后端。

## 服务结构

```text
backend/
  pom.xml                  Maven parent
  platform-common/         公共模型与工具模块
  platform-core-service/   平台核心服务，端口 8081
  visual-video-service/    可视化与视频服务，端口 8082
  gateway-service/         API 网关服务，端口 8080
```

前端和外部调用统一访问 `gateway-service` 的 `/api/**`，网关负责转发到平台核心服务和可视化视频服务。前端 Vite 代理仍指向 `http://localhost:8080`。

## 启动

`backend/pom.xml` 是 Maven parent，父工程没有 Spring Boot 启动类，因此不要在 `backend` 根目录直接执行：

```bash
mvn spring-boot:run
```

Windows PowerShell 推荐使用一键启动脚本：

```powershell
cd backend
.\start-all.ps1
```

如果 PowerShell 执行策略阻止脚本运行，可以使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

也可以使用批处理脚本：

```bat
start-all.bat
```

启动脚本会先执行 `mvn -q -DskipTests package`，再用 `java -jar` 分别启动三个服务；不需要执行 `mvn install`。

手动启动时需要分别打开 3 个终端：

```bash
cd backend && mvn -q -DskipTests package
java -jar platform-core-service/target/platform-core-service-0.0.1-SNAPSHOT.jar
java -jar visual-video-service/target/visual-video-service-0.0.1-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
```

- 网关地址：`http://localhost:8080`
- 平台核心服务：`http://localhost:8081`
- 可视化视频服务：`http://localhost:8082`
- 前端统一 API 入口：`http://localhost:8080/api/**`
- H2 Console：按实际服务配置访问对应端口
- 核心服务 H2 JDBC URL：`jdbc:h2:file:./data/platform-core-db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE`
- 可视化视频服务 H2 JDBC URL：`jdbc:h2:file:./data/visual-video-db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE`
- 默认账号：`admin / 123456`

## 鉴权

登录：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}'
```

响应中的 token 用于访问其它 `/api/**` 接口：

```http
Authorization: Bearer panda-iot-demo-token
```

## 关键 API

统一响应结构：`{ code, message, data, timestamp }`。

### 核心闭环

- `POST /api/auth/login` 登录获取 token
- `GET /api/dashboard/summary` 仪表板统计
- `GET /api/dashboard/charts` 仪表板图表数据
- `GET/POST/PUT/DELETE /api/products` 产品 CRUD
- `GET/POST/PUT/DELETE /api/product-categories` 产品分类 CRUD
- `GET/POST/PUT/DELETE /api/device-groups` 设备分组 CRUD
- `GET/POST/PUT/DELETE /api/devices` 设备 CRUD
- `POST /api/telemetry/simulate` 模拟上报遥测，示例：`{"deviceId":1,"temperature":86.2,"humidity":45,"pressure":101.3}`
- `POST /api/telemetry/report` 外部设备遥测上报，示例：`{"deviceKey":"key-th-001","temperature":86.2,"humidity":45,"pressure":101.3}`
- MQTT Topic `/iot/{deviceKey}/telemetry`，示例 Topic：`/iot/key-th-001/telemetry`
- `GET /api/telemetry` 最近遥测数据
- `GET/POST/PUT/DELETE /api/rules` 温度/指标阈值规则
- `GET /api/rule-audits` 规则审计
- `GET /api/alarms` 告警查询
- `POST /api/alarms/{id}/handle` 告警处置
- `POST /api/alarms/{id}/close` 告警关闭
- `GET/POST/PUT/DELETE /api/tasks` 任务 CRUD
- `POST /api/tasks/{id}/start` 启动任务并生成任务日志
- `POST /api/tasks/{id}/stop` 停止任务并生成任务日志
- `GET /api/tasks/logs` 任务日志

### 扩展能力

- `GET/POST/PUT/DELETE /api/network-services` 网络服务 CRUD
- `POST /api/network-services/{id}/start|stop` 网络服务启停
- `GET/POST/PUT/DELETE /api/scripts` 解析脚本 CRUD
- `GET/POST/PUT/DELETE /api/screens` 组态大屏 CRUD
- `POST /api/screens/{id}/publish` 大屏发布
- `GET/POST/PUT/DELETE /api/video-devices` 视频设备 CRUD
- `GET/POST/PUT/DELETE /api/video-streams` 视频流代理 CRUD
- `GET/POST/PUT/DELETE /api/video-alarm-tasks` 视频告警任务 CRUD
- `GET/POST/PUT/DELETE /api/firmwares` 固件台账 CRUD
- `POST /api/firmwares/{id}/upgrade` 固件升级状态修改
- `GET /api/dashboard/monitor` 服务监控样例数据
- `GET/POST/PUT/DELETE /api/users` 用户管理
- `GET/POST/PUT/DELETE /api/roles` 角色管理
- `GET /api/operation-logs` 操作日志
- `GET /api/login-logs` 登录日志

列表接口支持 `?keyword=xxx` 关键词筛选；继承通用 CRUD 的关键实体支持 `POST /api/{resource}/{id}/toggle` 状态切换。

## 业务验证链路

1. 使用 `admin/123456` 登录。
2. 新增产品、设备分组、设备，或使用内置样例：`PandaX温湿度采集器`、`注塑机温度传感器01`。
3. 调用 `POST /api/telemetry/simulate` 上报温度。
4. 当温度超过启用规则 `温度超过80℃自动告警` 的阈值时，自动生成告警与规则审计。
5. 通过仪表板、历史数据、告警列表查看统计联动，再调用告警处置接口关闭告警。

也可以使用外部模拟设备脚本一次性完成全流程：

```powershell
cd ..\
powershell -ExecutionPolicy Bypass -File .\scripts\simulated-device-flow.ps1 -CloseAlarm
```

该脚本会登录平台、读取设备、通过 `/api/telemetry/report` 上传正常遥测和高温遥测、查询历史数据和告警，并可自动处置最新 OPEN 告警。详细说明见 `docs/模拟设备接入全流程.md`。

## MQTTX 接入

项目提供本地 Mosquitto 配置，便于 MQTTX 连接测试。先在项目根目录启动 Broker：

```powershell
cd ..
docker compose -f .\docker-compose.mqtt.yml up -d
```

再启动后端服务。`platform-core-service` 会订阅：

```text
/iot/+/telemetry
/iot/telemetry
```

MQTTX 连接参数：

```text
Host: localhost
Port: 1883
Username: 留空
Password: 留空
```

推荐发布 Topic：

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

发布后会进入与 HTTP 上报相同的遥测、规则审计和告警链路。详细说明见 `docs/MQTTX接入说明.md`。
