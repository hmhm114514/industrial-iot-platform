# 工业互联网平台 API 清单

## 1. 接口约定

- 后端基础地址：`http://localhost:8080`
- API 统一前缀：`/api`
- 前端开发代理：`/api -> http://localhost:8080`
- 鉴权方式：除登录接口外，请求头携带 `Authorization: Bearer panda-iot-demo-token`
- 统一响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 2. 认证接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 用户登录 | Body：`username`、`password`；默认 `admin/123456` |

## 3. 仪表板接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| GET | `/api/dashboard/summary` | 获取统计卡片数据 | 产品数、分组数、设备数、在线设备数、告警数、任务数等 |
| GET | `/api/dashboard/charts` | 获取图表数据 | 设备状态、告警等级、近 7 日、最近遥测和告警 |
| GET | `/api/dashboard/monitor` | 获取服务监控 | CPU、内存、磁盘、MQTT、HTTP、JVM 线程等演示指标 |

## 4. 设备管理接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| GET | `/api/product-categories` | 查询产品分类 | 可选 `keyword` |
| POST | `/api/product-categories` | 新增产品分类 | Body 对应 `ProductCategory` |
| PUT | `/api/product-categories/{id}` | 修改产品分类 | Body 对应 `ProductCategory` |
| DELETE | `/api/product-categories/{id}` | 删除产品分类 | 路径参数 `id` |
| POST | `/api/product-categories/{id}/toggle` | 切换产品分类状态 | ENABLED/DISABLED |
| GET | `/api/products` | 查询产品 | 可选 `keyword` |
| POST | `/api/products` | 新增产品 | `name`、`code`、`categoryId`、`protocol`、`manufacturer` |
| PUT | `/api/products/{id}` | 修改产品 | 路径参数 `id` |
| DELETE | `/api/products/{id}` | 删除产品 | 路径参数 `id` |
| GET | `/api/device-groups` | 查询设备分组 | 可选 `keyword` |
| POST | `/api/device-groups` | 新增设备分组 | `name`、`code`、`parentId` |
| GET | `/api/devices` | 查询设备 | 可选 `keyword` |
| POST | `/api/devices` | 新增设备 | `productId`、`groupId`、`deviceKey`、位置等 |
| PUT | `/api/devices/{id}` | 修改设备 | 路径参数 `id` |
| DELETE | `/api/devices/{id}` | 删除设备 | 路径参数 `id` |
| POST | `/api/devices/{id}/toggle` | 切换设备状态 | ONLINE/OFFLINE 或 ENABLED/DISABLED 语义展示 |

## 5. 遥测与数据中心接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| POST | `/api/telemetry/simulate` | 模拟设备遥测上报 | Body：`deviceId`、`temperature`、`humidity`、`pressure` |
| GET | `/api/telemetry` | 查询最近 50 条遥测数据 | 按 `reportTime` 倒序 |
| GET | `/api/alarms` | 查询告警列表 | 可选 `keyword` |
| POST | `/api/alarms` | 新增告警 | 演示/测试使用 |
| PUT | `/api/alarms/{id}` | 修改告警 | 路径参数 `id` |
| DELETE | `/api/alarms/{id}` | 删除告警 | 路径参数 `id` |
| POST | `/api/alarms/{id}/handle` | 处置告警 | Body：`handler`、`remark` |
| POST | `/api/alarms/{id}/close` | 快速关闭告警 | 默认处理说明为“关闭告警” |

## 6. 规则引擎接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| GET | `/api/rules` | 查询规则 | 可选 `keyword` |
| POST | `/api/rules` | 新增规则 | `metric`、`operator`、`threshold`、`enabled`、`alarmLevel` |
| PUT | `/api/rules/{id}` | 修改规则 | 路径参数 `id` |
| DELETE | `/api/rules/{id}` | 删除规则 | 路径参数 `id` |
| POST | `/api/rules/{id}/toggle` | 启停规则 | 同步切换 `status` 和 `enabled` |
| GET | `/api/rule-audits` | 查询规则审计 | 规则执行 PASS/HIT 记录 |

## 7. 服务管理接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| GET | `/api/network-services` | 查询网络服务 | MQTT/HTTP 服务台账 |
| POST | `/api/network-services` | 新增网络服务 | `type`、`host`、`port` |
| PUT | `/api/network-services/{id}` | 修改网络服务 | 路径参数 `id` |
| DELETE | `/api/network-services/{id}` | 删除网络服务 | 路径参数 `id` |
| POST | `/api/network-services/{id}/start` | 启动网络服务 | 设置状态 RUNNING |
| POST | `/api/network-services/{id}/stop` | 停止网络服务 | 设置状态 STOPPED |
| GET | `/api/scripts` | 查询解析脚本 | 可选 `keyword` |
| POST | `/api/scripts` | 新增解析脚本 | `language`、`script` |

## 8. 组态大屏接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| GET | `/api/screens` | 查询大屏 | 大屏列表和分组 |
| POST | `/api/screens` | 新增大屏 | `groupName`、`configJson` |
| PUT | `/api/screens/{id}` | 修改大屏 | 路径参数 `id` |
| DELETE | `/api/screens/{id}` | 删除大屏 | 路径参数 `id` |
| POST | `/api/screens/{id}/publish` | 发布大屏 | 设置 `published=true`、`status=PUBLISHED` |

## 9. 视频中心接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| GET | `/api/video-devices` | 查询视频设备 | 摄像头/国标设备台账 |
| POST | `/api/video-devices` | 新增视频设备 | `channelNo`、`streamUrl`、`location` |
| GET | `/api/video-streams` | 查询拉流代理 | FLV/HLS/RTSP 等演示代理 |
| POST | `/api/video-streams` | 新增拉流代理 | `videoDeviceId`、`playUrl`、`protocol` |
| GET | `/api/video-alarm-tasks` | 查询视频告警任务 | 算法任务台账 |
| POST | `/api/video-alarm-tasks` | 新增视频告警任务 | `videoDeviceId`、`algorithm`、`enabled` |

## 10. 任务中心接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| GET | `/api/tasks` | 查询任务 | 可选 `keyword` |
| POST | `/api/tasks` | 新增任务 | `cron`、`running`、`status` |
| PUT | `/api/tasks/{id}` | 修改任务 | 路径参数 `id` |
| DELETE | `/api/tasks/{id}` | 删除任务 | 路径参数 `id` |
| POST | `/api/tasks/{id}/start` | 启动任务 | 设置 `running=true`、`status=RUNNING`，写入任务日志 |
| POST | `/api/tasks/{id}/stop` | 停止任务 | 设置 `running=false`、`status=STOPPED`，写入任务日志 |
| GET | `/api/tasks/logs` | 查询任务日志 | 最近 50 条任务执行记录 |

## 11. 系统设置接口

| 方法 | 路径 | 用途 | 参数/说明 |
| --- | --- | --- | --- |
| GET | `/api/users` | 查询用户 | 用户台账 |
| POST | `/api/users` | 新增用户 | `username`、`password`、`realName`、`roleName` |
| GET | `/api/roles` | 查询角色 | 角色和权限字符串 |
| POST | `/api/roles` | 新增角色 | `permissions` |
| GET | `/api/firmwares` | 查询固件 | 固件版本和升级状态 |
| POST | `/api/firmwares/{id}/upgrade` | 固件升级演示 | Body 可传 `status`，默认 `UPGRADING` |
| GET | `/api/operation-logs` | 查询操作日志 | 模块、动作、操作人、详情 |
| GET | `/api/login-logs` | 查询登录日志 | 用户名、IP、成功状态 |
