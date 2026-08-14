# SmartDoc 国产模型多供应商接入设计

日期：2026-08-14  
状态：已确认方向，待用户复核书面规格

## 1. 目标

SmartDoc 面向国内 Java 后端校招作品集，新增一个可配置的模型服务中心。用户可以接入 DeepSeek、阿里云百炼、火山方舟、智谱、腾讯混元、百度千帆、硅基流动，以及其他实现 OpenAI Chat Completions 兼容协议的服务。

首版强调“一个兼容协议覆盖多数厂商”，而不是为每家厂商维护独立 SDK。系统同时区分文本与视觉能力，使文本模型负责文档总结和推理，视觉模型负责图片、扫描页、流程图与代码截图识别。

成功标准：

- 无 API Key 时仍可使用 Demo AI 演示完整流程。
- 用户能创建、测试、编辑、删除自定义模型服务。
- 用户能分别指定默认文本模型和默认视觉模型。
- 普通文档问题只调用文本模型；图片请求只在用户主动操作时调用视觉模型。
- 兼容服务的密钥不进入 Git、浏览器持久化、URL、普通日志或明文数据库字段。
- README 如实描述为“OpenAI-compatible”，不声称兼容所有厂商和所有模型。

## 2. 范围与取舍

### 首版包含

- `DEMO` 和 `OPENAI_CHAT_COMPLETIONS` 两种协议。
- 国内厂商预设：DeepSeek、阿里云百炼、火山方舟、智谱、腾讯混元、百度千帆、硅基流动。
- 自定义服务：显示名称、Base URL、API Key、模型名、文本/视觉能力、启用状态。
- 默认文本模型、默认视觉模型与连接测试。
- OpenAI-compatible 文本消息和 `image_url`/Base64 图片消息。
- 图片识别结果缓存，避免相同内容重复调用。
- 统一超时、限额、错误映射和敏感信息脱敏。

### 首版不包含

- Anthropic、Gemini、DashScope 原生协议。
- 为每个厂商调用模型列表接口或维护实时价格。
- 视频、音频、文生图、Tool Calling、联网搜索和流式输出。
- 自动 OCR 全库、自动分析每个 PDF 页面。
- 统一充值、平台代付、计费或模型商城。
- 修改现有关键词检索为向量数据库/RAG。

原生协议可在后续通过新的 `ProviderAdapter` 增量加入，不改变业务控制器。

## 3. 用户体验

设置页由单一 DeepSeek 表单改为“模型服务”列表：

1. 用户选择厂商预设或“自定义兼容服务”。
2. 系统填充建议 Base URL；用户填写模型名和自己的 API Key。
3. 用户声明该模型支持文本、视觉或两者。预设只提供建议值，最终以实际模型能力为准。
4. 用户点击“测试连接”，测试通过后保存。
5. 用户分别选择默认文本模型和默认视觉模型。

每个配置卡只显示掩码后的密钥状态，不回传真实密钥。编辑时 API Key 留空表示保留原值，“清除密钥”是单独的确认操作。

阅读器 AI 面板显示本次将调用的模型：

- 摘要、解释、代码问答：默认文本模型。
- 识别图片、分析扫描页：默认视觉模型。
- “视觉识别后深度分析”：视觉模型先输出结构化观察，再将观察结果与当前文档上下文交给文本模型。
- “视觉模型直接回答”：只调用一次，节省 Token。

未配置所需能力时，界面明确引导到设置页，不静默改用其他收费模型。

## 4. 架构

### 4.1 核心边界

- `ProviderConfigService`：管理当前用户的服务配置、默认路由和密钥生命周期。
- `ProviderAdapter`：协议级接口，定义文本完成、视觉完成和连接测试。
- `OpenAiCompatibleAdapter`：实现 `/chat/completions` 文本及多模态消息。
- `DemoProviderAdapter`：保持离线、确定性的演示能力。
- `ModelRouter`：按请求能力选择默认文本或视觉配置，不按厂商名称写分支。
- `VisionObservationService`：将视觉模型输出标准化为描述、OCR、代码/图表信息、不确定项和来源页。
- `VisionCache`：以用户、文件 SHA-256、模型、提示词版本共同作为缓存键。

现有 `AiClient` 的摘要、问答和学习动作由 `ModelRouter` 提供选中的适配器。现有 `OpenAiCompatibleClient` 的请求与响应逻辑下沉到协议适配器，去除 `DeepSeekClientFactory` 这一厂商绑定命名。

### 4.2 数据流

文本请求：

```text
AI Action -> ModelRouter(TEXT) -> ProviderConfig -> OpenAiCompatibleAdapter
          -> 厂商 API -> 标准化文本结果 -> 历史记录/页面
```

视觉协作请求：

```text
当前图片或 PDF 当前页
  -> 大小与类型校验
  -> VisionCache 查询
  -> ModelRouter(VISION)
  -> 视觉模型生成 VisionObservation
  -> 缓存观察结果
  -> ModelRouter(TEXT)
  -> 文本模型结合观察结果与当前资料生成最终回答
```

不允许后台上传后自动触发视觉调用。所有付费调用都由明确的用户动作触发。

## 5. 数据模型与 API

新增 `ai_provider_config`：

- `id`, `user_id`, `display_name`
- `preset_code`, `protocol`, `base_url`, `model`
- `supports_text`, `supports_vision`, `enabled`
- `encrypted_api_key`, `key_present`
- `created_at`, `updated_at`
- 唯一约束：同一用户的显示名称不重复

新增 `ai_routing_config`：

- `user_id`
- `default_text_provider_id`
- `default_vision_provider_id`
- `updated_at`

新增 `ai_vision_cache`：

- `id`, `user_id`, `content_sha256`
- `provider_id`, `model`, `prompt_version`
- `observation`, `created_at`, `expires_at`
- 组合唯一约束防止并发产生重复缓存

所有查询必须在 SQL 边界绑定 `user_id`。删除服务前检查默认路由引用；删除后不得残留可解密密钥。

主要 API：

- `GET /api/ai/providers`
- `POST /api/ai/providers`
- `PUT /api/ai/providers/{id}`
- `DELETE /api/ai/providers/{id}`
- `POST /api/ai/providers/{id}/test`
- `GET /api/ai/routing`
- `PUT /api/ai/routing`
- `POST /api/documents/{id}/vision-actions`

旧 `/api/ai/settings` 在迁移期保留只读兼容或由前端一次性转为默认 Provider；最终 README 和前端只使用新 API。

## 6. 密钥与网络安全

- `.env`、真实 API Key 和运行时密钥文件必须在 `.gitignore` 中。
- `.env.example` 只包含占位符。
- 浏览器提交成功后立即清空输入；Pinia 不持久化 API Key。
- 后端永不返回真实 Key，只返回掩码和 `keyPresent`。
- 多用户部署时使用环境变量提供的主密钥，以 AES-GCM 加密每个 Provider Key；主密钥不进入数据库。
- 未配置主密钥时，只允许进程内存保存 API Key，并在页面提示重启后失效；不得降级为明文数据库存储。
- 保留现有 Windows DPAPI 作为个人本地运行的可选 SecretStore，但不能把它作为 Linux/Docker 部署的唯一方案。
- 日志、异常、审计字段统一脱敏 Authorization、API Key、查询参数和响应中的潜在密钥。
- Base URL 仅接受 HTTPS；HTTP 只允许显式启用的 loopback 本地模型。
- 默认阻止云元数据地址和内网地址，管理员通过部署配置显式允许私网模型，降低 SSRF 风险。
- 自定义地址保存前提示：API Key 将发送给该地址；测试请求也会产生厂商调用。

## 7. 限额、缓存与失败处理

- 配置连接、读取和删除不消耗 AI 额度；测试连接和实际推理消耗额度。
- 限额按用户统计，记录文本调用、视觉调用和协作调用的次数；不伪造跨厂商统一费用。
- HTTP 连接与读取均设置超时；限制响应体大小，避免异常服务占满内存。
- 图片仅接受允许的 MIME 类型和受控大小，PDF 只渲染用户选中的当前页。
- 视觉缓存命中时不再次调用视觉模型；模型名、图片内容或提示词版本变化会自然失效。
- 401/403 映射为“密钥无效或无权限”；404 映射为“地址或模型不存在”；429 映射为“厂商限流”；超时和 5xx 映射为可重试服务错误。
- 返回给前端的错误包含可操作说明，但不包含供应商原始响应中的敏感字段。
- 视觉成功、文本失败时保留本次视觉观察缓存，用户重试只需再次调用文本模型。

## 8. 测试策略

后端：

- 使用本地 Mock HTTP Server 验证标准文本、多模态请求、Bearer 鉴权和响应解析。
- 验证不同 Base URL 尾斜杠、兼容参数差异、空响应和非 JSON 响应。
- 验证 401、404、429、超时、5xx 的统一错误映射。
- 验证模型能力路由、默认项所有权和禁用配置不可调用。
- 验证 API Key 不出现在 DTO、日志、异常和数据库明文字段。
- 验证视觉缓存命中、并发去重、模型/提示版本隔离。
- 验证用户 A 不能读取、测试、修改或删除用户 B 的 Provider。

前端：

- Provider 新建、编辑、删除、掩码、清空密钥和测试连接交互。
- 默认文本/视觉模型选择及缺失能力提示。
- API Key 提交后从表单和 Store 清除。
- 视觉直接回答和“视觉 + 文本”两条调用路径。

发布前执行完整后端测试、前端测试与生产构建。

## 9. GitHub 交付

- README 增加模型适配架构图、国内厂商配置示例和兼容边界。
- 默认配置使用 Demo AI，克隆仓库后无需 Key 即可运行。
- 提供安全的 `.env.example`，不提供任何可用密钥。
- 提供至少一个文本 Mock 示例和一个视觉 Mock 示例，CI 不访问真实付费 API。
- 增加 GitHub Actions 执行后端测试、前端测试和构建。
- 上传前扫描当前工作树及 Git 历史中的常见密钥格式；发现真实密钥必须先吊销再清理历史。
- 项目说明使用“支持 OpenAI Chat Completions 兼容服务”，不使用“支持所有大模型 API”。

## 10. 实施顺序

1. Provider 配置、用户隔离、加密存储和迁移兼容。
2. 通用文本适配器、连接测试、统一错误和能力路由。
3. 设置页 Provider 管理与默认文本模型。
4. OpenAI-compatible 视觉输入、视觉缓存和 PDF 当前页入口。
5. 默认视觉模型与“视觉眼睛 + 文本大脑”协作。
6. README、示例、CI、密钥扫描和发布验收。

该顺序保证前四步完成后就有可公开展示的增量；视觉协作出现问题时，不影响现有文档学习功能。
