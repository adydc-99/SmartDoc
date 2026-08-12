# SmartDoc MVP 设计

## 目标

在最短时间内交付一个适合 Java 校招展示的独立项目，完整演示“登录、上传 PDF、解析、摘要、基于文档问答、来源引用和历史记录”。项目必须在没有 AI Key、MySQL、MinIO 的电脑上也能启动演示，并可通过配置切换到真实 OpenAI 兼容模型、MySQL 和 MinIO。

## 范围

首版只支持 PDF；提供预置账号 `demo / smartdoc123`；不做注册、后台管理、OCR、多文档知识库、向量数据库、消息队列和复杂权限。单文件上限 20 MB。

## 架构

- 后端：Java 11、Spring Boot 2.7.18、MyBatis-Plus、H2/MySQL、PDFBox。
- 前端：Vue 3、TypeScript、Vite、Element Plus。
- 存储：默认本地目录，生产配置切换 MinIO。
- AI：`AiClient` 接口隔离模型实现；默认演示实现保证零 Key 可运行，配置后切换 OpenAI 兼容 `/chat/completions`。
- 检索：PDF 文本按段切块，使用中英文 token 匹配评分召回 Top 3；接口边界允许后续替换 PGVector。

## 数据流

1. 用户用预置账号登录，后端返回 HMAC 签名的短期令牌。
2. 上传 PDF 后先保存文件和 `PROCESSING` 元数据，再异步解析文本。
3. 解析器提取文本并切块；AI 客户端生成摘要和关键词；文档变为 `READY`。失败则记录可读错误并变为 `FAILED`。
4. 提问时只检索当前用户、当前文档的相关片段，将片段交给 AI 客户端，并返回答案与引用片段。
5. 每次问答保存历史；所有文档和问答查询都带用户 ID。

## 错误处理与安全

- 校验 PDF 扩展名、Content-Type、空文件和 20 MB 限制。
- 令牌过期或签名不匹配返回 401；访问他人资源返回 404，避免泄露存在性。
- AI 调用失败返回明确错误，不删除原文件；解析失败保留失败状态。
- 密钥只从环境变量读取；仓库只提交 `.env.example`。
- 真实部署由 Redis 限流作为第二阶段；首版避免为了组件数量扩大范围。

## 测试与验收

- 单元测试覆盖令牌签发/校验、文本切块与关键词召回、演示 AI 输出。
- 后端 Maven 测试和打包通过；前端 TypeScript 构建通过。
- 无外部服务时可用 H2、本地存储、演示 AI 完成整条流程。
- Docker Compose 提供 MySQL、MinIO、后端和前端部署配置。

## 后续升级

第二阶段增加 Redis 限流、PGVector/Embedding、流式回答、DOCX 和 Actuator 指标；这些升级通过现有接口替换实现，不改变前端核心流程。
