# 账户、投稿与管理员控制台

Vue 3 多页面 + Spring Boot 3.5 / Java 21 + Spring JDBC + Aiven MySQL。没有新增第三种角色，没有接入真实判题。认证依赖版本由 Spring Boot 管理。

## 页面与权限

- 登录：`http://127.0.0.1:5173/account.html#/login`；注册：`account.html#/register`。
- 我的文章：`account.html#/articles`；个人资料：`account.html#/profile`。
- 管理员控制台：`account.html#/admin`；用户：`#/admin/users`；分类标签：`#/admin/taxonomy`。
- 公共导航的账号菜单提供入口。未登录显示登录/注册；只有 ADMIN 显示控制台。

| 权限 | 访客 | USER | ADMIN |
|---|---|---|---|
| 公开文章与题目 | 是 | 是 | 是 |
| 私有投稿 | 否 | 仅自己的 | 全部 |
| 保存草稿、提交、撤回 | 否 | 仅自己的 | 是 |
| 修改显示名称、本人密码 | 否 | 仅自己 | 仅自己 |
| 审核、直接发布、下架、推荐 | 否 | 否 | 是 |
| 真实用户列表、分类标签管理 | 否 | 否 | 是 |

后端强制校验。匿名受保护接口返回 401，用户访问管理接口或他人草稿返回 403。注册 DTO 没有 role；未知字段（role、authorId、status 等）返回 400。会话身份由服务器提供，不接受客户端作者或角色。用户列表查询明确选择基本资料字段，不查询密码、哈希或令牌。

## 增量迁移与首个管理员

V1/V2/V3 已执行文件不再修改。V3 添加可为空的唯一 username（历史作者与演示用户保持 NULL），将历史 AUTHOR 转为 USER，添加文章 PENDING/REJECTED、提交时间、审核轮次、版本号和审核记录表。V4 仅新增 users.credential_version 与 articles.draft_key，并添加凭据版本非负约束和作者＋草稿键唯一约束。原有文章、JSON 正文和作者外键保留。没有清空表、默认账号密码或启动自动播种。

在项目 `backend` 目录使用 Java 21：

```powershell
$env:JAVA_HOME=(Resolve-Path ../.tools/jdk-21.0.12.1).Path
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn package
./scripts/Database.ps1 -Profile aiven -Action check
./scripts/Database.ps1 -Profile aiven -Action migrate
```

已有 Aiven 私有配置与 CA 信任库继续使用，`VERIFY_IDENTITY` 不变。迁移由此独立命令执行，正常启动只校验版本。切换本地使用 `-Profile mysql-local` 及对应私有配置；数据库连接变量、TLS 配置见 `DATABASE.md`。

首个管理员只通过一次性命令建立。在被 Git 忽略的 `backend/secrets/admin-init.properties` 本地填写：

```properties
acm.admin.username=<YOUR_USERNAME>
acm.admin.display-name=\u7ad9\u70b9\u7ba1\u7406\u5458
acm.admin.password=<LOCAL_ONLY_PASSWORD>
```

```powershell
./scripts/Database.ps1 -Profile aiven -Action bootstrap-admin
./scripts/Database.ps1 -Profile aiven -Action serve
```

也可仅为初始化命令设置 ADMIN_USERNAME / ADMIN_DISPLAY_NAME / ADMIN_PASSWORD 环境变量。初始化不重复创建、不覆盖密码、不升级普通账号；已有首个管理员时拒绝创建额外管理员。命令不会输出密码。完成后可将初始化私有文件移至自己的安全密码存储，正常 serve 不读取它。

本次已生成首个管理员 `site_admin` 的随机独立密码并写在该私有文件中；请在本地读取。不要将文件、截图、内容或密码提交 Git 或发送聊天。不存在固定默认密码。
Java `.properties` 的中文使用上例 Unicode 转义；使用 ADMIN_DISPLAY_NAME 环境变量时可以直接填写中文。

前端启动：

```powershell
cd ../frontend
npm run dev
```

## Session 与 CSRF

密码 BCrypt cost 12；用户名 3–32 位 ASCII 字母/数字/下划线，归一化为小写；密码至少 12 字符，UTF-8 不超过 BCrypt 的 72 字节上限。历史无密码身份不能登录。

登录成功轮换 Session ID，服务端显式保存 SecurityContext；会话 30 分钟过期，Cookie HttpOnly、SameSite=Lax、只用 Cookie 跟踪；退出使会话失效。开发环回 HTTP 使用 Secure=false；生产 HTTPS **设置 SESSION_COOKIE_SECURE=true**。前后端部署为同源 `/api`，不开放跨域凭据访问。当前内存 Session 适用于单实例，后端重启需重新登录。多实例共享会话未在本次扩展。

`GET /api/auth/csrf` 返回当前 Session 的掩码令牌；Vue axios 的写请求拦截器发送 X-CSRF-TOKEN。登录/退出后清除客户端令牌缓存；令牌过期请求失败时保留输入，下一次重试重新获取。令牌不存入 localStorage。全局写请求（含 OJ 演示）保留 CSRF 防护。连续失败按用户名和实际客户端 IP 限制，账号错误返回同一提示；当前限流为单实例内存，不信任客户端 X-Forwarded-For。

站内登录来源使用同源页面白名单。拒绝跨域、反斜杠或非法路径，避免开放重定向。

个人资料页可以修改本人显示名称。`PUT /api/account/profile` 只接受 displayName，身份来自服务器；用户名、角色和作者归属不能修改。每次认证请求读取最新账号，跨页面使用不含身份和凭据的随机通知信号刷新导航与资料。

`POST /api/account/password` 接受 currentPassword、newPassword、confirmPassword，验证旧密码、两次新密码和既有密码规则，沿用 BCrypt。错误旧密码返回 400，不清除有效会话；连续失败沿用单实例限流。成功后在事务中更新密码哈希并递增 credential_version，当前 Session 注销；所有旧 Session 的后续请求都会被服务端拒绝，包括并发旧密码登录得到的旧版本会话。没有新增 Redis、管理员改他人密码或找回密码入口。密码和凭据版本不进入账号响应；密码不存到浏览器持久存储。

## 文章流程与结构

`DRAFT/REJECTED → PENDING → PUBLISHED/REJECTED`，未处理的 `PENDING → DRAFT` 可撤回；ADMIN 可 `DRAFT/REJECTED/ARCHIVED → PUBLISHED` 或 `PUBLISHED → ARCHIVED`。

正文仍为 ArticleSection[] JSON，含 heading/level/paragraphs/bullets/code，可选 codeLanguage。编辑器支持 H2/H3/H4、章节上移／下移、段落、列表与原始代码块；标签可以搜索、查看已选项，最多 8 个。预览和公开页复用同一代码块组件。历史缺少语言字段的代码仍按 C++ 展示，新代码块默认纯文本；支持 C++、Python、Java、JavaScript、SQL 展示，非 C++ 语法按需加载，不支持的语言回退纯文本。语言仅影响文章展示，不改变 OJ 执行能力。正文作为纯文本显示，后端拒绝标题、正文和标签中的 HTML 注入，代码原文保留且通过现有高亮组件转义显示。不接受富文本 HTML。

普通用户的已发布/下架文章只读。待审文章所有人不能直接改正文，管理员在预览里批准或填写原因驳回。保存和状态操作均检查 revision，事务锁定文章行；并发审核只有一次有效转换。审核轮次唯一约束防止重复审核记录；撤回再提交开启新轮次。标签关联、正文保存与审核状态变更使用事务，失败整体回滚。

公开列表、搜索、元数据标签、详情及相关推荐只读 PUBLISHED。下架不删除内容或审核记录。标签重命名保留文章/OJ 关联，不提供删除在用标签。文章列表、用户列表在数据库筛选分页；模糊搜索采用参数化 LIKE 并转义 `%/_`。

我的文章与管理列表只返回列表摘要，不返回正文、全部审核记录或标签关联；REJECTED 列表只关联当前轮次的驳回原因。一次列表查询为 COUNT＋分页 SELECT，详情接口继续按权限读取完整正文、标签与审核记录。搜索、状态、分页和更新时间／投稿时间排序由数据库完成；列表条件保存于账号页面 URL，详情返回保留条件。控制台待审数量来自数据库，点击应用 PENDING 与投稿时间正序。

有权限的 DRAFT/REJECTED 在输入停止约 1.2 秒后自动保存；无效内容只保留本机并说明校验要求。首次有效创建使用 UUID draftKey，作者＋草稿键唯一约束保证重试或并发创建只产生一篇文章。每个编辑器只发出一个保存请求，响应只确认快照与 revision，不覆盖请求期间的新输入；手动保存等待并补存最新内容，提交审核先保存最新内容。PENDING、PUBLISHED 不自动保存；管理员修改已发布文章必须点击“保存并更新公开文章”并确认，成功后立即影响公开页面。

本地备份按用户、文章与标签页隔离，采用独立快照写入，避免浏览器复制标签页时互相覆盖备份。刷新恢复后比较服务端 revision；冲突暂停自动保存，提供服务器版本预览、复制本地全文、确认恢复及确认覆盖。不会静默换成最新 revision 后覆盖。网络失败或会话过期暂停自动保存并保留输入，需手动重试；不会无限重试或自动投稿。保存状态与最近成功时间独立展示。

本地备份依赖浏览器存储可用性，写入失败有提示；共享电脑请注意本地草稿包含正文，其中不存密码或 Session。离开已保存文章会清理本标签页备份；未保存备份保留用于恢复，其他标签页的快照不删除。

## 验证命令

```powershell
mvn test
$env:ENABLE_CLOUD_AUTH_TESTS='true'
mvn test
Remove-Item Env:ENABLE_CLOUD_AUTH_TESTS
cd ../frontend
npm run build
npm run test:oj
npm run test:writing
```

云集成测试仅显式启用，并断言实际 acm.db.environment=development，读取被忽略的本地数据库配置。MockMvc 禁止打印请求内容。AuthCloudTest 的专用账号与文章事务回滚；并发测试只提交 UUID 专用数据，finally 删除本次精确 ID 的验证记录，不修改业务数据。没有接入 Aiven 配置时云测试跳过，不应声称通过。

## 范围与限制

邮件验证、短信、找回密码、修改用户名、管理员修改他人密码、复杂权限配置、完整富文本编辑器、共享 Session 和分布式登录限流未开放。只开放修改本人显示名称与本人密码。OJ 仅显示真实登录身份；DEMO 的身份、记录和统计保持独立，正式提交仍返回判题服务未接入，不创建真实 AC。所有管理员密码与数据库凭据仅在后端私有环境；禁止 VITE_* 凭据。
