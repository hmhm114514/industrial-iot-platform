# 视频中心工业监控功能 AI 合并说明

> 适用对象：由另一个 AI 或开发人员把当前视频中心功能合并到包含其他并行开发成果的代码库。
>
> 核心原则：**新增文件可以直接复制；共享文件只能按本文指定代码段合并，禁止整文件覆盖。**

## 1. 目标功能

合并后，视频中心应只保留两个子功能：

1. `监控设备`：`/video/devices`
   - 主动授权并检测当前电脑连接的摄像头。
   - 将物理摄像头登记为工业监控设备。
   - 支持新增、查询、编辑、删除、启停和人工重新绑定。
   - 支持自定义设备名称、设备编码、监控用途、安装位置和备注。
   - 区分当前工作站、其他工作站、可用、未检测、检测失败和停用状态。

2. `监控查看`：`/video/monitor`
   - 只查看当前工作站已经登记且启用的监控设备。
   - 使用档案保存的 `browserDeviceId` 精确连接摄像头，禁止失败后回退到系统默认摄像头。
   - 自动协商摄像头能力范围内的最佳努力最高分辨率，不提供人工清晰度选项。
   - 支持镜像、截图、全屏和最长 5 分钟本地录像。

## 2. 合并前必须遵守的规则

执行合并的 AI 必须遵守：

1. 先运行 `git status --short` 和 `git diff`，识别其他开发人员尚未提交的改动。
2. 不得执行 `git reset --hard`、`git checkout -- <共享文件>` 或整目录覆盖。
3. 不得修改视频中心之外的业务功能。
4. 共享文件存在冲突时，只合并本文列出的最小代码段。
5. 不得重新引入 RTSP 演示地址、虚假视频设备、虚假录像、虚假告警事件或前端 fallback 数据。
6. 不得把浏览器 `deviceId` 当作全局永久硬件编号；它必须与当前浏览器工作站标识 `bindingClientId` 一起使用。
7. 重新绑定必须由用户明确确认，禁止根据设备名称或 `groupId` 静默绑定，避免连接错误的工业监控点。
8. 页面只能在 `localhost` 或 HTTPS 下使用摄像头。

## 3. 可以直接复制的新增文件

如果目标分支不存在下列文件，直接复制当前实现；如果已经存在，先比较内容再合并。

### 3.1 前端新增文件

#### `frontend/src/views/LocalCameraDevices.vue`

完整的“监控设备”管理页，负责：

- 调用浏览器权限检测本机视频输入。
- 临时授权流必须在 `finally` 中停止全部 track。
- 调用专用后端 API 管理设备档案。
- 创建时保存业务字段和当前工作站绑定字段。
- 普通编辑只允许修改业务字段，不允许修改绑定字段。
- 重新绑定必须调用专用 `rebind` 接口。
- 管理页列出全部 `LOCAL_CAMERA` 档案，以便 localStorage 被清除后仍可找回并人工重新绑定。
- 检测状态必须区分 `idle/loading/complete/error`。
- 权限拒绝或设备占用属于“检测失败”，不能显示成“未检测到”。

#### `frontend/src/api/video.js`

必须保留以下 API 封装：

```js
import http from './http'

const base = '/video-devices/local-cameras'

export const monitorDeviceApi = {
  list: (params) => http.get(base, { params }),
  get: (id) => http.get(`${base}/${id}`),
  create: (data) => http.post(base, data),
  update: (id, businessData) => http.put(`${base}/${id}`, businessData),
  rebind: (id, bindingData) => http.post(`${base}/${id}/rebind`, bindingData),
  toggle: (id) => http.post(`${base}/${id}/toggle`),
  remove: (id) => http.delete(`${base}/${id}`)
}
```

#### `frontend/src/utils/monitorClient.js`

负责生成并持久化当前浏览器工作站 ID：

```text
localStorage key: industrial-iot-monitor-client-id
ID prefix: monitor-client-
```

优先使用 `crypto.randomUUID()`；localStorage 不可用时使用当前页面生命周期内稳定的 fallback ID。

#### `frontend/src/utils/camera.js`

这是与 Vue 解耦的媒体控制器，必须保留以下能力：

- `enumerateDevices()` 视频设备枚举。
- `getUserMedia()` 精确设备连接。
- 原子切换：新流成功后才停止旧流。
- stop、switch、dispose 与异步请求之间的 token 竞态保护。
- 设备拔出后的 track ended 处理。
- 自动最高画质协商。
- MediaRecorder 本地录像及 5 分钟自动停止。
- Blob URL 创建和撤销异常保护。
- 订阅者异常隔离。

最高画质流程不得简化为固定 1080p：

1. 使用 exact `deviceId` 和宽松约束取得基础流。
2. 读取 `track.getCapabilities()`。
3. 校验 width、height、frameRate 为有限正数。
4. 使用最大 width/height 作为 `ideal`。
5. 帧率最大限制为 30fps。
6. `applyConstraints()` 失败时保留基础流，并通过 `qualityWarning` 提示降级。
7. 最终只显示 `track.getSettings()` 返回的真实宽高和帧率。
8. 在 `getUserMedia()` 和 `applyConstraints()` 两个 await 后都必须检查请求 token。

#### 前端测试

```text
frontend/tests/camera.test.js
frontend/tests/monitorClient.test.js
```

测试覆盖媒体竞态、资源清理、最高画质降级、录像停止、工作站 ID 持久化等行为。

### 3.2 后端新增文件

#### `backend/visual-video-service/src/main/java/com/practice/visual/controller/LocalCameraController.java`

专用接口根路径：

```text
/api/video-devices/local-cameras
```

接口：

```text
GET    /api/video-devices/local-cameras
GET    /api/video-devices/local-cameras/{id}
POST   /api/video-devices/local-cameras
PUT    /api/video-devices/local-cameras/{id}
POST   /api/video-devices/local-cameras/{id}/rebind
POST   /api/video-devices/local-cameras/{id}/toggle
DELETE /api/video-devices/local-cameras/{id}
```

列表支持：

```text
GET /api/video-devices/local-cameras?bindingClientId=<当前工作站ID>
```

不传 `bindingClientId` 时返回全部本机摄像头档案；传入时只返回当前工作站档案。

#### `backend/visual-video-service/src/main/java/com/practice/visual/dto/LocalCameraDtos.java`

必须保留三个 DTO：

1. `CreateRequest`
   - `name`
   - `code`
   - `purpose`
   - `location`
   - `remark`
   - `enabled`
   - `bindingClientId`
   - `browserDeviceId`
   - `browserGroupId`
   - `deviceLabel`

2. `UpdateRequest`
   - 只包含业务字段。
   - 禁止包含或修改绑定字段。

3. `RebindRequest`
   - 只包含新的工作站和浏览器设备绑定字段。

字段长度必须与当前实现一致，尤其是：

```text
bindingClientId  <= 128
browserDeviceId  <= 1024
```

#### `backend/visual-video-service/src/main/java/com/practice/visual/service/LocalCameraService.java`

必须保留：

- 服务端强制 `deviceType = LOCAL_CAMERA`。
- 只查询 `LOCAL_CAMERA`，不返回旧 GB28181 或其他 legacy 数据。
- 设备编码重复检查。
- `(bindingClientId, browserDeviceHash)` 重复绑定检查。
- 使用 SHA-256 计算 `browserDeviceHash`。
- 普通 update 不修改绑定。
- rebind 原子替换 `bindingClientId/deviceId/hash/groupId/label`。
- toggle 只在 `ENABLED` 和 `DISABLED` 之间切换。

#### 后端测试

```text
backend/visual-video-service/src/test/java/com/practice/visual/LocalCameraControllerTest.java
backend/visual-video-service/src/test/java/com/practice/visual/DataInitializerTest.java
backend/visual-video-service/src/test/resources/application-test.yml
```

测试必须使用独立内存 H2，不能连接开发文件数据库。

## 4. 可以整体替换的视频专用文件

### `frontend/src/views/VideoSquare.vue`

该文件已经从旧“视频广场”完全重构成“监控查看”。如果目标分支没有其他人修改视频查看，可整体采用当前文件。

必须保留以下关键不变量：

1. 页面只请求当前 `bindingClientId` 下已启用的档案。
2. URL query 中的 `deviceId` 表示后端档案 ID，不是浏览器设备 ID。
3. query 指向不存在、停用或其他工作站档案时，保持未选择并显示提示；禁止默认连接第一台设备。
4. 摄像头连接必须使用档案中的 exact `browserDeviceId`。
5. exact 连接失败时禁止回退系统默认设备。
6. `applySnapshot()` 收到流后，必须根据 `snapshot.deviceId === record.browserDeviceId` 同步当前档案和 query。
7. 刷新档案失败时，已有直播必须继续运行，只显示非阻断错误。
8. 档案被删除、停用、转移或重新绑定时，先停止旧流，再替换档案状态。
9. 开始录像时捕获设备名称和编码；切换设备后下载旧录像仍使用原设备名称。
10. 监控源按钮包含 `aria-pressed`。

### `backend/visual-video-service/src/main/java/com/practice/visual/entity/VideoDevice.java`

在保留原有 `channelNo`、`streamUrl`、`location` 的基础上增加：

```java
public String deviceType;
public String bindingClientId;
public String browserDeviceId;
public String browserDeviceHash;
public String browserGroupId;
public String deviceLabel;
public String purpose;
```

字段约束：

```text
deviceType          VARCHAR(30)
bindingClientId     VARCHAR(128)
browserDeviceId     VARCHAR(1024)
browserDeviceHash   VARCHAR(64), @JsonIgnore
browserGroupId      VARCHAR(255)
deviceLabel         VARCHAR(255)
purpose             VARCHAR(255)
```

唯一约束：

```text
code
(binding_client_id, browser_device_hash)
```

禁止把原始 `browserDeviceId VARCHAR(1024)` 放入联合唯一索引，否则 utf8mb4 下可能超过 InnoDB 3072 字节索引限制。

### `backend/visual-video-service/src/main/java/com/practice/visual/dao/VideoDeviceDao.java`

增加：

```java
findAllByDeviceTypeOrderByIdAsc
findAllByDeviceTypeAndBindingClientIdOrderByIdAsc
existsByCode
existsByCodeAndIdNot
existsByBindingClientIdAndBrowserDeviceHash
existsByBindingClientIdAndBrowserDeviceHashAndIdNot
```

## 5. 必须人工合并的共享文件

### 5.1 `frontend/src/config/menu.js`

只替换“视频中心”的 children，其他菜单全部保留：

```js
{
  title: '视频中心',
  icon: 'VideoCamera',
  children: [
    { title: '监控设备', path: '/video/devices', special: 'LocalCameraDevices' },
    { title: '监控查看', path: '/video/monitor', special: 'VideoSquare' }
  ]
}
```

删除旧菜单中的虚假或未实现入口：

```text
国标设备
拉流代理
视频广场
流媒体服务
录像回放
视频告警
```

### 5.2 `frontend/src/router/index.js`

新增 import：

```js
import LocalCameraDevices from '../views/LocalCameraDevices.vue'
```

在 `specialComponents` 中增加：

```js
LocalCameraDevices,
VideoSquare,
```

保留其他开发人员新增的页面和路由。

### 5.3 `frontend/src/assets/styles.css`

禁止整体覆盖。

只合并以下根选择器下的规则及其响应式规则：

```css
.local-camera-page { ... }
.local-camera-page ... { ... }
.monitor-device-page { ... }
.monitor-device-page ... { ... }
```

在 `@media (max-width: 1100px)` 和 `@media (max-width: 760px)` 中，也只合并以上两个根类开头的规则。

不得改动其他模块样式。

### 5.4 `frontend/package.json`

只在 scripts 中增加：

```json
"test": "node --test tests/camera.test.js tests/monitorClient.test.js"
```

没有新增 npm 运行时依赖，不应因为此功能修改 `package-lock.json`。

### 5.5 `backend/visual-video-service/src/main/java/com/practice/visual/controller/Controllers.java`

只修改旧 `VideoDeviceController`：

- `GET /api/video-devices` 和 `GET /api/video-devices/{id}` 保持只读兼容。
- `POST/PUT/DELETE/toggle` 返回 HTTP 405。
- 本机摄像头的所有写操作必须经过专用 `LocalCameraController`。

这样可以防止客户端绕过 DTO、绑定防重和字段校验，直接向旧接口写入 `LOCAL_CAMERA`。

如果其他开发人员仍然需要旧视频设备写接口，必须先进行人工架构协调，不能简单恢复通用写接口。

### 5.6 `backend/visual-video-service/src/main/java/com/practice/visual/init/DataInitializer.java`

保留其他开发人员新增的真实初始化逻辑，但删除以下虚假视频初始化：

```text
车间球机01
STREAM-001 拉流代理
AI-HELMET-001 安全帽识别任务
```

当前实现只依赖 `DashboardScreenDao`，初始化判断只检查 `screens.count()`，不能再用视频数据是否存在来阻止大屏初始化。

### 5.7 `backend/visual-video-service/pom.xml`

只增加：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

## 6. 数据库修改

### 6.1 新建数据库

合并 `database/schema-mysql.sql` 中新的 `video_device` 定义：

```sql
CREATE TABLE IF NOT EXISTS video_device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100),
  code VARCHAR(100),
  status VARCHAR(30),
  remark VARCHAR(255),
  device_type VARCHAR(30),
  binding_client_id VARCHAR(128),
  browser_device_id VARCHAR(1024),
  browser_device_hash VARCHAR(64),
  browser_group_id VARCHAR(255),
  device_label VARCHAR(255),
  purpose VARCHAR(255),
  channel_no VARCHAR(100),
  stream_url VARCHAR(500),
  location VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT uk_video_device_code UNIQUE (code),
  CONSTRAINT uk_video_device_binding_hash UNIQUE (binding_client_id, browser_device_hash)
);
```

### 6.2 已存在的 MySQL 数据库

`CREATE TABLE IF NOT EXISTS` 不会给已有表增加列。执行 AI 必须先检查：

```sql
SHOW COLUMNS FROM video_device;
SHOW INDEX FROM video_device;
```

然后只添加缺失列。典型迁移如下；已经存在的列或约束必须从 SQL 中删除后再执行：

```sql
ALTER TABLE video_device
  ADD COLUMN device_type VARCHAR(30),
  ADD COLUMN binding_client_id VARCHAR(128),
  ADD COLUMN browser_device_id VARCHAR(1024),
  ADD COLUMN browser_device_hash VARCHAR(64),
  ADD COLUMN browser_group_id VARCHAR(255),
  ADD COLUMN device_label VARCHAR(255),
  ADD COLUMN purpose VARCHAR(255);
```

添加唯一约束前先检查重复编码：

```sql
SELECT code, COUNT(*)
FROM video_device
WHERE code IS NOT NULL
GROUP BY code
HAVING COUNT(*) > 1;
```

确认无重复后执行：

```sql
ALTER TABLE video_device
  ADD CONSTRAINT uk_video_device_code UNIQUE (code),
  ADD CONSTRAINT uk_video_device_binding_hash
    UNIQUE (binding_client_id, browser_device_hash);
```

H2 开发环境使用 `spring.jpa.hibernate.ddl-auto=update`，启动后会自动补充实体字段。

### 6.3 演示数据

从 `database/data-mysql.sql` 删除以下三条演示 INSERT：

```text
video_device 的 rtsp://demo.pandax.local 数据
video_stream_proxy 的 mock FLV 数据
video_alarm_task 的安全帽识别演示任务
```

## 7. 前后端请求契约

### 7.1 新增设备

```json
{
  "name": "一号车床监控",
  "code": "MON-LATHE-001",
  "purpose": "监控车床运行状态",
  "location": "一号车间东侧",
  "remark": "主轴区域监控",
  "enabled": true,
  "bindingClientId": "monitor-client-...",
  "browserDeviceId": "浏览器返回的真实deviceId",
  "browserGroupId": "浏览器返回的groupId",
  "deviceLabel": "USB Camera"
}
```

### 7.2 编辑业务信息

编辑接口禁止携带绑定字段：

```json
{
  "name": "一号车床监控",
  "code": "MON-LATHE-001",
  "purpose": "监控车床运行状态和人员接近",
  "location": "一号车间东侧",
  "remark": "更新后的说明",
  "enabled": true
}
```

### 7.3 人工重新绑定

```json
{
  "bindingClientId": "monitor-client-...",
  "browserDeviceId": "用户明确选择的新deviceId",
  "browserGroupId": "新groupId",
  "deviceLabel": "新摄像头标签"
}
```

## 8. 不应修改的文件和模块

本功能不需要修改：

```text
frontend/src/api/http.js
frontend/src/api/platform.js
frontend/src/config/resources.js
frontend/src/utils/fallback.js
backend/gateway-service
backend/platform-common
其他设备、规则、任务、用户、数据中心等业务模块
```

现有网关对 `/api/video-devices/**` 的路由可以直接覆盖新专用接口，无需增加网关配置。

`.slim/deepwork/industrial-monitoring-devices.md` 是开发过程记录，不属于运行时功能，不需要复制。

## 9. 推荐合并顺序

执行 AI 按以下顺序操作：

1. 检查 git 状态和共享文件冲突。
2. 添加后端 DTO、service、controller。
3. 合并 `VideoDevice` 实体和 DAO。
4. 将旧 VideoDevice 通用写接口改为只读。
5. 合并初始化器和数据库脚本。
6. 添加后端测试并运行后端测试。
7. 添加前端 `camera.js`、`monitorClient.js` 和 `video.js`。
8. 添加 `LocalCameraDevices.vue`，替换或合并 `VideoSquare.vue`。
9. 最小合并菜单、路由和视频专用样式。
10. 添加前端测试脚本并运行测试、构建。
11. 检查最终 diff，确认没有视频中心之外的意外修改。

## 10. 验证命令

### 前端

```bash
cd frontend
npm test
npm run build
```

预期：

```text
43 tests passed
production build succeeded
```

### 后端

```bash
cd backend
mvn -q -pl visual-video-service -am test
```

预期：

```text
15 tests passed
0 failures
0 errors
```

## 11. 浏览器人工验收

1. 使用 Chrome 或 Edge，通过 `http://localhost` 或 HTTPS 打开系统。
2. 进入“视频中心 → 监控设备”。
3. 点击“检测本机摄像头”，允许权限。
4. 确认摄像头指示灯只在检测期间短暂亮起，检测完成后临时流立即停止。
5. 新增两个设备档案，例如“车床监控”和“人员监控”。
6. 进入“监控查看”，选择档案并连接。
7. 确认画面使用所选摄像头，不会连接其他默认摄像头。
8. 验证设备切换、拔出、重新绑定、截图、镜像、全屏和录像。
9. 确认页面没有清晰度选择，只显示实际分辨率和帧率。
10. 停止后端服务后刷新档案，正在运行的本地直播不能被主动关闭。

## 12. 可直接交给另一个 AI 的提示词

```text
请先阅读仓库根目录 VIDEO_CENTER_AI_HANDOFF.md，然后把工业监控视频中心合并到当前分支。

要求：
1. 先检查 git status 和 git diff，保护当前分支中其他开发人员的改动。
2. 新增文件可以直接采用文档列出的当前实现。
3. menu.js、router/index.js、styles.css、package.json、Controllers.java、DataInitializer.java、pom.xml 和数据库脚本只能做最小代码段合并，禁止整文件覆盖。
4. 不修改视频中心之外的业务模块。
5. 必须保留工作站 bindingClientId、人工 rebind、exact deviceId、自动最高画质、媒体竞态保护和旧写接口防绕过逻辑。
6. 不得重新加入虚假 RTSP、视频代理、录像或告警 fallback 数据。
7. 完成后运行前端 43 项测试、前端生产构建和后端 15 项测试。
8. 最后输出修改文件清单、共享文件冲突处理、测试结果和仍需人工摄像头验收的项目。
```
