# SmartDoc MVP 工作计划

## 目标
尽快交付独立、可运行、适合校招展示的 SmartDoc MVP。

## 阶段
- [x] 范围与架构设计
- [x] 实施计划
- [ ] 后端认证与核心领域
- [ ] 文档上传、解析与问答
- [ ] Vue 展示界面
- [ ] Docker 与 README
- [ ] 完整验证

## 固定决策
- Java 11 + Spring Boot 2.7，适配当前机器。
- 默认 H2 + 本地存储 + Demo AI，保证零外部依赖演示。
- 生产配置支持 MySQL + MinIO + OpenAI 兼容 API。
- 仅 PDF、预置账号、不做管理后台和向量库。

## 已知环境问题
- Docker CLI 已安装但 Docker Desktop daemon 未启动；Compose 只做静态配置与后续验证。
- `rg.exe` 在当前环境 Access Denied，文本搜索改用 PowerShell `Select-String`。
