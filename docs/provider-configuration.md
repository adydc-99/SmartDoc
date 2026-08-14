# AI 服务商配置

SmartDoc 在“设置 → AI 服务商”中按当前用户保存服务商和路由配置。服务商密钥在提交后立即从浏览器输入框清除，接口只返回掩码状态；请勿把密钥放到 `.env`、文档、截图或 Git 历史中。

## 配置步骤

1. 使用 Demo 账号登录，打开“设置 → AI 服务商”。
2. 点击“添加服务商”，选择预设或“自定义兼容服务”，填写显示名称、基础 URL、模型名、文本/视觉能力和 API Key。
3. 保存后，在“服务商与默认路由”中分别选择默认文本模型和默认视觉模型；视觉深度分析还需要可用的默认文本模型。
4. 可用“测试”按钮验证文本服务商连通性。此操作会调用该服务商，并计入每日配额。

服务商可由各用户独立配置。请在服务商控制台确认模型名、可用性、区域、价格和配额；这些内容可能变化，SmartDoc 的预设不作保证。

## DeepSeek 示例

在新增服务商表单中使用以下字段。`<...>` 是占位符，不是可以使用的凭据。

| 字段 | 示例值 |
| --- | --- |
| 预设 | `DEEPSEEK`（DeepSeek） |
| 协议 | `OPENAI_CHAT_COMPLETIONS` |
| 基础 URL | `https://api.deepseek.com/v1` |
| 模型 | `<confirm-a-current-deepseek-text-model>` |
| 文本能力 | 启用 |
| 视觉能力 | 按实际模型能力设置；预设默认关闭 |
| API Key | `<deepseek-api-key>` |

## 阿里云百炼（Qwen）示例

| 字段 | 示例值 |
| --- | --- |
| 预设 | `QWEN`（阿里云百炼） |
| 协议 | `OPENAI_CHAT_COMPLETIONS` |
| 基础 URL | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| 模型 | `<confirm-a-current-qwen-model>` |
| 文本能力 | 启用 |
| 视觉能力 | 仅在所选模型支持时启用 |
| API Key | `<qwen-api-key>` |

## 预设与兼容边界

当前预设包括：DeepSeek、阿里云百炼（Qwen）、火山方舟、智谱、腾讯混元、百度千帆、硅基流动，以及自定义兼容服务。预设只填入已知基础 URL 和默认能力提示；请自行核实模型与计费信息。

SmartDoc supports services implementing the OpenAI Chat Completions request/response shape used here; presets do not guarantee every model or vendor extension.

具体而言，SmartDoc 仅发送本项目所需的 Chat Completions 文本请求，以及以 `image_url` 数据 URL 传图的视觉请求；它不实现原生 Gemini、Anthropic、DashScope 协议，也不承诺 Responses、Assistants、流式输出、工具调用、音频、视频、模型发现或其他厂商扩展。

## 密钥、网络与路由安全

- 密钥只在后端使用；浏览器存储、URL 和常规服务商读取响应均不包含明文密钥。
- 后端具备安全密钥持久化能力时，会加密保存；否则密钥只保留在服务进程内存，重启后需要重新输入。
- 服务商地址默认要求 HTTPS。环回地址和私有网络地址默认不允许；仅在受控开发环境中可通过后端环境变量显式放开。
- 文本与视觉路由独立，且必须引用属于当前用户、已启用、具备相应能力并已配置密钥的服务商。
- 每日调用次数和最大输出 token 由每个用户的路由配置限制；视觉识别结果会按内容、模型和提示词版本缓存一段时间。
