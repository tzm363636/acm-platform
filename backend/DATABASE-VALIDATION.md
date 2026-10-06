# 数据库交付与实测记录

验收日期：2026-10-06（Asia/Shanghai）。本记录不包含主机、用户名、密码、连接字符串或会话令牌。

本文保留 V1/V2 数据库阶段的验收记录。后续 V3 用户认证、文章审核与控制台已经接入；当前结果见 `AUTH-VALIDATION.md`，操作见 `AUTH.md`。下面“尚未接入”中的账号和文章管理说明仅表示当时阶段，不代表当前状态。

## 实际完成的范围

| 层次 | 状态与证据 |
|---|---|
| 表结构已设计 | 12 张业务表，字段、索引、外键、删除规则和关系图见 DATABASE.md；V1/V2 SQL 为结构权威来源 |
| 云数据库实际连接 | 用户已创建的 Aiven MySQL 8.4.8；Connector/J 使用 VERIFY_IDENTITY 和服务 CA 信任库，TLS 已协商成功 |
| 迁移已执行 | 初查目标库为 0 表；执行 V1、增量 V2 后为 12 张业务表及 flyway_schema_history；再次 migrate 输出 executed migrations: 0 |
| 内容已导入 | 显式开发 seed 导入网站现有 8 篇文章和 8 道 DEMO 题目，不复制截图的通过率或预置 AC；重复 seed 不新增、不覆盖已有内容 |
| 后端已接入 | Spring JDBC 参数化查询、Hikari 小连接池；文章、题库、提交、详情和统计接口直接访问 MySQL；数据库执行筛选、排序、分页 |
| 前端已接入 | 本地 VITE_DATA_SOURCE=api，Vue 经 /api 代理访问 Java；请求失败不会暗中切回离线数据；代码草稿仍保存在浏览器 |
| 页面已验证 | 文章筛选/取消、文章目录、题库搜索及返回、演示运行/提交/快照/继续修改、提交筛选、未登录提示及后端重启后读取 |

没有创建、删除或升级本机 WAMP MySQL；没有新建 Aiven 服务（服务由用户创建）；没有部署公网网站、Git 提交或推送。

当前私有配置明确为 development，允许演示写入；公开 Aiven 模板默认 cloud、禁止种子与演示写入。当前网页中题目、运行、提交及统计为演示数据，不是真实执行或判题。保留了 1 条带“数据库联调专用”注释的 WA 演示提交，便于在详情页复核；它不影响任何正式统计。

## 实测检查

- Maven package 成功，10 项 JUnit 检查通过：版本/参数/排序/LIKE/分页/会话/TLS/前端判题字段注入/空指标场景。
- DB_ACTION=verify 在真实 Aiven 上完成 20 项检查：中文与 emoji、代码换行、多标签、多样例、用户和题目关联、快照与源码权限、正式/演示隔离、重复 AC 按题去重、数据库筛选排序分页、唯一/FK/CHECK、历史数据删除限制。专用测试数据在事务末尾回滚；自增序号可能前移，不要求连续。
- 重复 migrate 执行 0 项；重复 seed 后文章与演示题目仍各 8 条。
- Vue/TypeScript 生产构建通过；原有 12 项离线 OJ 检查通过。
- HTTP 运行 AC/WA/TLE/MLE/RE/CE/SystemError 固定场景返回对应结果；NetworkError 返回 503；运行前后提交总数不变。CE 的耗时和内存为 null，编译信息保留换行。
- 未登录查询个人记录返回 401；正式提交返回 501；不存在的题目、文章、提交返回 404；没有会话权限的详情省略源码。
- API mode=real 的题库和统计均为 0；mode=demo 题库为 8。演示身份不能获取真实个人状态。
- 浏览器：搜索 P1001 → 题目 → 返回，URL 与搜索值保留；手机题目/代码切换和刷新保留代码与测试输入。
- 浏览器：固定 CE 运行 → 错误反馈 → 修复服务后重试成功；随后 WA 演示提交由后端推进并生成唯一编号 → 详情 → 继续修改。恢复原有草稿后，历史快照仍是提交时的代码。
- 重启 Java 后端，再刷新同一提交详情，结果、时间和快照可读取；文章/题库仍可通过接口读取。
- 浏览器：数据库文章异步加载后生成 3 项目录；跳转末节后高亮对应章节、阅读进度为 100%；返回链接保留 more=1。
- 浏览器：再次点击“图论”取消，控件可视 top 在两次结果切换前后约为 405px；保留原有侧栏锚点行为。
- 桌面 1280px、手机 390px 和 320px 代表页面未发生整页横向溢出；390px 详情/编辑/文章、320px 提交详情完成实际检查。不是对所有页面所有尺寸的穷举验证。
- 私有 properties、CA、PKCS12 和前端 .env.local 均由 Git 忽略；git diff --check 通过。

## 文件清单

后端新增：

- `.env.example`、`aiven.env.example`、`DATABASE.md`、`DATABASE-VALIDATION.md`。
- `scripts/Database.ps1`、`scripts/Initialize-TrustStore.ps1`。
- `src/main/resources/application-mysql-local.properties`、`application-aiven.properties`、`database.properties`。
- `src/main/resources/db/migration/V1__content_and_oj.sql`、`V2__submission_consistency.sql`。
- `src/main/resources/db/seed/site-content.json`。
- `src/main/java/com/acm/platform/db/DatabaseConfig.java`、`DatabaseLifecycle.java`、`DatabaseVerification.java`、`DevelopmentSeed.java`、`DemoService.java`、`Page.java`、`PlatformRepository.java`。
- `src/main/java/com/acm/platform/controller/DataController.java`、`CapabilitiesController.java`、`ApiErrors.java`。
- `src/test/java/com/acm/platform/db/DatabaseRulesTest.java`。

后端修改：`pom.xml`、`PlatformApplication.java`、`application.properties`。

前端新增：`.env.example`、`scripts/export-db-seed.mjs`、`src/api/database.ts`、`content.ts`、`src/oj/adapter.ts`、`databaseApi.ts`。

前端修改：`src/api/client.ts`；`src/oj/OjApp.vue`、`ProblemList.vue`、`ProblemDetail.vue`、`SubmissionList.vue`、`SubmissionDetail.vue`、`Sidebar.vue`、`store.ts`、`types.ts`、`api.ts`；`src/views/CodeSharingView.vue`、`ArticleView.vue`；`src/composables/useArticleReading.ts`（支持接口正文晚于挂载到达）。

公共修改：仓库 `.gitignore`、`README.md`。本地私有文件不属于可提交交付文件。

## 尚未执行或未接入

- 真实账号认证、正式提交判题、后台判题任务和新增语言：未实现。正式源码接口需要未来可信的服务端认证授权。
- 文章发布/管理、题目管理、隐藏测试存储：没有扩展后台管理功能，现有查询及开发演示已接入。
- 正式数据种子、生产迁移、公开 Java 部署、多实例容量验证、最小权限运行账号切换：未执行。
- 本地 MySQL 8.4 部署和迁移：未执行，本机旧 5.7 不兼容本方案。
- 原编辑器生产包仍约 537KB，构建有 >500KB 提示；未因本任务改造编辑器或宣称性能量化提升。
- 参考截图保存于工作区 `.qa/database-desktop.jpg`、`.qa/database-mobile-390.jpg`，不入 Git。

初始化、环境变量、TLS、云端单次迁移步骤及本地切换命令见 DATABASE.md。私有凭据已配置，无需在聊天发送；更换服务凭据或 CA 时需在本地更新并重启后端。
