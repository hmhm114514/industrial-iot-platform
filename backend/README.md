# Industrial IoT Platform Backend

Spring Boot 3 + Java 17 + Maven 的工业互联网/物联网平台课程实践后端。

## 启动

```bash
cd industrial-iot-platform/backend
mvn spring-boot:run
```

- 服务地址：`http://localhost:8080`
- H2 Console：`http://localhost:8080/h2-console`
- H2 JDBC URL：`jdbc:h2:file:./data/iot-platform-db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE`
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

### 演示模块

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
- `GET /api/dashboard/monitor` 服务监控模拟数据
- `GET/POST/PUT/DELETE /api/users` 用户管理
- `GET/POST/PUT/DELETE /api/roles` 角色管理
- `GET /api/operation-logs` 操作日志
- `GET /api/login-logs` 登录日志

列表接口支持 `?keyword=xxx` 关键词筛选；继承通用 CRUD 的关键实体支持 `POST /api/{resource}/{id}/toggle` 状态切换。

## 演示链路

1. 使用 `admin/123456` 登录。
2. 新增产品、设备分组、设备，或使用内置样例：`PandaX温湿度采集器`、`注塑机温度传感器01`。
3. 调用 `POST /api/telemetry/simulate` 上报温度。
4. 当温度超过启用规则 `温度超过80℃自动告警` 的阈值时，自动生成告警与规则审计。
5. 通过仪表板、历史数据、告警列表查看统计联动，再调用告警处置接口关闭告警。
