# SmartDoc — AI 智能文档助手

一个为 Java 校招作品集设计的轻量文档问答项目：上传 PDF 后自动提取文本、生成摘要和关键词，并基于相关原文片段回答问题。

## 功能

- HMAC 签名登录令牌与用户数据隔离
- PDF 上传、异步解析、状态轮询和删除
- PDFBox 分页提取、重叠切块和中英文关键词召回
- AI 摘要、关键词、单文档问答与原文页码引用
- 问答历史记录
- 零配置 Demo AI；可切换 DeepSeek 等 OpenAI 兼容 API
- 默认 H2 + 本地文件，也支持 MySQL + MinIO
- Vue 3 + Element Plus 响应式工作台

## 架构

```text
Vue 3 SPA ──REST──> Spring Boot
                         ├── AuthTokenService（HMAC）
                         ├── DocumentProcessor（PDFBox + Chunker）
                         ├── KeywordRetriever ──> AiClient
                         ├── MyBatis Plus ──> H2 / MySQL
                         └── FileStorage ──> Local / MinIO
```

`AiClient`、`FileStorage` 和检索器相互独立，后续可把关键词召回替换为 Embedding + PGVector，而无需修改控制器和前端流程。

## 最快启动（无需 Docker、数据库或 AI Key）

要求：Java 11、Maven 3.6+、Node.js 18+、pnpm。

```powershell
cd backend
mvn spring-boot:run

# 新终端
cd frontend
pnpm install
pnpm dev
```

访问 `http://localhost:3000`，使用演示账号：

```text
demo / smartdoc123
```

Demo AI 会生成确定性的本地摘要和引用回答，适合无 Key 演示完整流程。

## Docker Compose

```powershell
Copy-Item .env.example .env
# 修改 .env 中的密码
docker compose up --build
```

访问：

- SmartDoc：`http://localhost:3000`
- MinIO 控制台：`http://localhost:9001`
- 后端 API：`http://localhost:8080`

## 接入真实 AI

在 `.env` 中配置：

```dotenv
SMARTDOC_AI_TYPE=openai
AI_BASE_URL=https://api.deepseek.com/v1
AI_API_KEY=your-api-key
AI_MODEL=deepseek-chat
```

接口需兼容 `POST /chat/completions`。密钥不会写入数据库或前端。

## 核心 API

| 方法 | 地址 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/documents` | 上传 PDF |
| GET | `/api/documents` | 文档列表 |
| GET | `/api/documents/{id}` | 文档详情/处理状态 |
| DELETE | `/api/documents/{id}` | 删除文档 |
| POST | `/api/documents/{id}/questions` | 基于文档提问 |
| GET | `/api/documents/{id}/questions` | 问答历史 |

除登录外，请求需要 `Authorization: Bearer <token>`。

## 测试与构建

```powershell
cd backend
mvn test
mvn package

cd ../frontend
pnpm build
```

## 面试可讲的技术点

1. 为什么文件放对象存储、元数据放关系数据库。
2. 上传后用 `PROCESSING / READY / FAILED` 状态描述异步生命周期。
3. 通过分页切块、重叠窗口和关键词评分处理长文档上下文限制。
4. 返回页码和原文片段，降低 AI 答案不可验证的问题。
5. 所有查询绑定令牌中的用户 ID，访问他人文档统一表现为 404。
6. AI、检索和存储都面向接口，方便替换供应商和升级 RAG。

## 首版边界

仅支持最大 20 MB 的文本型 PDF；扫描件 OCR、DOCX、Redis 限流、流式回答和 PGVector 放在第二阶段。这个边界是有意为之：首版优先保证完整、可运行、可解释。

## 学习工作区快速开始

SmartDoc 支持 PDF、Markdown、TXT 和常见代码文件。前端通过 `/api` 访问 Spring Boot；默认 H2 配置适合本地快速启动，MySQL 结构变更请手动执行 `scripts/alter_smartdoc_study_workspace.sql`，若先前已应用首版工作区迁移，再执行 `scripts/alter_document_record_processing_version.sql`。

AI 有 Demo 与 DeepSeek 两种模式。Demo 不联网；DeepSeek 只接收当前资料、当前页或明确选中的文本，token 上限和每日额度可在设置页控制。API 密钥不会写入浏览器存储、URL 或日志；提交后输入框立即清空，Windows DPAPI 可用时才允许持久化，否则仅保存在服务进程内存。

前端测试与构建：`pnpm test`、`pnpm build`。架构按路由拆分阅读器、笔记与设置页面，PDF/Markdown/语法高亮依赖只在阅读器路由加载。

截图：待浏览器集成验收后补充。
