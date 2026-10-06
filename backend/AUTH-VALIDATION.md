# 账户与投稿审核：实施与验证

日期：2026-10-06，Asia/Shanghai。测试使用明确的 Aiven development 环境与专用账号、文章。此记录不包含任何密码、哈希、会话令牌、数据库地址或完整连接字符串。

前半部分保留初次认证与投稿交付的历史记录；当前账号、写作与审核优化的实施、文件与验收结果见本文末尾“账号、写作与审核体验优化”。

## 实施范围与权限

1. 检查 Vue 多页面、既有 JDBC/Flyway 数据模型及 Aiven TLS 配置。验收：复用既有 users/articles/categories/tags，不增加后台技术栈。
2. 新增 V3 增量迁移与 Session 认证。验收：只有 USER/ADMIN，历史作者与 DEMO 身份不能登录；原有文章与作者关联保留。
3. 实现事务化草稿与审核 API。验收：服务端身份、作者权限、PENDING 锁定、revision 冲突、审核轮次唯一约束与公开 PUBLISHED 查询。
4. 接入账号页面、共享导航、编辑预览与管理控制台。验收：真实数据库分页与统计、异常反馈、草稿保留、手机键盘操作；OJ 演示继续独立。
5. 实际迁移、集成测试、重启持久化与浏览器验证。验收：以下记录仅记载实际执行的结果。

| 功能 | 访客 | USER | ADMIN |
|---|---|---|---|
| 公开文章、题库 | 可以 | 可以 | 可以 |
| 草稿与审核信息 | 不可以 | 仅自己 | 全部 |
| 投稿与撤回 | 不可以 | 自己的草稿／未处理投稿 | 有权限的文章 |
| 审核、直接发布、下架、推荐设置 | 不可以 | 不可以 | 可以 |
| 真实用户资料、分类和标签管理 | 不可以 | 不可以 | 可以 |

## 数据库与检查结果

- 迁移前只读检查了实际环境、MySQL 8.4.8、现有用户角色与文章状态、约束和连接限制。私有配置明确为 development。
- V3 已在现有 Aiven 数据库执行；再次 migrate 为 0 项。V1/V2 原文件未修改，13 张业务表加 Flyway 历史表。没有重建、清空或删除原有业务数据。
- 历史 AUTHOR 迁为不可登录的 USER；两种角色约束生效。原有 8 篇已发布文章及 OJ 演示记录保留。
- 首个 site_admin 通过一次性命令创建，随机密码仅保存在被 Git 忽略的私有初始化文件。再次执行输出已存在且保留密码；重启后仍能使用同一密码登录。
- 新管理员中文显示名的 properties 编码问题在联调中修复；文档中的中文 properties 使用 Unicode 转义，环境变量可以直接填写中文。
- 最终显式启用的 Maven 测试：17 项全部通过，0 失败、0 错误、0 跳过。包含实际 Aiven 注册、Session ID 轮换、CSRF、越权与字段注入、投稿／撤回／驳回／重投／批准、直接发布／下架、分类标签创建／重命名／重复名冲突、公开查询及真实统计隔离。
- 并发审核：两个线程同时批准同一专用投稿，结果为 200 与 409，只有一条有效审核记录。专用并发数据按精确 ID 清理；事务集成测试的数据整体回滚。
- 原有 DB_ACTION=verify 的 20 项检查在 V3 后全部通过：中文、emoji、代码换行、多标签、多样例、提交快照、正式／演示去重、查询分页、唯一／外键／CHECK 与历史删除限制。验证事务回滚。
- 最终回归发现 OJ 演示提交列表的 GET 被正式提交权限规则覆盖；已将认证要求限制为正式提交 POST。新增访客 GET 为 200、访客正式提交为 401 的断言后，17 项测试再次全部通过；最终运行包也已重新构建并启动。
- 后端实际停止并重启后，专用投稿的中文代码、PUBLISHED 状态与两条审核记录仍可从 API 查询；登录／获取账号／退出再次成功。
- 云连接在个别测试启动时出现超时；保留 VERIFY_IDENTITY 与 CA 校验。复测使用 60 秒借连接超时通过；日常配置仍为 30 秒。连接池保持 2 条，缩短连接寿命并配置心跳，未增加连接预算或关闭安全校验。
- Vue/TypeScript 与 Vite 构建通过；原有 12 项 OJ 检查全部通过。现有编辑器包约 537KB，保留原有体积提示。

## 浏览器验证

- 普通用户实际登录，返回站内来源文章；注销后私人页面提示登录。USER 直接访问控制台显示无管理员权限，导航无管理入口。
- 新建专用文章，编辑中文正文与原始代码，预览、保存到 Aiven；320px 刷新后本地草稿、原始代码与换行恢复。
- 投稿确认框默认聚焦取消，Tab 循环、Escape 取消、焦点恢复、背景 inert 均实际验证。
- 手机投稿、PENDING 正文锁定、撤回和再次投稿可用；复制代码有成功反馈，剪贴板文本与专用代码一致。
- 管理员登录、真实统计、待审筛选、审核预览、驳回必填原因与实际驳回可用；作者重新登录可读到原因，修改、保存并重新投稿。
- 重新投稿后，管理员在浏览器批准；公开详情能读取更新后的正文、代码与实际作者，目录继续自动生成。
- 暂停本地后端后，保存及时失败且保留标题、摘要和正文；重启后的过期 Session 被拒绝，编辑器保留输入并提示重新登录。重新登录回到原页面，恢复本地草稿，再保存成功。
- 管理员在浏览器新建、保存、直接发布与推荐标记保存成功；新推荐文章在公开列表可见，不会因存在主推荐而丢失。推荐／横向分组采用明确优先级，避免同一文章重复统计。
- 最终文章管理数据库分页显示 11 篇、两页，第二页范围 11–11；无匹配搜索显示 0–0 和无结果提示。手机真实用户列表为 2 个账户，不显示历史作者或演示身份。
- OJ 手机代码标签与固定 CE 运行场景实际可用，CSRF 写请求成功，错误信息明确说明未编译用户代码；运行前后演示提交均为 1 条。真实身份与演示身份分别标注，正式提交按钮仍为未接入。
- 三篇浏览器专用测试文章均已下架，内容和审核记录保留；最终 HTTP 公开查询为原有 8 篇，专用文章不会出现在公开搜索和推荐中。专用普通测试账户保留，便于复查。
- 桌面 1280px、手机 390px 与 320px 代表账户页面已检查整页宽度；修复预览代码撑宽问题后，320px 页面宽度为 305px（含滚动条差值），代码仅内部横向滚动。
- 窄屏导航显示账号名称、资料与退出；管理员菜单 Escape 后恢复触发按钮焦点。

截图保存在被 Git 忽略的 `.qa/`：最终控制台 `auth-admin-desktop-final.jpg`、最终真实用户列表 `auth-users-mobile-390.jpg`，以及流程验收时的 `auth-admin-desktop.jpg`、`auth-user-mobile-320.jpg`、`auth-public-mobile-390.jpg`。截图反映验收时刻的实际专用数据，统计不是固定示例。

## 交付文件

后端新增：

- `src/main/java/com/acm/platform/auth/`：Account、AuthService、AuthController、SecurityConfig、LoginThrottle。
- `src/main/java/com/acm/platform/content/`：ArticleInput、ArticleService、AccountController。
- `src/main/resources/db/migration/V3__accounts_and_article_review.sql`。
- `src/test/java/com/acm/platform/auth/`：AuthRulesTest、AuthCloudTest、AuthConcurrencyCloudTest。
- `AUTH.md`、`AUTH-VALIDATION.md`、`admin.env.example`。

后端修改：`pom.xml`、`scripts/Database.ps1`、`application.properties`、`database.properties`、`DatabaseConfig`、`DatabaseLifecycle`、`DevelopmentSeed`、`PlatformRepository`、`DemoService`、`DataController`、`CapabilitiesController`、`ApiErrors`、`DATABASE.md`、`DATABASE-VALIDATION.md`。

前端新增：`account.html`、`src/account.ts`、`src/account/`（认证状态、登录注册、文章列表、编辑器、控制台、确认框与 DTO）、`src/styles/account.css`。

前端修改：`vite.config.ts`、`src/api/client.ts`、`src/api/database.ts`、`Navbar.vue`、`data/articles.ts`、`ArticleView.vue`、`CodeSharingView.vue`、`OjApp.vue`、`ProblemList.vue`、`ProblemDetail.vue`、`SubmissionList.vue`。仓库 `README.md` 同步入口说明。

所有私有配置、临时联调文件和截图位于 Git 忽略目录；没有提交、推送或公网部署。

## 保留边界

真实判题、邮箱／短信验证、找回密码、资料修改、共享 Session 与分布式登录限流未接入。当前 Session 为单后端实例，重启需要重新登录。管理员不能通过公开接口更改用户角色或密码。OJ 的身份、记录、AC 与统计仍明确为 DEMO；真实提交服务未开放。

使用与初始化命令见 `AUTH.md`，数据库/TLS 配置见 `DATABASE.md`。

## 账号、写作与审核体验优化（2026-10-06）

### 实施步骤与验收

| 阶段 | 实施内容 | 验收标准 |
|---|---|---|
| 查询与操作可靠性 | 摘要列表、数据库排序分页、管理员公开更新确认 | 不返回正文／全部审核历史，查询次数实测；公开更新确认后生效 |
| 草稿与编辑器 | 单请求防抖保存、草稿创建幂等、revision 冲突、章节／标签／语言 | 连续输入与旧响应不丢稿；冲突双方保留，明确确认处理 |
| 账号与审核 | 本人名称与密码、所有旧会话失效、待审入口和来源列表 | USER 不能越权；数据与统计来自数据库，返回保留条件 |
| 界面与回归 | 保存反馈、手机操作、焦点、轻量样式、原有文章与 OJ | 桌面／320px／390px，无整页横向溢出；既有主要流程保留 |

权限仍为 USER/ADMIN：访客仅公开浏览；USER 修改本人资料与密码、自己的 DRAFT/REJECTED，不能编辑 PENDING/PUBLISHED；ADMIN 审核和公开文章管理。两种角色都不能通过客户端修改角色、用户名、作者归属或他人密码。

### 本次数据库变更

- 实际只读检查 Aiven development、MySQL 8.4.8、既有结构及连接限制后，执行 V4。V1/V2/V3 未改。
- users.credential_version：BIGINT NOT NULL DEFAULT 0，非负约束。修改密码时与哈希一起在事务中更新；Session 请求比对版本，拒绝全部旧凭据版本。
- articles.draft_key：可空 ASCII UUID；UNIQUE(author_id,draft_key)。旧文章为 NULL，不改正文和作者。新草稿重试／并发创建不会生成重复文章。
- V4 实际执行 1 项，重复执行为 0 项。没有新增业务表、清空数据、升级用户角色或增大连接池。TLS VERIFY_IDENTITY 和 CA 校验保持，日常池大小仍为 2。

### 实测查询次数

`ArticleListQueryCloudTest` 包装 JDBC PreparedStatement 的实际执行计数，读取同一开发数据库的管理员列表：优化前每页 10 篇为 **22 次**（COUNT、文章、每篇标签、每篇审核），优化后每页 10 篇为 **2 次**（COUNT、摘要 SELECT），每页 1 篇也为 2 次。REJECTED 的当前轮次原因通过同一 SELECT 关联，不增加逐篇查询。

计数只涵盖文章列表服务，不含认证账号读取、单独的管理统计请求和详情查询。没有测量吞吐、延迟或性能百分比。原始只读计数日志为 `target/list-baseline.log`、`target/experience-query-final.log`，日志位于 Git 忽略目录。

### 自动检查

- 后端共 21 项测试已执行，最终各测试类结果均通过：AccountExperienceCloudTest 2、AuthCloudTest 1、AuthConcurrencyCloudTest 2、AuthRulesTest 5、ArticleListQueryCloudTest 1、DatabaseRulesTest 10。
- 最后一次全套运行中，20 项通过，查询计数测试启动因 Aiven 连接借用超时失败；单独以 `-Dacm.db.pool-timeout-ms=60000` 复查通过。没有关闭证书校验或改变日常 30 秒借连接超时。完整日志与复查日志分别为 `target/experience-final-tests.log`、`target/experience-query-final.log`。
- 云测试验证本人姓名校验、禁止角色／身份字段注入、CSRF、USER 管理接口 403、错误旧密码 400 且当前 Session 保持有效、新密码确认、当前与第二个旧 Session 失效、旧密码拒绝及新密码登录。响应不包含密码、哈希或 credentialVersion。使用随机专用账号并事务回滚，没有改管理员或既有测试用户密码。
- 云测试验证 UUID 草稿幂等、两个线程并发首次创建同一键只产生一篇文章、revision 409、驳回原因摘要、投稿时间正／倒序及数据库分页、Python 元数据和中文／原始代码换行、管理员更新公开正文及作者显示名称联动。
- 并发审核保持 200＋409、只有一条审核记录。只有本轮新建的 UUID 并发测试数据按精确 ID 清理；既有测试账号与文章保留。
- V4 后实际执行原有数据库 verify，20 项通过，覆盖中文、emoji、代码、多标签、多样例、快照、正式／演示统计隔离、分页、唯一／外键／CHECK 与历史删除限制；测试事务回滚。
- `npm run test:writing`：10 项通过，覆盖防抖、单请求协调、新输入不被旧响应覆盖、手动补存最新内容、无效内容不请求、失败暂停重试、公开／锁定状态无自动保存、卸载保护、语言回退和 MySQL JSON 属性顺序回归。
- `npm run test:oj`：12 项通过；`npm run build`（vue-tsc＋Vite）通过。原有 OJ 编辑器约 537KB 的构建体积提示保留，没有扩展判题功能。

复查命令（backend 使用 Java 21，私有配置沿用已有文件）：

```powershell
$env:ENABLE_CLOUD_AUTH_TESTS='true'
mvn '-Dacm.db.pool-timeout-ms=60000' test
Remove-Item Env:ENABLE_CLOUD_AUTH_TESTS
./scripts/Database.ps1 -Profile aiven -Action verify
cd ../frontend
npm run test:writing
npm run test:oj
npm run build
```

### 浏览器实际流程

- 普通用户新文章首次有效输入后自动保存取得唯一编号，手动保存后刷新恢复相同文章、正文和代码。连续输入过程旧响应没有覆盖后续输入。
- 发现 MySQL JSON 属性顺序导致内容签名不稳定的问题，改为固定字段的规范签名；专用文章实际恢复稳定“已保存”，不再持续重复自动保存。
- 两个标签页分别编辑同一文章：第一次保存成功，另一页出现 409，服务器内容与本地输入同时保留。服务器预览、复制本地全文成功，刷新后冲突仍在；Escape 取消恢复保持本地，确认恢复后才替换本地。复制文本保留代码和换行。
- 暂停实际本地后端，自动保存失败并暂停，不反复请求；输入保留，列表有重试。后端重启后的旧 Session 被拒绝；同一用户重新登录回来源并恢复本机内容，手动保存成功。专用文章数据库内容重启后仍可读。
- 新摘要输入后直接投稿：先保存最新内容再提交，待审正文锁定。管理员点击真实待审数量进入 PENDING＋投稿时间正序，审核后返回保留搜索、筛选、页码、排序，待审数量更新为 0。
- 管理员编辑专用 PUBLISHED 文章不自动保存。点击明确的“保存并更新公开文章”打开确认；取消后公开摘要未变化，确认后公开页立即显示新摘要，反馈与实际状态一致。
- 章节上移／下移、搜索与选择标签、已选数量、Python 预览与公开高亮可用。公开目录正常，复制成功后的剪贴板内容与含中文／转义字符／换行的原始代码完全一致。
- 普通用户修改本人显示名称，资料、导航、另一个已打开页面和作者名刷新为相同名称；最后恢复专用用户原名称。用户名与角色没有变化。
- USER 直接访问管理员页面显示无权限；后端测试同时覆盖 403 和字段注入拒绝。
- 管理员数据库分页为 12 篇、两页；第二页显示 11–12，文章详情返回保留第二页。补充同页入口与浏览器 URL 条件同步，避免地址清空后继续显示上一页结果。
- 本轮新建的 `写作优化验收 20261006` 专用文章最后已下架，保留正文和批准／下架记录；原有三篇测试文章也继续保留。公开数量恢复原有 8 篇。
- 原有历史文章预览与 C++ 高亮保持；OJ 固定 CE 运行实际显示“未编译用户代码”且不计入提交记录，正式提交仍禁用未接入，真实身份与演示身份分开。
- 实测桌面 1280px、手机 320px／390px 代表列表、编辑器和资料页。手机文档宽度分别为 305px／375px（滚动条差值），等于可用宽度，没有整页横向溢出。确认框默认取消、Escape、键盘焦点恢复与链接 Enter 已验证；界面保持现有配色与卡片。

截图位于 Git 忽略的 `.qa/`：`writing-admin-desktop-final.png`、`writing-admin-320.png`、`writing-admin-390.png`、`writing-conflict-desktop.png`、`writing-editor-320.png`、`writing-profile-390.png`。截图为验收时实际内容，数字来自数据库。

### 本轮文件清单

后端新增：

- `src/main/java/com/acm/platform/auth/ProfileController.java`、`SessionValidityFilter.java`。
- `src/main/resources/db/migration/V4__draft_identity_and_credentials.sql`。
- `src/test/java/com/acm/platform/auth/AccountExperienceCloudTest.java`、`src/test/java/com/acm/platform/content/ArticleListQueryCloudTest.java`。

后端修改：`auth/Account.java`、`AuthService.java`、`SecurityConfig.java`；`content/ArticleInput.java`、`ArticleService.java`、`AccountController.java`；`auth/AuthConcurrencyCloudTest.java`；`AUTH.md`、`AUTH-VALIDATION.md`。

前端新增：`src/account/draftSaver.ts`、`contentSignature.ts`、`ProfilePage.vue`；`src/components/codeLanguages.ts`；`scripts/test-writing.mjs`。

前端修改：`src/account.ts`；`src/account/ArticleEditor.vue`、`ArticleList.vue`、`AccountApp.vue`、`auth.ts`、`confirmation.ts`、`types.ts`；`src/styles/account.css`；`src/components/ArticleCodeBlock.vue`；`src/data/articles.ts`；`src/views/ArticleView.vue`；`package.json`。没有新增依赖、前端框架或判题语言支持。

### 已知限制与未执行项

- 密码变更与全部旧会话失效实际通过后端 MockMvc＋Aiven 专用事务账号验证；没有在浏览器表单输入并提交新密码，没有更改既有用户密码。个人资料／密码表单布局实际检查。
- prefers-reduced-motion 沿用已有规则，新增轻量交互样式不增加持续动画；没有切换操作系统减少动态效果进行实测。
- 后端 Session 与尝试限流仍为单实例；没有 Redis 或多实例共享会话。密码版本持久化保证旧凭据会话后续访问被拒绝，但后端重启本身仍需要重新登录。
- 浏览器存储被禁用或容量耗尽会显示本地备份失败；本机备份包含文章内容，不是服务器永久备份。复制标签页使用独立写入快照的隔离实现已构建检查，未单独实测浏览器菜单“复制标签页”。
- 没有新增文章版本历史、批量批准、邮箱／短信／找回密码、管理员提权或真实判题。没有测量性能改善比例。
- 没有提交、推送 GitHub 或部署公网网站。当前本地前后端运行，并使用既有 Aiven development 数据库。
- 会话切换后发现本地开发服务停止，已用隐藏的本地 PowerShell 帮助进程重新启动；最后 HTTP 检查 Vue 首页和 Spring Boot 文章接口均为 200。后端重启后需要重新登录。最后尝试绑定浏览器错误页时被浏览器工具的协议安全策略阻止，因此没有声称完成这次重启后的 UI 复查；前述桌面／手机截图和流程验收是在重启前实际完成。
