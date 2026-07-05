# database

本目录提供课程交付用 MySQL 脚本。后端默认使用 H2 文件数据库 `./data/iot-platform-db`，启动时由 JPA 自动建表并初始化演示数据；如需切换 MySQL，可先执行：

```bash
mysql -uroot -p < schema-mysql.sql
mysql -uroot -p < data-mysql.sql
```

然后在 `backend/src/main/resources/application.yml` 中将 datasource 改为 MySQL 连接，例如：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/industrial_iot_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

后端 `pom.xml` 已包含 `mysql-connector-j` 运行时依赖；切换 MySQL 时主要需要修改连接地址、账号、密码，并确认本机 MySQL 已创建数据库 `industrial_iot_platform`。

MySQL 脚本覆盖用户、角色、产品分类、产品、设备分组、设备、遥测、告警、规则、规则审计、网络服务、解析脚本、组态大屏、视频、任务、固件、操作日志和登录日志等表。
