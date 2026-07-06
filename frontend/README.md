# 工业互联网/物联网平台前端

技术栈：Vue 3 + Vite + Element Plus + ECharts。

## 启动

```bash
npm install
npm run dev
```

默认访问：`http://localhost:5173`

默认账号提示：`admin / 123456`

开发环境已配置代理：前端请求 `/api` 会转发到 `http://localhost:8080`，即后端 `gateway-service`。前端不直接访问 `platform-core-service(8081)` 或 `visual-video-service(8082)`。

## 构建

```bash
npm run build
```

后端接口未启动时，部分页面会使用前端内置样例数据降级展示，用于保持页面可浏览。实际业务验证应以网关和后端微服务返回的数据为准。
