# SmartDoc MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付可零配置演示、可切换真实基础设施的 PDF 智能文档助手。

**Architecture:** 单体 Spring Boot API 与 Vue SPA 分离。后端通过存储、AI、检索接口隔离外部依赖，默认 H2/本地存储/演示 AI，生产配置使用 MySQL/MinIO/OpenAI 兼容 API。

**Tech Stack:** Java 11, Spring Boot 2.7.18, MyBatis-Plus 3.5.5, PDFBox, Vue 3, TypeScript, Vite, Element Plus, MySQL 8, MinIO

## Global Constraints

- 首版仅支持最大 20 MB PDF。
- 预置账号为 `demo / smartdoc123`，不提供注册和管理后台。
- 所有资源访问按令牌中的用户 ID 隔离。
- 默认模式不依赖 Docker 或 AI Key。
- 新业务行为遵循测试先行。

---

### Task 1: 建立后端骨架与认证

**Files:** `backend/pom.xml`, `backend/src/main/**`, `backend/src/test/**`

**Interfaces:** 产生 `POST /api/auth/login` 和 Bearer 令牌过滤器。

- [ ] 先写令牌签发、过期、篡改测试并运行，确认因实现缺失而失败。
- [ ] 实现 HMAC 令牌服务、登录控制器和请求过滤器。
- [ ] 运行 `mvn test`，确认认证测试通过。

### Task 2: 文档解析、切块和检索

**Files:** `backend/src/main/java/com/smartdoc/document/**`, `backend/src/test/java/com/smartdoc/document/**`

**Interfaces:** 产生 `TextChunker.split(String)`、`KeywordRetriever.retrieve(String,List<Chunk>,int)` 和 PDF 提取器。

- [ ] 先写段落切块、长段切分、中英文问题召回测试并确认失败。
- [ ] 实现最小切块与评分算法。
- [ ] 运行定向测试并确认通过。

### Task 3: 文件存储与文档 API

**Files:** `backend/src/main/java/com/smartdoc/storage/**`, `backend/src/main/java/com/smartdoc/document/**`, `backend/src/main/resources/**`

**Interfaces:** 产生文档上传、列表、详情、删除 API；存储接口支持本地和 MinIO。

- [ ] 先写上传校验与用户隔离服务测试并确认失败。
- [ ] 实现数据库模型、Mapper、存储适配器和异步处理器。
- [ ] 运行后端完整测试并确认通过。

### Task 4: 摘要、问答和历史

**Files:** `backend/src/main/java/com/smartdoc/ai/**`, `backend/src/main/java/com/smartdoc/chat/**`, corresponding tests

**Interfaces:** 产生 `POST /api/documents/{id}/questions` 与历史查询 API；回答含 `answer`、`references`。

- [ ] 先写演示摘要、关键词、问答引用测试并确认失败。
- [ ] 实现演示 AI、OpenAI 兼容 AI、问答服务和历史持久化。
- [ ] 运行后端完整测试与打包。

### Task 5: Vue 展示界面

**Files:** `frontend/package.json`, `frontend/src/**`, `frontend/vite.config.ts`

**Interfaces:** 消费认证、文档和问答 API。

- [ ] 建立登录、文档列表/上传、详情摘要和问答界面。
- [ ] 实现轮询处理状态、错误提示和令牌持久化。
- [ ] 运行 `pnpm build` 并修复所有 TypeScript 错误。

### Task 6: 部署与项目展示

**Files:** `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `.env.example`, `README.md`

**Interfaces:** 提供本地快速启动和 Compose 两种运行方式。

- [ ] 添加 MySQL、MinIO、后端、前端 Compose 配置和健康检查。
- [ ] 编写架构、功能、配置、API、面试亮点和升级路线文档。
- [ ] 重新运行 `mvn test`, `mvn package`, `pnpm build` 并记录结果。
