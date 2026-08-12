# SmartDoc 学习资料工作台第一阶段 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把现有单份 PDF 问答 MVP 升级为可长期使用的单用户本地资料库，完成资料组织、多格式阅读、阅读进度、页码笔记、DeepSeek 设置和按需 AI 辅助。

**Architecture:** 保持 Spring Boot 单体 API 与 Vue 3 SPA 分离，后端按 `library/reader/note/settings/ai` 领域拆分。现有 `document_record` 继续作为资料主表，新增关联表承载文件夹、标签、阅读进度、笔记和 AI 结果缓存；前端改为 Router + Pinia 的工作台结构，并以 PDF.js、Markdown 和 Highlight.js 提供分类型阅读器。

**Tech Stack:** Java 11, Spring Boot 2.7.18, MyBatis-Plus 3.5.5, H2/MySQL 8, PDFBox, Vue 3.5, TypeScript 5.7, Vue Router, Pinia, PDF.js, markdown-it, highlight.js, Element Plus, Vitest, Vue Test Utils

## Global Constraints

- 单用户本地工作台，不增加注册、多租户或密码找回。
- 第一阶段只支持 PDF、Markdown、TXT 和白名单中的文本代码文件；不实现 DOCX、OCR、PGVector、Ollama 和在线代码执行。
- DeepSeek Key 默认只驻留后端内存；持久保存仅在 Windows DPAPI 可用时开放，API 永不返回完整 Key。
- 普通阅读、关键词搜索、笔记和文件管理不得调用 AI。
- 所有 AI 结果标识 `DEMO` 或 `DEEPSEEK`，并记录来源、生成时间和可复用缓存键。
- 上传和解析过程不自动调用 AI；摘要、问答和解释都必须由用户点击触发，因此闲置阅读不会消耗 Token。
- 继续使用 TDD：每项业务行为先出现预期失败，再写最小实现。
- 所有数据库变更同时更新 H2 schema 与 `scripts/alter_smartdoc_study_workspace.sql`；不自动执行用户 MySQL。
- UI 遵循墨绿色设计系统、44×44px 触控区、可见焦点、语义化结构、低动效和 375/768/1024/1440px 响应式要求。
- 下文 `mvn` 命令均在 `backend/` 执行，`pnpm` 命令均在 `frontend/` 执行，文件路径均相对仓库根目录。

## File Map

### Backend

- `library/`: `FolderRecord`, `TagRecord`, `DocumentTagRecord`, `LibraryService`, `LibraryController` 及 Mapper，负责文件夹、标签、收藏、移动与资料查询。
- `reader/`: `ReadingProgressRecord`, `DocumentContentService`, `ReaderService`, `ReaderController`，负责内容读取、文档内搜索和位置恢复。
- `search/`: `SearchService`, `SearchController`，负责文件名、正文、笔记和标签的统一关键词搜索。
- `note/`: `NoteRecord`, `NoteTagRecord`, `NoteService`, `NoteController`，负责页码摘录、Markdown 笔记、标签、收藏和导出。
- `settings/`: `AiSettingsService`, `AiSettingsController`, `DpapiSecretStore`，负责运行时配置、掩码和本机密钥存储。
- `ai/`: 扩展 `AiClient`、新增 `AiActionService`, `AiResultRecord` 和调用限额，负责选区解释、代码操作与结果缓存。
- `document/`: 扩展资料类型、解析状态、内容下载和级联删除。

### Frontend

- `router/index.ts`: 页面路由与标题。
- `stores/`: `documents.ts`, `reader.ts`, `settings.ts`。
- `layouts/WorkspaceLayout.vue`: 应用导航、移动抽屉和全局状态。
- `views/`: `DashboardView`, `LibraryView`, `ReaderView`, `NotesView`, `AiChatView`, `ReviewView`, `SettingsView`。
- `components/library/`: 资料树、列表、筛选、批量上传。
- `components/reader/`: PDF、Markdown、文本、代码阅读器和左右侧栏。
- `components/note/`: 笔记编辑器与笔记列表。
- `components/ai/`: AI 面板、快捷动作和来源引用。

---

### Task 1: 数据迁移与资料类型模型

**Files:**
- Create: `scripts/alter_smartdoc_study_workspace.sql`
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/java/com/smartdoc/document/DocumentRecord.java`
- Create: `backend/src/main/java/com/smartdoc/document/DocumentType.java`
- Create: `backend/src/test/java/com/smartdoc/document/DocumentTypeTest.java`

**Interfaces:**
- Produces: `DocumentType.fromFilename(String): DocumentType`, `DocumentType.isText(): boolean`。
- Adds document fields: `documentType`, `mimeType`, `favorite`, `folderId`, `lastOpenedAt`, `contentText`。

- [ ] **Step 1: 写资料类型失败测试**

```java
@Test void detectsSupportedDocumentTypes() {
    assertEquals(DocumentType.PDF, DocumentType.fromFilename("jvm.pdf"));
    assertEquals(DocumentType.MARKDOWN, DocumentType.fromFilename("notes.md"));
    assertEquals(DocumentType.CODE, DocumentType.fromFilename("Demo.java"));
    assertThrows(InvalidDocumentException.class, () -> DocumentType.fromFilename("archive.zip"));
}
```

- [ ] **Step 2: 运行测试确认 RED**

Run: `mvn -q -Dtest=DocumentTypeTest test`

Expected: test compilation fails because `DocumentType` does not exist.

- [ ] **Step 3: 实现类型白名单**

```java
import java.util.Locale;

public enum DocumentType {
    PDF, MARKDOWN, TEXT, CODE;

    public static DocumentType fromFilename(String filename) {
        if (filename == null) {
            throw new InvalidDocumentException("文件名不能为空");
        }
        String normalized = filename.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".pdf")) return PDF;
        if (normalized.endsWith(".md") || normalized.endsWith(".markdown")) return MARKDOWN;
        if (normalized.endsWith(".txt")) return TEXT;
        String[] codeExtensions = {
            ".java", ".xml", ".yml", ".yaml", ".sql", ".js", ".ts",
            ".json", ".properties", ".sh", ".ps1"
        };
        for (String extension : codeExtensions) {
            if (normalized.endsWith(extension)) return CODE;
        }
        throw new InvalidDocumentException("暂不支持该文件类型");
    }

    public boolean isText() { return this != PDF; }
}
```

允许扩展名固定为 `.pdf`, `.md`, `.markdown`, `.txt`, `.java`, `.xml`, `.yml`, `.yaml`, `.sql`, `.js`, `.ts`, `.json`, `.properties`, `.sh`, `.ps1`；其余抛出 `InvalidDocumentException("暂不支持该文件类型")`。

- [ ] **Step 4: 编写两套 schema**

H2 `schema.sql` 使用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`；MySQL 脚本为 `document_record` 增加：

```sql
document_type VARCHAR(20) NOT NULL DEFAULT 'PDF',
mime_type VARCHAR(100),
favorite BOOLEAN NOT NULL DEFAULT FALSE,
folder_id BIGINT,
last_opened_at DATETIME,
content_text LONGTEXT
```

同一 SQL 文件创建以下表，字段与后端模型保持一一对应：

- `folder(id, parent_id, name, sort_order, created_at, updated_at)`，索引 `parent_id`。
- `tag(id, name, color, created_at)`，唯一约束 `name`。
- `document_tag(id, document_id, tag_id)`，唯一约束 `(document_id, tag_id)`，分别索引两个外键列。
- `reading_progress(id, document_id, page_number, scroll_ratio, zoom, updated_at)`，唯一约束 `document_id`。
- `note(id, document_id, page_number, source_text, content_markdown, favorite, created_at, updated_at)`，索引 `(document_id, page_number)` 和 `(favorite, updated_at)`。
- `note_tag(id, note_id, tag_id)`，唯一约束 `(note_id, tag_id)`，分别索引两个外键列。
- `ai_result(id, document_id, action, cache_key, source_page, source_text, content_markdown, mode, model, created_at)`，索引 `(document_id, cache_key, created_at)`。

该文件明确标注为一次性迁移脚本，文件头记录执行前检查命令与执行后验证命令；不要由应用自动执行用户 MySQL。H2 对应长文本使用 `CLOB`，MySQL 使用 `LONGTEXT`；布尔字段分别使用 `BOOLEAN`，时间分别使用 `TIMESTAMP`/`DATETIME`。

- [ ] **Step 5: 运行完整后端测试并提交**

Run: `mvn test`

Expected: all existing tests plus `DocumentTypeTest` pass.

Commit: `feat: add study document types and schema`

---

### Task 2: 文件夹、标签、收藏与资料查询

**Files:**
- Create: `backend/src/main/java/com/smartdoc/library/FolderRecord.java`
- Create: `backend/src/main/java/com/smartdoc/library/TagRecord.java`
- Create: `backend/src/main/java/com/smartdoc/library/DocumentTagRecord.java`
- Create: `backend/src/main/java/com/smartdoc/library/mapper/FolderMapper.java`
- Create: `backend/src/main/java/com/smartdoc/library/mapper/TagMapper.java`
- Create: `backend/src/main/java/com/smartdoc/library/mapper/DocumentTagMapper.java`
- Create: `backend/src/main/java/com/smartdoc/library/LibraryService.java`
- Create: `backend/src/main/java/com/smartdoc/library/LibraryController.java`
- Create: `backend/src/test/java/com/smartdoc/library/LibraryServiceTest.java`

**Interfaces:**
- `GET/POST/PATCH/DELETE /api/folders`
- `GET/POST/DELETE /api/tags`
- `PATCH /api/documents/{id}/organization` body `{folderId, favorite, tagIds}`
- `GET /api/library/documents?folderId=&tagId=&favorite=&type=&sort=&query=`
- Produces DTOs `FolderNode`, `TagView`, `DocumentListItem`。

- [ ] **Step 1: 写文件夹树和循环移动失败测试**

```java
@Test void buildsTreeAndRejectsMovingFolderUnderDescendant() {
    FolderRecord root = service.create("Java", null);
    FolderRecord child = service.create("JVM", root.getId());
    assertEquals("JVM", service.tree().get(0).getChildren().get(0).getName());
    assertThrows(InvalidDocumentException.class,
        () -> service.move(root.getId(), child.getId()));
}
```

- [ ] **Step 2: 运行测试确认 RED**

Run: `mvn -q -Dtest=LibraryServiceTest test`

Expected: compilation fails on missing library types.

- [ ] **Step 3: 实现最小文件夹与标签服务**

`FolderRecord` fields: `id`, `parentId`, `name`, `sortOrder`, `createdAt`, `updatedAt`。

`TagRecord` fields: `id`, `name`, `color`, `createdAt`；标签名唯一，颜色仅允许 `#RRGGBB`。

`DocumentTagRecord` fields: `id`, `documentId`, `tagId`；建立唯一约束 `(document_id, tag_id)`。

移动文件夹必须拒绝自身和任意后代；删除非空文件夹返回 400 并提示先移动资料。

- [ ] **Step 4: 实现收藏、移动、标签绑定和过滤**

```java
@Transactional
public DocumentListItem organize(long documentId, Long folderId,
                                 Boolean favorite, List<Long> tagIds)
```

更新时验证文档、文件夹和标签存在；重建指定文档的标签关联。查询支持名称模糊匹配、精确类型、收藏和标签过滤，默认按 `updatedAt DESC`。

- [ ] **Step 5: 增加控制器集成测试**

使用 MockMvc 创建文件夹、标签，组织一个测试文档，再按收藏和标签过滤并断言只返回该文档。

- [ ] **Step 6: 验证并提交**

Run: `mvn test`

Commit: `feat: organize documents with folders and tags`

---

### Task 3: 多格式上传、解析和安全读取

**Files:**
- Modify: `backend/src/main/java/com/smartdoc/document/DocumentUploadValidator.java`
- Modify: `backend/src/main/java/com/smartdoc/document/DocumentService.java`
- Modify: `backend/src/main/java/com/smartdoc/document/DocumentProcessor.java`
- Create: `backend/src/main/java/com/smartdoc/document/TextDocumentExtractor.java`
- Create: `backend/src/main/java/com/smartdoc/document/DocumentContentController.java`
- Modify: `backend/src/main/java/com/smartdoc/storage/FileStorage.java`
- Create: `backend/src/test/java/com/smartdoc/document/TextDocumentExtractorTest.java`
- Modify: `backend/src/test/java/com/smartdoc/SmartDocFlowIntegrationTest.java`

**Interfaces:**
- `POST /api/documents` accepts supported types and optional `folderId`。
- `GET /api/documents/{id}/content` streams PDF or returns UTF-8 text DTO `{type, language, content}`。
- `POST /api/documents/{id}/retry` retries FAILED parsing.
- `GET /api/documents/{id}/delete-impact` returns `{notes, excerpts, questions, aiResults, reviewItems}` before confirmation.

- [ ] **Step 1: 写 UTF-8 文本和二进制拒绝测试**

```java
@Test void readsUtf8AndRejectsBinaryContent() {
    assertEquals("public class Demo {}", extractor.read(javaBytes));
    assertThrows(InvalidDocumentException.class, () -> extractor.read(binaryWithNullBytes));
}
```

- [ ] **Step 2: 运行测试确认 RED**

Run: `mvn -q -Dtest=TextDocumentExtractorTest test`

Expected: compilation fails because extractor is absent.

- [ ] **Step 3: 扩展上传验证**

以 `DocumentType.fromFilename` 为最终判断，MIME 仅作为辅助；PDF 20MB，文本/代码 5MB。文本解析仅接受有效 UTF-8，拒绝 NUL 字节和解码异常。存储 key 后缀使用经过白名单确认的原扩展名，不使用用户路径。

- [ ] **Step 4: 分类型处理**

PDF 延续 PDFBox 分页与切块；文本类型直接读取 UTF-8，写入 `contentText`，并按换行切块，页码统一为 1。移除现有 `DocumentProcessor` 中上传后自动调用 `ai.summarize(...)` 的逻辑，解析成功只写页数、文本 chunks 和 `READY`；所有格式均由用户在阅读器中主动触发摘要。解析线程池拒绝新任务时将资料立即标记为 `FAILED` 并写入可读错误，不允许永久停留 `PROCESSING`。

- [ ] **Step 5: 提供授权内容读取**

PDF 使用后端代理流，设置 `Content-Type: application/pdf`, `Content-Disposition: inline`, `X-Content-Type-Options: nosniff`；文本只返回数据库中已验证的 UTF-8 内容。所有接口先走现有文档归属检查。

- [ ] **Step 6: 扩展端到端测试并提交**

测试 Markdown 上传后状态为 READY、内容接口返回原文；测试 `.exe` 和伪造 PDF 被拒绝；测试 FAILED 文档可重试；测试线程池拒绝和删除影响计数。

Run: `mvn test`

Commit: `feat: support markdown text and code documents`

---

### Task 4: 阅读进度与文档内搜索

**Files:**
- Create: `backend/src/main/java/com/smartdoc/reader/ReadingProgressRecord.java`
- Create: `backend/src/main/java/com/smartdoc/reader/mapper/ReadingProgressMapper.java`
- Create: `backend/src/main/java/com/smartdoc/reader/ReaderService.java`
- Create: `backend/src/main/java/com/smartdoc/reader/ReaderController.java`
- Create: `backend/src/main/java/com/smartdoc/search/SearchService.java`
- Create: `backend/src/main/java/com/smartdoc/search/SearchController.java`
- Create: `backend/src/test/java/com/smartdoc/reader/ReaderServiceTest.java`
- Create: `backend/src/test/java/com/smartdoc/search/SearchServiceTest.java`

**Interfaces:**
- `PUT /api/documents/{id}/progress` body `{pageNumber, scrollRatio, zoom}`
- `GET /api/documents/{id}/progress`
- `GET /api/documents/{id}/search?q=&limit=50`
- `GET /api/reader/recent?limit=8`
- `GET /api/search?q=&types=document,note,tag&limit=30`

- [ ] **Step 1: 写进度 upsert 和边界失败测试**

```java
@Test void upsertsProgressAndValidatesBounds() {
    service.save(documentId, new ProgressInput(3, 0.45, 1.2));
    service.save(documentId, new ProgressInput(4, 0.10, 1.0));
    assertEquals(4, service.get(documentId).getPageNumber());
    assertThrows(InvalidDocumentException.class,
        () -> service.save(documentId, new ProgressInput(0, 1.2, 8.0)));
}
```

- [ ] **Step 2: 运行 RED 并实现进度模型**

`reading_progress` fields: `id`, `documentId` unique, `pageNumber`, `scrollRatio`, `zoom`, `updatedAt`。约束：page ≥ 1、scrollRatio 0–1、zoom 0.5–3.0。

- [ ] **Step 3: 实现搜索结果 DTO**

```java
public final class SearchHit {
    private int pageNumber;
    private int chunkIndex;
    private String snippet;
    private int matchStart;
    private int matchLength;
}
```

搜索仅查询当前文档 chunks，关键词 trim 后长度 2–100，返回最多 50 条；snippet 截取匹配点前后各 80 字符并进行大小写不敏感匹配。

- [ ] **Step 4: 实现最近阅读**

保存进度时同步更新 `document_record.last_opened_at`。最近阅读按该字段倒序，不包含被删除文档。

- [ ] **Step 5: 实现统一关键词搜索**

搜索文件名、`content_text`、PDF chunks、笔记正文/摘录和标签名，合并为 `UnifiedSearchHit(type, documentId, noteId, pageNumber, title, snippet)`；结果按名称命中、正文命中、更新时间排序并限制 30 条。关键词长度 2–100，使用参数绑定和 HTML 转义前的纯文本 snippet，不在后端插入 `<mark>`。

- [ ] **Step 6: 验证并提交**

Run: `mvn test`

Commit: `feat: save reading progress and search documents`

---

### Task 5: 页码摘录、Markdown 笔记和导出

**Files:**
- Create: `backend/src/main/java/com/smartdoc/note/NoteRecord.java`
- Create: `backend/src/main/java/com/smartdoc/note/NoteTagRecord.java`
- Create: `backend/src/main/java/com/smartdoc/note/mapper/NoteMapper.java`
- Create: `backend/src/main/java/com/smartdoc/note/mapper/NoteTagMapper.java`
- Create: `backend/src/main/java/com/smartdoc/note/NoteService.java`
- Create: `backend/src/main/java/com/smartdoc/note/NoteController.java`
- Create: `backend/src/test/java/com/smartdoc/note/NoteServiceTest.java`
- Modify: `backend/src/main/java/com/smartdoc/document/DocumentService.java`

**Interfaces:**
- `POST /api/documents/{id}/notes`
- `GET /api/documents/{id}/notes`
- `PATCH/DELETE /api/notes/{id}`
- `GET /api/notes?query=&favorite=&tagId=`
- `GET /api/notes/export?documentId=` returns `text/markdown` attachment.

- [ ] **Step 1: 写笔记归属、页码和导出失败测试**

```java
@Test void savesSourceLinkedNoteAndExportsMarkdown() {
    NoteView note = service.create(documentId,
    new NoteInput(7, "GC 原理", "> G1 divides heap into regions", false, List.of(tagId)));
    assertEquals(7, note.getPageNumber());
    assertTrue(service.exportMarkdown(documentId).contains("第 7 页"));
}
```

- [ ] **Step 2: 运行 RED 并实现笔记实体**

`note` fields: `id`, `documentId`, `pageNumber`, `sourceText`, `contentMarkdown`, `favorite`, `createdAt`, `updatedAt`。`note_tag` fields: `id`, `noteId`, `tagId`，唯一约束 `(note_id, tag_id)`。正文最大 20,000 字，摘录最大 5,000 字；PDF 页码不得超过文档页数，文本资料页码固定为 1。

- [ ] **Step 3: 实现 CRUD 与 Markdown 导出**

导出结构固定为文档标题、导出时间、按页码/创建时间排序的笔记。所有用户正文按原文本写入 Markdown，不解释 HTML；前端渲染时单独净化。

- [ ] **Step 4: 扩展文档删除级联**

文档删除事务中先删除笔记标签、笔记、进度、文档标签、AI 缓存和问答，再删除 chunks/document；提交后清理文件。删除前接口返回影响计数并由前端确认；新增集成断言确保这些表均无孤儿数据。

- [ ] **Step 5: 验证并提交**

Run: `mvn test`

Commit: `feat: add source linked markdown notes`

---

### Task 6: DeepSeek 设置、密钥掩码和调用限额

**Files:**
- Create: `backend/src/main/java/com/smartdoc/settings/AiSettings.java`
- Create: `backend/src/main/java/com/smartdoc/settings/AiSettingsService.java`
- Create: `backend/src/main/java/com/smartdoc/settings/AiSettingsController.java`
- Create: `backend/src/main/java/com/smartdoc/settings/SecretStore.java`
- Create: `backend/src/main/java/com/smartdoc/settings/DpapiSecretStore.java`
- Create: `backend/src/main/java/com/smartdoc/settings/UnavailableSecretStore.java`
- Create: `backend/src/main/java/com/smartdoc/ai/AiQuotaService.java`
- Modify: `backend/src/main/java/com/smartdoc/config/AppConfig.java`
- Create: `backend/src/test/java/com/smartdoc/settings/AiSettingsServiceTest.java`
- Create: `backend/src/test/java/com/smartdoc/ai/AiQuotaServiceTest.java`

**Interfaces:**
- `GET /api/settings/ai` returns mode, baseUrl, model, maskedKey, keyConfigured, persistenceAvailable, persistKey, maxOutputTokens, dailyLimit, todayUsed.
- `PUT /api/settings/ai` accepts optional new key; empty key preserves current key.
- `POST /api/settings/ai/test` makes one bounded model request and returns latency/model.
- `DELETE /api/settings/ai/key` removes memory and persisted key.

- [ ] **Step 1: 写掩码与不泄露失败测试**

```java
@Test void neverReturnsFullApiKey() {
    service.update(new AiSettingsInput("DEEPSEEK", base, model, "sk-secret-123456", false, 1024, 20));
    AiSettingsView view = service.get();
    assertEquals("sk-****3456", view.getMaskedKey());
    assertFalse(json.writeValueAsString(view).contains("secret"));
}
```

- [ ] **Step 2: 运行 RED 并实现内存设置**

使用线程安全不可变快照和 `AtomicReference<AiSettings>`。允许模式仅 `DEMO`/`DEEPSEEK`；base URL 必须 HTTPS（localhost 可用 HTTP）；dailyLimit 1–500；maxOutputTokens 128–4096。

- [ ] **Step 3: 实现 SecretStore 能力探测**

Windows 下 `DpapiSecretStore` 通过受控 PowerShell `ProtectedData.Protect/Unprotect` 调用 CurrentUser 范围，输出写入 `data/ai-key.bin`；命令参数不得包含明文 Key，使用标准输入传递。非 Windows 或命令失败时使用 `UnavailableSecretStore`，设置页显示“当前环境不支持安全保存”，并拒绝 `persistKey=true`。

- [ ] **Step 4: 实现每日调用限额**

```java
public void consume(String operation) {
    LocalDate today = LocalDate.now(clock);
    if (used(today) >= settings.dailyLimit()) throw new AiQuotaExceededException();
    increment(today, operation);
}
```

进程内按日期计数；每次真实 AI 调用前消耗一次，连接测试也计数。Demo 不计数。后续若需跨重启统计再迁移数据库，本阶段设置页明确显示“本次运行”。

- [ ] **Step 5: 让 OpenAI 客户端读取运行时设置**

将固定 profile bean 改为 `RoutingAiClient`，每次调用读取 `AiSettingsService.snapshot()`，DEMO 委托 `DemoAiClient`，DEEPSEEK 构建或复用带超时的兼容客户端。Key 缺失返回明确配置错误。

- [ ] **Step 6: 验证日志和响应不含 Key并提交**

Run: `mvn test`

使用测试日志 appender 和 JSON 序列化断言完整 Key 不出现。

Commit: `feat: configure personal DeepSeek access safely`

---

### Task 7: AI 选区动作与结果复用

**Files:**
- Create: `backend/src/main/java/com/smartdoc/ai/AiAction.java`
- Create: `backend/src/main/java/com/smartdoc/ai/AiResultRecord.java`
- Create: `backend/src/main/java/com/smartdoc/ai/mapper/AiResultMapper.java`
- Create: `backend/src/main/java/com/smartdoc/ai/AiActionService.java`
- Create: `backend/src/main/java/com/smartdoc/ai/AiActionController.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/AiClient.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/DemoAiClient.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/OpenAiCompatibleClient.java`
- Modify: `backend/src/main/java/com/smartdoc/chat/QuestionService.java`
- Modify: `backend/src/main/java/com/smartdoc/chat/QuestionController.java`
- Create: `backend/src/test/java/com/smartdoc/ai/AiActionServiceTest.java`

**Interfaces:**
- `POST /api/documents/{id}/ai/actions` body `{action, question, selectedText, pageNumber, force}`。
- Actions: `ASK`, `DOCUMENT_SUMMARY`, `CURRENT_PAGE_SUMMARY`, `EXPLAIN`, `SUMMARIZE`, `EXPLAIN_CODE`, `LINE_BY_LINE`, `COMPLEXITY`, `FIND_ISSUES`, `GENERATE_EXAMPLE`, `INTERVIEW_QUESTION`。
- Response: `{id, action, content, mode, cached, source:{documentId,pageNumber,text}, createdAt}`。

- [ ] **Step 1: 写缓存与强制刷新失败测试**

```java
@Test void reusesSameActionUnlessForced() {
    AiActionResult first = service.execute(doc, input(false));
    AiActionResult second = service.execute(doc, input(false));
    assertEquals(first.getId(), second.getId());
    assertTrue(second.isCached());
    assertNotEquals(first.getId(), service.execute(doc, input(true)).getId());
}
```

- [ ] **Step 2: 运行 RED 并实现缓存键**

缓存键为 SHA-256(`documentId|action|pageNumber|normalizedQuestion|normalizedSelectedText|mode|model`)。选区动作的 `selectedText` 长度 1–12,000；`ASK` 的 `question` 长度 2–500；代码动作只对 CODE 资料或包含典型代码标记的选择开放。`DOCUMENT_SUMMARY` 和 `CURRENT_PAGE_SUMMARY` 复用已有结果，除非 `force=true`。

- [ ] **Step 3: 实现结构化提示词**

每种 `AiAction` 提供固定 system instruction；要求中文回答、不得声称执行代码、明确引用选中文字。`ASK` 只能基于当前文档 chunks 作答，证据不足时明确说明；`LINE_BY_LINE` 输出 Markdown 表格；`COMPLEXITY` 分时间/空间复杂度；`FIND_ISSUES` 按严重性列表。模型失败只记录该 AI 请求的错误，不修改资料、笔记或解析状态。

- [ ] **Step 4: 实现结果持久化和模式标识**

`ai_result` fields: `id`, `documentId`, `action`, `cacheKey`, `sourcePage`, `sourceText`, `contentMarkdown`, `mode`, `model`, `createdAt`；索引 `(document_id, cache_key, created_at)`。

- [ ] **Step 5: 验证并提交**

Run: `mvn test`

Commit: `feat: add reusable AI study actions`

---

### Task 8: 前端基础设施、路由和设计系统

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/src/main.ts`
- Replace: `frontend/src/App.vue`
- Replace: `frontend/src/styles.css`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/layouts/WorkspaceLayout.vue`
- Create: `frontend/src/components/AppSidebar.vue`
- Create: `frontend/src/views/DashboardView.vue`
- Create: `frontend/src/views/LibraryView.vue`
- Create: `frontend/src/views/ReaderView.vue`
- Create: `frontend/src/views/NotesView.vue`
- Create: `frontend/src/views/AiChatView.vue`
- Create: `frontend/src/views/ReviewView.vue`
- Create: `frontend/src/views/SettingsView.vue`
- Create: `frontend/src/test/setup.ts`
- Create: `frontend/src/components/__tests__/AppSidebar.spec.ts`
- Create: `frontend/vitest.config.ts`

**Interfaces:**
- Routes: `/`, `/library`, `/reader/:id`, `/recent`, `/favorites`, `/notes`, `/review`, `/ai`, `/settings`。
- Global CSS tokens exactly reflect approved green palette and light/dark themes.

- [ ] **Step 1: 安装依赖并配置测试**

Add runtime dependencies: `vue-router`, `pinia`, `pdfjs-dist`, `markdown-it`, `dompurify`, `highlight.js`。

Add dev dependencies: `vitest`, `@vue/test-utils`, `jsdom`, `@types/markdown-it`, `@types/dompurify`。

Configure `pnpm test` as `vitest run`.

- [ ] **Step 2: 写导航可访问性失败测试**

```ts
it('exposes labeled navigation and mobile drawer trigger', () => {
  const wrapper = mount(AppSidebar, { global: { plugins: [router] } })
  expect(wrapper.get('nav[aria-label="主导航"]')).toBeTruthy()
  expect(wrapper.get('button[aria-label="打开资料导航"]')).toBeTruthy()
})
```

- [ ] **Step 3: 运行测试确认 RED**

Run: `pnpm test -- AppSidebar`

Expected: module/component not found.

- [ ] **Step 4: 实现应用外壳**

`App.vue` 只渲染 `<router-view />`；`WorkspaceLayout` 包含 skip link、桌面侧栏、移动抽屉、主内容和 AI 模式徽标。导航交互高度 ≥44px，icon-only controls 有 `aria-label` 与 tooltip。`ReviewView` 明确展示第二阶段范围；`AiChatView` 提供当前资料问答入口和历史结果，不伪装跨文档 RAG。

- [ ] **Step 5: 实现设计 tokens 与响应式骨架**

CSS variables define `--color-primary:#173F34`, `--color-accent:#3F8C70`, `--color-bg:#F5F7F6`, `--color-text:#17231F`, `--color-border:#DDE5E1`, `--color-danger:#C24141`，以及暗色对应值。实现 `:focus-visible` 和 `prefers-reduced-motion`。375px 下导航进入抽屉而非隐藏。

- [ ] **Step 6: 验证并提交**

Run: `pnpm test && pnpm build`

Commit: `feat: add accessible study workspace shell`

---

### Task 9: 资料库、工作台与前端状态

**Files:**
- Create: `frontend/src/api/library.ts`
- Create: `frontend/src/api/search.ts`
- Create: `frontend/src/stores/documents.ts`
- Create: `frontend/src/components/GlobalSearch.vue`
- Create: `frontend/src/components/library/FolderTree.vue`
- Create: `frontend/src/components/library/DocumentToolbar.vue`
- Create: `frontend/src/components/library/DocumentList.vue`
- Create: `frontend/src/components/library/UploadQueue.vue`
- Modify: `frontend/src/views/DashboardView.vue`
- Modify: `frontend/src/views/LibraryView.vue`
- Create: `frontend/src/components/library/__tests__/DocumentList.spec.ts`

**Interfaces:**
- Store actions: `loadFolders`, `loadTags`, `loadDocuments`, `uploadFiles`, `organizeDocument`, `previewDelete`, `deleteDocument`。
- URL query mirrors filters so browser back/refresh preserves library state.

- [ ] **Step 1: 写筛选与移动端操作失败测试**

```ts
it('keeps filters and exposes document actions without hover', async () => {
  const wrapper = mount(DocumentList, { props: { documents, view:'list' } })
  expect(wrapper.findAll('[data-document-row]')).toHaveLength(2)
  expect(wrapper.get('[aria-label="收藏 JVM.pdf"]')).toBeTruthy()
  expect(wrapper.get('[aria-label="打开 JVM.pdf"]')).toBeTruthy()
})
```

- [ ] **Step 2: 运行 RED 并实现 store/API**

API 类型与后端 DTO 一致，不使用 `any`。上传队列并发数固定 2，每项显示校验、上传、解析、成功或失败状态；失败项可重试。

- [ ] **Step 3: 实现资料库 UI**

桌面为 folder tree + content，移动端 folder tree 使用 drawer。提供搜索、类型/标签/收藏筛选、排序、列表/卡片切换和批量选择。重命名、移动、删除和收藏均有显式入口；删除前展示资料、笔记、摘录、问答和 AI 结果影响计数。所有动作可通过键盘与显式菜单访问，不依赖 hover。

- [ ] **Step 4: 实现工作台数据**

显示最近阅读、最近笔记、资料数量、收藏数量、快速上传和全局搜索；全局搜索按资料/正文/笔记/标签分组并可跳转页码。复习卡片区域显示“第二阶段开放”，不伪造数据。

- [ ] **Step 5: 验证并提交**

Run: `pnpm test && pnpm build`

Commit: `feat: build document library workspace`

---

### Task 10: 分类型阅读器、搜索、进度和笔记面板

**Files:**
- Create: `frontend/src/api/reader.ts`
- Create: `frontend/src/api/notes.ts`
- Create: `frontend/src/stores/reader.ts`
- Create: `frontend/src/components/reader/PdfReader.vue`
- Create: `frontend/src/components/reader/PdfThumbnailRail.vue`
- Create: `frontend/src/components/reader/MarkdownReader.vue`
- Create: `frontend/src/components/reader/TextReader.vue`
- Create: `frontend/src/components/reader/CodeViewer.vue`
- Create: `frontend/src/components/reader/ReaderToolbar.vue`
- Create: `frontend/src/components/reader/ReaderSearchPanel.vue`
- Create: `frontend/src/components/note/NoteEditor.vue`
- Create: `frontend/src/components/note/NoteList.vue`
- Create: `frontend/src/components/ai/AiPanel.vue`
- Create: `frontend/src/components/ai/SourceReference.vue`
- Modify: `frontend/src/views/ReaderView.vue`
- Create: `frontend/src/components/reader/__tests__/ReaderView.spec.ts`

**Interfaces:**
- `readerStore.open(id)`, `saveProgressDebounced`, `search`, `jumpToPage`, `setSelection`。
- Reader emits `{pageNumber, selectedText}` to Notes and AI panels.

- [ ] **Step 1: 写位置恢复与移动面板失败测试**

```ts
it('restores saved page and keeps notes available on mobile', async () => {
  mockProgress({ pageNumber: 7, zoom: 1.2 })
  const wrapper = mount(ReaderView, readerTestOptions(375))
  await flushPromises()
  expect(wrapper.get('[data-current-page]').text()).toContain('7')
  expect(wrapper.get('button[aria-label="打开笔记面板"]')).toBeTruthy()
})
```

- [ ] **Step 2: 运行 RED 并实现 reader store**

打开资料时并行加载 metadata、content/proxy URL、progress、notes；离开页面前 flush progress。保存进度使用 800ms debounce，页码变化立即保存。

- [ ] **Step 3: 实现 PDF 阅读器**

使用 `pdfjs-dist` worker；页面按可视区惰性渲染，支持缩略图、上一页/下一页、页码输入、缩放、适应宽度、全屏和搜索跳转。每页 canvas 配 text layer 以支持选择文字。对象 URL 在卸载时 revoke。

- [ ] **Step 4: 实现 Markdown、文本和代码阅读器**

Markdown 使用 `markdown-it` 渲染并经 DOMPurify 净化；代码使用 Highlight.js 的显式语言映射，未知语言按纯文本；复制按钮有状态反馈。任何文档内容都不使用未净化的 `v-html`。

- [ ] **Step 5: 实现笔记闭环**

选区操作栏可“摘录/写笔记/AI 解释/AI 总结/复制”。NoteEditor 使用 textarea + Markdown 预览，支持标签和收藏，保存当前页码和选区；NoteList 点击后调用 `jumpToPage`。桌面左栏和右栏可折叠并通过拖拽调整宽度，宽度限制在 240–480px；移动端 Note/AI 通过底部按钮打开抽屉。

- [ ] **Step 6: 实现 AI 面板**

显示自由提问、文档/当前页摘要、快捷动作、Demo/DeepSeek 标识、是否缓存、来源页码和原文；`SourceReference` 点击可跳回页码。未配置真实 Key 时保留 Demo 并提供设置入口。相同问题先展示历史结果，由用户选择强制重新生成；提交期间禁用重复操作。

- [ ] **Step 7: 验证并提交**

Run: `pnpm test && pnpm build`

手动检查 viewport 375/768/1024/1440、Tab 顺序、焦点、减少动画和无横向溢出。

Commit: `feat: add document reader notes and AI actions`

---

### Task 11: 设置页、全部笔记、集成验证与文档

**Files:**
- Create: `frontend/src/api/settings.ts`
- Create: `frontend/src/stores/settings.ts`
- Modify: `frontend/src/views/SettingsView.vue`
- Modify: `frontend/src/views/NotesView.vue`
- Create: `frontend/src/views/__tests__/SettingsView.spec.ts`
- Modify: `README.md`
- Modify: `.env.example`
- Modify: `docs/superpowers/specs/2026-08-12-study-workspace-design.md` only if implementation discovered an approved clarification
- Create: `docs/screenshots/.gitkeep`

**Interfaces:**
- Settings form maps exactly to Task 6 API and never stores API Key in localStorage/Pinia persistence.
- Notes view supports search, favorite filter, document/tag filters and Markdown export.

- [ ] **Step 1: 写 Key 不持久化前端测试**

```ts
it('submits a key without persisting it in browser storage', async () => {
  const wrapper = mount(SettingsView, settingsTestOptions())
  await wrapper.get('input[type="password"]').setValue('sk-secret-value')
  await wrapper.get('button[type="submit"]').trigger('click')
  expect(localStorage.getItem('deepseek-key')).toBeNull()
  expect(wrapper.text()).not.toContain('sk-secret-value')
})
```

- [ ] **Step 2: 实现设置页**

显示模式、base URL、模型、密码输入、掩码、连接测试、最大输出、每日上限、本次运行已用次数和持久保存能力。连接测试显示成功延迟或按未授权/余额/限流/超时分类的错误。

- [ ] **Step 3: 实现全部笔记页**

提供搜索、收藏、资料筛选、按更新时间排序；点击笔记跳到 `/reader/{documentId}?page={pageNumber}&note={id}`；导出下载由后端提供。

- [ ] **Step 4: 更新 README 与启动说明**

README 首屏说明本地优先、支持格式、AI 成本边界、Demo/DeepSeek 配置、截图位置、架构、测试命令和安全策略。`.env.example` 不包含可用密钥。

- [ ] **Step 5: 运行完整验证**

Backend:

```powershell
cd backend
mvn clean test package
```

Frontend:

```powershell
cd frontend
pnpm test
pnpm build
```

Deployment:

```powershell
$env:SMARTDOC_AUTH_SECRET='verification-only-32-byte-secret-value'
docker compose config --quiet
git diff --check
```

Expected: all commands exit 0; production build may report bundle-size warnings but no TypeScript or test errors.

- [ ] **Step 6: 端到端冒烟验证**

使用后端随机端口和前端 dev server 验证：打开工作台、上传 PDF/Markdown/Java、创建文件夹和标签、收藏、恢复页码、搜索、创建页码笔记、切换 Demo、保存临时 DeepSeek 设置（用测试 stub）、执行选区解释、导出笔记和删除文档级联。

- [ ] **Step 7: 代码审查与最终提交**

使用 `requesting-code-review` 审查权限、Key 泄露、删除完整性、文档渲染 XSS、PDF 资源释放、响应式与可访问性；修复 Critical/Important 后重新运行 Step 5。

Commit: `feat: complete SmartDoc study workspace phase one`

## Deferred Plans

下列能力不在本计划实施：

- 第二阶段：知识卡片、题库、错题本、复习日期、全局搜索、多文档问答。
- 第三阶段：PGVector、Embedding、DOCX、OCR、Ollama、流式回答和间隔复习算法。

它们在第一阶段稳定后分别建立独立规格与实施计划。
