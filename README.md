# 工业互联网平台项目

本项目是面向设备接入、运行监控、规则告警和可视化运维场景的工业互联网 / 物联网平台原型系统，参考 PandaX 物联网平台的功能域进行模块划分。

项目采用前端单页应用 + 后端微服务结构，实现“设备接入—数据上报—规则告警—统计展示—告警处置—日志审计”的核心业务闭环，并提供 PandaX 参考功能中的设备管理、服务管理、规则引擎、组态大屏、视频中心、数据中心、任务中心和系统设置等模块。

## 1. 项目结构

```text
industrial-iot-platform/
  backend/                  Spring Boot 微服务工程
    pom.xml                 Maven parent
    platform-common/        公共模型与工具模块
    platform-core-service/  平台核心服务，端口 8081
    visual-video-service/   可视化与视频服务，端口 8082
    gateway-service/        API 网关服务，端口 8080
  frontend/                 Vue3 前端单页应用
  database/                 MySQL 建表与示例数据脚本
  docs/                     项目实践报告文档
  README.md                 项目总说明
```

## 2. 技术栈

### 后端

- Java 17 目标版本，本机 Java 21 可运行
- Spring Boot 3 微服务架构
- Spring Web
- Spring Data JPA
- H2 文件数据库，默认开箱即用
- MySQL 脚本，用于项目交付和扩展部署
- 简化 Token 鉴权

后端当前由 `gateway-service`、`platform-core-service`、`visual-video-service` 和 `platform-common` 组成。前端统一访问 `gateway-service` 暴露的 `/api/**`，网关再转发到平台核心服务和可视化视频服务。
其中业务服务内部采用 Controller、Service、DAO 三层架构：Controller 负责接口入口，Service 负责编排业务流程，DAO 基于 Spring Data JPA 负责数据访问。

### 前端

- Vue 3
- Vite
- Element Plus
- ECharts
- Axios

## 3. 默认账号

```text
账号：admin
密码：123456
Token：panda-iot-demo-token
```

前端登录页会提示默认账号。登录成功后，前端会把 Token 保存到 `localStorage`，后续请求自动携带：

```http
Authorization: Bearer panda-iot-demo-token
```

## 4. 后端启动

`backend/pom.xml` 是 Maven parent，不能直接执行 `mvn spring-boot:run`。后端需要启动 3 个 Spring Boot 服务：核心服务、可视化视频服务和网关服务。

Windows PowerShell 推荐使用一键启动脚本：

```powershell
cd backend
.\start-all.ps1
```

如果 PowerShell 执行策略阻止脚本运行，可以使用：

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

也可以使用批处理脚本：

```bat
cd backend
start-all.bat
```

启动脚本会先执行 `mvn -q -DskipTests package`，再用 `java -jar` 分别启动三个服务；不需要在本地 Maven 仓库执行 `install`。

手动启动时需要分别打开 3 个终端：

```bash
cd backend && mvn -q -DskipTests package
java -jar platform-core-service/target/platform-core-service-0.0.1-SNAPSHOT.jar
java -jar visual-video-service/target/visual-video-service-0.0.1-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
```

服务端口：

```text
gateway-service: http://localhost:8080
platform-core-service: http://localhost:8081
visual-video-service: http://localhost:8082
```

前端和外部调用统一访问网关：

```text
/api/** -> http://localhost:8080
```

H2 Console 与数据源由后端服务配置提供。核心服务和可视化视频服务使用独立 H2 文件库：

```text
jdbc:h2:file:./data/platform-core-db
jdbc:h2:file:./data/visual-video-db
```

默认 H2 数据库文件：

```text
backend/platform-core-service/data/platform-core-db
backend/visual-video-service/data/visual-video-db
```

## 5. 前端启动

进入前端目录：

```bash
cd frontend
```

安装依赖：

```bash
npm install
```

启动开发服务：

```bash
npm run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

前端默认通过 Vite 代理访问后端：

```text
/api -> http://localhost:8080
```

生产构建：

```bash
npm run build
```

## 6. 数据库脚本

`database/` 目录提供 MySQL 交付脚本：

- `schema-mysql.sql`：建表脚本
- `data-mysql.sql`：示例数据
- `README.md`：数据库说明与切换建议

默认运行使用 H2，便于本地快速启动和功能验证；如需 MySQL，可按 `database/README.md` 修改后端数据源配置。
后端 `pom.xml` 已包含 `mysql-connector-j` 运行时依赖，切换到 MySQL 时无需额外添加驱动。

## 7. 已实现模块

### 控制台 / 仪表板

- 设备数量、在线设备、产品数量、告警数量、今日消息、任务数量
- 消息上报趋势
- 设备状态占比
- 近 7 日新增设备
- 告警趋势

### 设备管理

- 产品分类
- 产品管理
- 设备分组
- 设备管理
- 设备地图位置展示
- 设备模拟遥测上报

### 服务管理

- 网络通信服务
- 接入服务启停
- 解析脚本管理

### 规则引擎

- 温度阈值规则配置
- 规则启停
- 规则审计记录
- 超阈值自动生成告警

### 组态大屏

- 大屏分组
- 组态大屏列表
- 大屏预览 / 发布状态

### 视频中心

- 视频设备台账
- 流代理配置
- 视频广场通道状态与 1 / 4 / 9 分屏布局
- 视频告警任务

### 数据中心

- 历史遥测数据
- 历史告警
- 告警处置
- 服务监控

### 任务中心

- 定时任务新增、编辑、删除
- 任务启停
- 任务日志

### 系统设置

- 用户管理
- 角色管理
- 固件管理
- 操作日志
- 登录日志

## 8. 核心业务验证链路

建议按以下步骤进行本地运行验证：

1. 启动后端和前端。
2. 使用 `admin / 123456` 登录平台。
3. 进入“设备管理 / 产品管理”，查看或新增产品。
4. 进入“设备管理 / 设备管理”，查看示例设备。
5. 点击设备的“模拟上报”，上报温度值，例如 `88.6℃`。
6. 后端写入历史遥测数据，并执行温度阈值规则。
7. 如果超过阈值，系统自动生成告警和规则审计记录。
8. 进入“数据中心 / 历史数据”，查看遥测记录。
9. 进入“数据中心 / 历史告警”，查看并处置告警。
10. 返回仪表板，观察告警和消息统计变化。
11. 进入“系统设置 / 操作日志”和“登录日志”，查看审计记录。

## 9. 关键后端 API

| 功能 | API |
| --- | --- |
| 登录 | `POST /api/auth/login` |
| 仪表板统计 | `GET /api/dashboard/summary` |
| 仪表板图表 | `GET /api/dashboard/charts` |
| 服务监控 | `GET /api/dashboard/monitor` |
| 产品分类 | `/api/product-categories` |
| 产品管理 | `/api/products` |
| 设备分组 | `/api/device-groups` |
| 设备管理 | `/api/devices` |
| 模拟遥测上报 | `POST /api/telemetry/simulate` |
| 历史遥测 | `GET /api/telemetry` |
| 告警管理 | `/api/alarms` |
| 告警处置 | `POST /api/alarms/{id}/handle` |
| 规则管理 | `/api/rules` |
| 规则审计 | `/api/rule-audits` |
| 任务管理 | `/api/tasks` |
| 任务启动 | `POST /api/tasks/{id}/start` |
| 任务停止 | `POST /api/tasks/{id}/stop` |
| 任务日志 | `GET /api/tasks/logs` |

## 10. 验证结果

已完成以下验证：

```bash
# 后端构建
cd backend
mvn -q -DskipTests package

# 前端构建
cd frontend
npm install
npm run build
```

验证结果：

- 后端 Maven 构建成功。
- 后端 Jar 可启动，H2/JPA 初始化成功。
- 登录接口验证成功。
- 仪表板统计接口验证成功。
- 设备遥测模拟上报验证成功。
- 温度超阈值自动生成告警验证成功。
- 任务启动接口验证成功。
- 前端 Vite 构建成功。

前端构建存在非阻断提示：

- `npm audit` 提示 1 个 moderate vulnerability，可按需要后续审计。
- ECharts + Element Plus 打包体积较大，Vite 提示 chunk 超过 500kB；不影响本地运行验证运行。

## 11. 项目实践文档

`docs/` 目录已整理项目说明、设计、接口、数据和验证文档：

| 文档 | 说明 |
| --- | --- |
| `docs/README.md` | 文档目录与阅读顺序 |
| `docs/迭代记录.md` | 项目实践过程迭代记录 |
| `docs/需求分析报告.md` | 业务背景、用户角色、功能与非功能需求 |
| `docs/总体规划报告.md` | 建设目标、技术路线、模块规划和实施计划 |
| `docs/架构设计报告.md` | 前端、网关、微服务、数据库、鉴权和核心流程 |
| `docs/平台设计报告.md` | 各业务模块页面和流程设计 |
| `docs/项目报告.md` | 项目归档主体报告 |
| `docs/功能覆盖矩阵.md` | 参考资料/PandaX 功能点与实现位置映射 |
| `docs/产品介绍与技术说明.md` | 产品介绍、业务验证流程和技术说明问答 |
| `docs/API清单.md` | 核心 REST API 列表 |
| `docs/数据库设计.md` | 主要表结构、关系和 H2/MySQL 策略 |
| `docs/测试用例.md` | 功能、接口、异常、构建和验证测试用例 |

## 12. 常见问题

### 1. 前端登录后接口 401

确认后端已启动，并且请求头包含：

```http
Authorization: Bearer panda-iot-demo-token
```

### 2. 前端页面显示样例数据

当前端无法访问后端接口时，会自动使用样例数据降级，以保证页面仍可展示。请确认后端端口为 `8080`，前端代理配置没有被修改。

### 3. H2 数据异常或想恢复初始数据

停止后端后删除：

```text
backend/data/
```

再次启动后端会重新初始化示例数据。

### 4. Node 版本提示

本机验证环境为 Node.js v24.16.0、npm 11.13.0。一般 Node 18+ 均可运行。

## 13. 交付说明

本项目面向项目实践验证，采用“完整可运行原型 + 核心闭环真实实现 + 复杂模块轻量化实现”的方式控制范围：

- P0 真实实现：登录、设备/产品管理、遥测上报、规则告警、历史数据、告警处置、任务管理、日志审计。
- P1 当前版本覆盖：网络服务、解析脚本、可视化大屏配置与发布、视频设备台账、流代理配置、视频广场通道状态、服务监控、固件管理。
- P2 文档说明：AI 智能体、开发工具等 PandaX 扩展能力。

真实流媒体播放、GB28181 级联和拖拽组态编辑器属于后续演进方向，当前版本不声明为完整生产级实现。
