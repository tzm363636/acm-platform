# ACM Code Share

ACM / OJ 与代码分享个人平台：Vue 多页面前端、Spring Boot API，以及 MySQL / Aiven 数据库接入。用户登录、文章投稿审核和管理员控制台已接入；OJ 运行／提交仍为固定演示流程，真实判题引擎未接入。

数据库设计、TLS、迁移、环境变量和启动步骤见 [backend/DATABASE.md](backend/DATABASE.md)。默认无数据库 profile 时保留健康接口；数据库模式须先显式迁移，再启动服务。
登录、两种角色、投稿流程、首个管理员初始化及控制台入口见 [backend/AUTH.md](backend/AUTH.md)。账号页面为 `account.html`，管理员控制台为 `account.html#/admin`。

## 目录

- `frontend/`：Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios、Tailwind CSS。首页样式主要在 `src/styles/main.css`，各区块在 `src/components/`。
- `backend/`：Java 21、Spring Boot 3、Spring JDBC、MySQL、Flyway。提供健康、文章、题库、提交记录与开发演示接口。

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

上面是无数据库的健康接口启动方式；数据库模式请按 `backend/DATABASE.md` 使用显式 check/migrate/serve。当前电脑默认是 Java 17，因此上面的两行会让此终端使用项目 `.tools/` 中已准备好的 Java 21。换电脑时可安装自己的 JDK 21 并设置 `JAVA_HOME`；`.tools/` 不会提交到 Git。

前端地址以 Vite 输出为准。数据库 profile 的默认 API 端口为 8080，无数据库 profile 的默认端口为 10000，也可用 PORT 指定。`GET /api/health` 返回 `{"code":200,"message":"ACM Platform Backend Running"}`。前端开发服务器将 `/api` 代理到 8080 后端。

```powershell
cd frontend
npm run build
```

构建结果位于 `frontend/dist/`。前端可作为静态站点发布；GitHub Pages 无法运行 Spring Boot 服务，后端未来需要单独部署。
