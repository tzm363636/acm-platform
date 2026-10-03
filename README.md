# ACM Code Share

ACM / OJ 与代码分享个人平台的第一阶段：响应式首页和 Spring Boot 健康接口。当前没有真实判题、文章发布、账户或数据库；首页中的代码运行和文章都是演示。

## 目录

- `frontend/`：Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios、Tailwind CSS。首页样式主要在 `src/styles/main.css`，各区块在 `src/components/`。
- `backend/`：Java 21、Spring Boot 3。当前只提供 `GET /api/health`。

## 本地运行

需要 Node.js 20+、npm 和 JDK 21。

```powershell
cd frontend
npm install
npm run dev
```

另开一个终端：

```powershell
cd backend
$env:JAVA_HOME = (Resolve-Path ../.tools/jdk-21.0.12.1).Path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

当前电脑默认是 Java 17，因此上面的两行会让此终端使用项目 `.tools/` 中已准备好的 Java 21。换电脑时可安装自己的 JDK 21 并设置 `JAVA_HOME`；`.tools/` 不会提交到 Git。

前端地址以 Vite 输出为准。后端接口为 `http://127.0.0.1:8080/api/health`，返回 `{"code":200,"message":"ACM Platform Backend Running"}`。前端开发服务器已将 `/api` 代理到后端。

```powershell
cd frontend
npm run build
```

构建结果位于 `frontend/dist/`。前端可作为静态站点发布；GitHub Pages 无法运行 Spring Boot 服务，后端未来需要单独部署。
