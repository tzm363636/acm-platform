# 账户与投稿审核：实施与验证

最新记录：2026-10-07 的登录连续性与部署复查见本文末尾。以下旧记录保留。

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

## 登录连续性与用户体验（2026-10-07）

### 范围、步骤和完成标准

1. 复查认证、部署文档及页面入口，保持同源 `/api`、Session、CSRF、USER/ADMIN。验收：不增加认证体系或数据库结构，分别检查本地与公网。
2. 统一身份状态、请求协调与跨标签页通知。验收：公开页面先显示内容，私人页面等待可信身份；迟到请求不能覆盖新账号；服务失败与未登录分开。
3. 完善导航、密码字段、资料入口和草稿离开保护。验收：退出以服务器结果为准，输入保留、账号隔离，保存校验失败不离开，确认框和移动菜单支持键盘。
4. 执行单元、真实 Aiven development、浏览器与响应式验证。验收：以下仅列实际执行内容，公网配置未应用不能算修复成功。

权限矩阵沿用本文原表：访客只看公开内容；USER 只操作本人资料及稿件；ADMIN 才能管理与审核。后端仍强制校验，既有审核、revision、改密凭据版本和 OJ DEMO 隔离未改。

### 实际改动

- `account/auth.ts`：五种身份状态、单一在途身份请求、身份变化时旧响应失效、焦点与历史缓存复核、同源标签页失效信号、服务失败保留最后身份提示。
- `account/identityEpoch.ts` 与 `api/client.ts`：身份代次保护；旧 401 不影响新登录，等待 CSRF 时切换账号取消私人写请求；CSRF 失败可手动重试且不自动重放。登录错误的 401 与会话失效的 401 区分。
- `Navbar.vue`、`AccountApp.vue`：稳定身份占位、统一菜单、退出不强制跳页、私人页面等待认证；跨标签页确认中隐藏私人内容而保留编辑器实例，不提前显示无权限。
- `ArticleEditor.vue`、`leaveGuard.ts`、确认框：保存／本地保留／取消，离开请求合并；失败保留输入，身份变化暂停保存，不同账号隐藏原稿，同账号恢复后手动继续保存。
- `PasswordField.vue`、`AuthPage.vue`、`ProfilePage.vue`：密码显示／隐藏、Caps Lock 提示、autocomplete、提交防重；已登录的登录页提供继续入口；注册成功但登录失败时不重复注册；最近文章与审核状态读取已有摘要接口。
- `router/index.ts`：修复直接打开 `/index.html` 时首页空白，保留 `/` 和多页面架构。
- `oj/OjApp.vue`：仅修正真实身份确认中／服务异常的提示，不提前显示未登录；演示流程、身份、草稿与记录仍独立。
- `scripts/Check-Render.ps1`：增加实际 Cookie `Path=/` 检查。

### 已执行的检查

| 检查 | 实际结果 |
|---|---|
| `npm run test:session` | 23 项通过；迟到响应／401、并发身份查询、焦点去重、跨标签页、服务失败、退出失败、账号切换取消写请求、安全返回、离开确认及注册后登录失败的重试 |
| `npm run test:api` | 11 项通过；CSRF、HTML 代理错误、认证消息、超时／网络／服务失败区分 |
| `npm run test:writing` | 10 项通过；防抖、旧响应、新输入、单一写入者、失败保留、正文语言回退 |
| `npm run test:oj` | 12 项通过；演示隔离、快照、草稿、异常和分页 |
| `npm run build` | vue-tsc 与 Vite 通过；保留已有 OJ 编辑器 500KB 以上分块提示 |
| 默认 `mvn test` | 共 22 项，16 项通过，6 项显式云测试跳过；0 失败、0 错误 |
| 显式云测试 | `AuthCloudTest` 1 项＋`AccountExperienceCloudTest` 2 项，真实 Aiven development 全部通过、0 跳过；专用事务数据回滚，涵盖注册、会话轮换、CSRF、越权、投稿审核、资料和改密旧会话失效 |
| 本地 HTTP 验收 | 19 项通过；Vue `/api`→Spring Boot→Aiven，USER/ADMIN、公开文章、401/403、CSRF、会话轮换、身份查询与退出；未改密码或业务内容 |
| 实际本地 Cookie | 从前端 proxy 响应确认 `Path=/`、HttpOnly、SameSite=Lax；开发 HTTP 的 Secure=false。公网 Secure/透传未通过验收 |
| 公网只读复查 | curl：后端 capabilities HTTP 200，database/login=false；前端 CSRF HTTP 404。PowerShell 另出现过一次没有 HTTP 响应的本机网络失败，未当作服务器状态 |

### 浏览器实际流程

- 专用 USER 和既有 ADMIN 分别打开首页、代码分享、文章、作者、网站介绍、成长足迹、OJ、个人资料；刷新和同源新标签页身份保持。ADMIN 控制台可读，USER 直接打开显示没有管理员权限。
- 登录返回 OJ 带 query/hash 的来源，另一轮返回代码分享的标签筛选；已登录访问登录页显示当前身份与继续入口，隐藏重复登录表单。
- 另一个标签页退出，公开页面留在原地址和标签筛选，原标签页草稿保留并禁用保存；换成另一个账号后原编辑器隐藏。重新登录原账号，文本恢复并可手动保存。
- 资料页退出后隐藏旧账号的私人内容，保留组件内输入并显示带原来源的重新登录入口；最后浏览器复查确认资料标题不可见。
- 专用测试账号改名在另一标签页同步，稿件输入保留；完成后已恢复原显示名称。未修改既有账号密码。
- 新建专用私有 DRAFT「登录连续性验收 20261007」；取消离开、无效摘要保存后离开被阻止、保留本地后离开、重新打开恢复，均实际验证。原有已发布文章未修改；该测试草稿保留，未删除或公开发布。
- 停止本轮自己的本地后端验证保存连接失败：结束加载、显示重试、保留输入并暂停自动保存。恢复同配置后端后，旧内存会话失效，重试得到会话过期；同账号重新登录后手动重试成功，最新摘要与正文保存。没有停止 Aiven 或关闭 TLS 校验。
- 开发联调中一次数据库连接暂不可用返回 503，输入保留；恢复本地连接后重试成功。重启后旧 CSRF 登录失败，手动重试重新获取令牌后成功。没有把服务故障显示为密码错误。
- 320px、390px 的登录、资料、稿件页面实际测量整页宽度不大于 viewport；默认桌面 1280px 验证。移动账号菜单 Escape 关闭并恢复焦点，USER 菜单没有管理员入口；密码显示按钮用无敏感测试文本验证。
- 公开文章目录定位 hash 与代码复制成功；OJ 固定演示运行及手机题目／代码／结果切换回归，原代码、测试输入保持。运行只显示固定场景，没有创建正式 AC。

截图保存于被 Git 忽略的 `.qa`：`session-leave-desktop.png`、`session-draft-network-error.png`、`session-profile-desktop.png`、`session-profile-320.png`、`session-profile-390.png`、`session-login-320.png`、`session-editor-390.png`、`session-private-logout.png`。这些是本地前后端连接 Aiven 的证据，不是公网部署证据。

### 文件清单

新增：`frontend/src/account/identityEpoch.ts`、`leaveGuard.ts`、`PasswordField.vue`，`frontend/scripts/test-session.mjs`。

修改：`frontend/src/account/auth.ts`、`AccountApp.vue`、`AuthPage.vue`、`ArticleEditor.vue`、`ProfilePage.vue`、`confirmation.ts`、`ConfirmationDialog.vue`；`frontend/src/api/client.ts`；`frontend/src/components/Navbar.vue`；`frontend/src/oj/OjApp.vue`；`frontend/src/router/index.ts`；`frontend/src/styles/account.css`；`frontend/package.json`；`frontend/scripts/test-api.mjs`；`scripts/Check-Render.ps1`；`backend/AUTH.md`、`backend/AUTH-VALIDATION.md`、`RENDER.md`。

没有修改已执行迁移，没有新增迁移／表／后端依赖，也没有增加前端依赖。没有提交、推送或部署。

### 依赖、限制与安全说明

- 公网仍需在 Render 应用 `aiven,render`、后端 DB_* 与 CA Secret File，以及前端 `/api/*` Rewrite；步骤见 `RENDER.md`。仅保存仓库配置不能应用已有服务。生产 Cookie、POST、公网登录与跨页面流程未执行成功验收。
- 单实例内存 Session、30 分钟期限不变。后端重启需重新登录；没有长期记住登录、持久 Session、多实例共享或 Redis。
- 慢身份请求和请求竞态用隔离 adapter 实际验证；没有测量性能百分比，也没有使用浏览器网络节流作量化基准。操作系统减少动态效果、物理 Caps Lock 切换及浏览器菜单“复制标签页”未单独实测；沿用减少动效规则，Caps Lock 读取有兼容性保护。
- 浏览器验证发现密码字段的非标准事件触发错误，开发 Vue 错误日志曾包含专用测试账号密码。已经提醒更换该测试账号密码，修复触发错误并验证控件无新增错误；未输出管理员密码、Cookie、CSRF 或数据库凭据。遵守本次不修改既有密码的约束，测试账号密码未擅自更换，仍应由用户更换；今后不输出原始组件日志。
- 既有 Aiven CA／信任库／私有配置及截图继续被 Git 忽略。没有向 Render 公网发送登录凭据，没有关闭证书校验。
