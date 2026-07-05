# 工业互联网/物联网平台前端

技术栈：Vue 3 + Vite + Element Plus + ECharts。

## 启动

```bash
npm install
npm run dev
```

默认访问：`http://localhost:5173`

默认账号提示：`admin / 123456`

开发环境已配置代理：前端请求 `/api` 会转发到 `http://localhost:8080`。

## 构建

```bash
npm run build
```

后端接口未启动时，页面会使用前端内置演示数据降级展示，便于课程答辩演示。
