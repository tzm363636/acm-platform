# Render 登录请求失败：接入与验收

## 已确认的问题与修复范围

2026-10-07 对现有公网服务的只读检查发现：

- 前端 `https://acm-platform-web.onrender.com` 的 `/api/auth/csrf` 与 `/api/auth/me` 返回静态站点 404。
- 后端 `https://acm-platform-api.onrender.com/api/health` 返回 200，但 `/api/capabilities` 的 database/login 均为 false；认证接口为 404。无数据库 profile 只开放健康功能。
- 后端不开放跨域 Session 请求。Vite 的开发 proxy 不包含在构建后的静态文件中。
- 本地现有 Aiven Host 在系统 DNS 中解析失败，独立公共 DNS 查询返回 NXDOMAIN。必须以当前 Aiven 控制台 Host 和 Running 状态核对；不猜测新地址或修改密码。

本次保持浏览器 `/api` 同源请求、Session、CSRF 和 Aiven `VERIFY_IDENTITY`。不修改数据库结构、账号密码、内容、审核流程或 OJ 演示隔离。`render.yaml` 是配置参考，**不会因本地保存而自动修改既有服务**。本文件中的配置准备完成与公网部署成功是不同状态。

## 1. 配置已有后端 acm-platform-api

Docker 构建上下文：仓库的 `backend`，Dockerfile：`backend/Dockerfile`（路径依 Render 是否设置 Root Directory 而定）。仓库根目录方式见 render.yaml；若 Root Directory 为 backend，则使用 `./Dockerfile`、context `.`。不要新建同名服务或第二套数据库。

按 `backend/render.env.example` 在该服务 Environment 填写：

| 变量 | 含义 |
|---|---|
| SPRING_PROFILES_ACTIVE | `aiven,render`；同时启用数据库与 Render 部署配置，保持此顺序 |
| DB_ACTION | `serve`，只验证已有迁移，不自动迁移、播种或初始化管理员 |
| DB_ENVIRONMENT | `cloud` |
| DB_HOST / DB_PORT / DB_NAME / DB_USER | 实际 Aiven 服务及已有数据库信息 |
| DB_PASSWORD | 仅后端的私密环境变量 |
| DB_EXPECTED_MAJOR | 已确认的 MySQL 主版本；当前项目服务为 `8` |
| DB_POOL_MAX | 当前单实例可先使用 `2`，扩实例前重新核算连接预算 |
| DB_CA_CERT_PATH | Render Secret File 的 `/etc/secrets/ca.pem` |
| DB_TRUSTSTORE_PASSWORD | 独立的私密信任库密码，至少 6 字符 |

在后端添加 Secret File，名称 `ca.pem`，内容为对应 Aiven 服务下载的 CA PEM。不要上传 Windows 私有配置、管理员初始化文件或包含其他凭据的文件。Secret File 的绝对路径由 Render 提供，不是电脑路径。

容器入口用 `keytool -storepass:env` 将 CA 导入临时 PKCS12，密码不进入命令行或日志；JDBC 继续校验证书和主机名。也可以挂载已有 PKCS12，配置 DB_TRUSTSTORE_PATH 与 DB_TRUSTSTORE_PASSWORD，此时不需要 DB_CA_CERT_PATH。缺少 CA/信任库时云容器拒绝启动。

render profile 监听 `0.0.0.0`，使用 Render 提供的 PORT，启用 Secure/HttpOnly/SameSite=Lax Session Cookie。本地非容器 mysql-local/aiven 启动方式保持原样。不要在 Render 添加旧的 Windows `spring.config.additional-location` 或将启动命令覆盖为没有数据库 profile 的命令。

部署后先检查后端 `/api/capabilities` 的 database/login 为 true。只有 `/api/health` 为 200 不代表数据库和登录已启用。当前已有迁移应可通过校验；若服务提示有未执行迁移，先执行原有 check/inspect，审阅 pending 迁移及目标环境，确认生产变更后再用已有 `DB_ACTION=migrate` 独立命令执行。不要将 migrate 放入每个实例的启动命令，不要重新导入演示数据或重置管理员密码。

## 2. 配置已有前端 acm-platform-web

Environment 只设置 `VITE_DATA_SOURCE=api`，构建命令 `cd frontend && npm ci && npm run build`，发布目录 `frontend/dist`（若 Root Directory 为 frontend，则 `npm ci && npm run build`、`dist`）。任何 DB_* 或管理员凭据都不能进入前端服务或 VITE_*。

在该 Static Site 的 Redirects/Rewrites 添加以下规则：

| Source | Destination | Action |
|---|---|---|
| `/api/*` | `https://acm-platform-api.onrender.com/api/*` | **Rewrite** |

保留 `/api` 前缀和通配符；不要用 Redirect，不要把 API 改为跨域地址，不要把 `/api/*` 指向 index.html。此项目为多页面架构，不需要添加 `/* → /index.html`。已有兜底规则若存在，API 规则应排在它之前。

Render 文档允许 Rewrite 的目标为公开 URL；规则见 [Redirects and Rewrites](https://render.com/docs/redirects-rewrites)，配置字段见 [Blueprint specification](https://render.com/docs/blueprint-spec)。已有服务可直接修改上述配置；不要仅上传 Blueprint 就认为既有服务已同步。`sync: false` 的新增变量也需要在既有服务手动填写。

## 3. 验收

先运行不发送账号密码、不修改业务数据的检查：

```powershell
./scripts/Check-Render.ps1
```

检查后端健康与能力、前端同源 API、匿名身份、CSRF JSON、Cookie 属性，以及匿名 POST 的 JSON 401。脚本不输出 Cookie 值或 CSRF 令牌。任何失败退出码为 1；应先解决对应部署配置，而不是改账号密码。

随后在前端登录页用已有账号实际验证：

1. 登录成功，刷新仍保持身份；Network 请求地址始终为前端域名 `/api/...`。
2. `/api/auth/me` 返回本人，Set-Cookie 未被拦截；Cookie 有 HttpOnly、Secure、SameSite=Lax。
3. 管理员可进入控制台，普通用户仍不能访问管理员接口。
4. 退出成功，私人接口返回 401；再次登录可用。错误密码应显示“用户名或密码错误”，不能显示服务接入错误。
5. 文章列表和详情、个人资料等通过相同代理访问；OJ 仍标注演示。

**公网 Rewrite 的 POST、Cookie 透传及实际登录必须在应用配置后实测。** 若宿主无法正确透传，不能以放开 CORS、关闭 CSRF 或证书验证绕过；需将前端与反向代理部署在同一个 Web Service，该部署拓扑调整应单独确认。本次没有对尚未应用的公网配置声称验收通过。

本地回归：

```powershell
cd frontend
npm run test:api
npm run test:writing
npm run test:oj
npm run build
cd ../backend
# 在 Java 21 环境中运行；默认不执行 Aiven 云集成测试。
mvn test
```

容器检查：`docker build -t acm-platform-api ./backend`。在部署秘密已配置后才运行容器；不要把真实密码写在 docker 命令参数中。此项目单实例内存 Session，后端重启需重新登录；Render 冷启动可能超时，页面保留输入并允许手动重试，不自动重复登录或投稿请求。

Secret File、变量及网络监听说明来自 [Render environment variables](https://render.com/docs/configure-environment-variables) 和 [Web services](https://render.com/docs/web-services)。

## 本轮实际验证（2026-10-07）

- `test:api` 11 项、`test:writing` 10 项、`test:oj` 12 项通过；vue-tsc/Vite build 通过。保留已有 OJ 编辑器大于 500KB 的构建提示。
- `mvn test`：16 项通过，6 项需要显式启用的云集成测试跳过；共 22 项、0 失败。新增配置测试检查 aiven/render 顺序、绑定地址、PORT、Cookie 和 VERIFY_IDENTITY，发现并修正了初版 profile group 的覆盖顺序问题。
- `mvn package -DskipTests` 构建通过。没有新增依赖或 Flyway 迁移。
- 容器入口在 Windows Git sh 中完成 6 项检查，包括用实际 CA 与 Java 21 keytool 导入并读取 PKCS12、缺失／无效 CA 终止、已有信任库和本地 profile。**未执行完整 Docker image build**，本机没有 Docker CLI。
- 实际浏览器检查本地服务失败：登录表单结束加载、展示连接错误、输入保留。截图 `.qa/render-login-error.jpg` 为本地异常反馈验收，不能作为公网登录成功证据。
- 公网检查脚本实际运行：后端健康 JSON 成功；数据库／登录未启用，前端代理／匿名身份／CSRF／Cookie 检查失败；因此没有发送匿名 POST 或账号密码。
- 本地真实登录联调已尝试，后端因现有 Aiven Host 的 UnknownHostException 退出，未完成认证。**本轮未通过 Aiven 实际连接或公网真实登录验收。**
- 上述首次验收时未提交、推送或部署；未执行数据库迁移、播种、密码变更或内容修改。用户随后已授权提交、推送和 Render 部署。应用配置仍需要当前 Aiven Host 核对和可用的 Render 控制台登录；授权不等于已经成功部署。
