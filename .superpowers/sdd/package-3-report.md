# SmartDoc 发布整理工作包报告

## 交付内容

- 新增 GitHub Actions：后端 Maven 测试、前端安装/测试/构建，以及独立的密钥扫描。
- 更新公开仓库忽略规则与 `.env.example`，避免环境文件、H2、上传文件、MinIO/MySQL 数据和构建产物进入版本控制。
- 重写 UTF-8 中文 README，涵盖 H2 快速启动、Docker/MySQL/MinIO、Demo AI、国内服务商预设、兼容边界、文本/视觉路由、安全模型、架构、测试与手动迁移提醒。
- 新增服务商配置文档，包含 DeepSeek 和阿里云百炼（Qwen）的占位符示例。

## 密钥扫描

对工作树、已跟踪文件和完整 Git 历史执行了静态扫描，检查 OpenAI 风格 `sk-` 密钥、非占位符 AI API Key 赋值、明文 Bearer Token 和主密钥赋值；最终结果均为 0 个命中。扫描没有使用或输出任何密钥值。

初次历史扫描曾将 `git show` 的输出当作 blob 内容，产生了 13 个 `AI_API_KEY` 疑似命中。主代理复核后确认这些都是补丁上下文误判；逐 blob 复核显示唯一历史 `AI_API_KEY` 赋值位于提交 `29a6538` 且值为空。密钥工作流现改为逐个 `git cat-file blob` 扫描唯一历史 blob，避免再次把补丁上下文误报为凭据。

明确允许的测试占位符模式为 `test-key-not-secret`、`test-token-not-secret` 和 `<...>`；本次没有需要作为扫描例外报告的疑似密钥。

## 未执行项与关注点

- 按工作包约束，本包未重复运行后端/前端全量测试；最终验证由主代理统一完成。
- 未执行任何 SQL。已有 MySQL 数据库必须由用户手动执行 `scripts/alter_ai_provider_config.sql`；新数据库使用 `scripts/init_mysql.sql`。
- 仓库没有真实截图。虽然本机具备 Chrome 与前端依赖，但当前没有可靠的已登录 Demo/mock-provider 自动截图流程；为避免伪造或泄露信息，未创建 `docs/screenshots` 图片，也没有在 README 引用不存在的图片文件。
