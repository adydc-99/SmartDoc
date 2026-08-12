# Progress

- 2026-08-12：完成仓库调研、范围收敛、设计与实施计划。
- 2026-08-12：确定独立目录和默认零依赖演示策略。
- 2026-08-12：后端 9 个认证、切块、召回、AI 和上传校验测试通过。
- 2026-08-12：前端首次安装被 pnpm 的 esbuild 构建策略阻止，改为项目级精确放行。
- 2026-08-12：定位 pnpm `allowBuilds` 占位值根因并修复，前端生产构建通过。
- 2026-08-12：完成 MySQL、MinIO、OpenAI 兼容适配、Docker Compose 和 README。
- 2026-08-12：端到端测试贯通登录、PDF 上传、异步解析、摘要、问答和引用。
- 2026-08-12：最终验证：13 个后端测试通过，Maven 打包、前端生产构建、Compose 配置解析和 git diff 检查通过。
- 2026-08-12：根据独立审阅加固密钥、文档级并发锁、删除历史、存储补偿、AI 超时和有界异步线程池；加固后重新完成全量验证。
## 2026-08-12 Task 3 subtask
- Read `.superpowers/sdd/task-3-brief.md`; scope is backend multi-format ingestion/content/retry/rejection/compensation only.
- Tooling error: `rg.exe` failed with `Access is denied` during file discovery. Root cause is environment executable permission, not repository code; switched to native PowerShell enumeration and will not retry the identical command.
- Required RED captured at `.superpowers/sdd/task-3-red-output.txt`: `mvn -q -Dtest=TextDocumentExtractorTest test` failed at test compilation because `TextDocumentExtractor` did not exist.
- Test-run invocation error: PowerShell parsed the comma in `-Dtest=DocumentUploadValidatorTest,LocalFileStorageTest`; corrected by quoting the Maven property argument.
- Boundary fixture error: a zero-filled 5 MiB byte array correctly triggered the NUL rejection. The test fixture was corrected to fill the exact-boundary payload with valid UTF-8 ASCII bytes.
- Compile error in rejection handler: Spring `TaskRejectedException` subclasses `RejectedExecutionException`, so Java forbids both in one multi-catch. Catching the common superclass covers both.
- Integration failure root cause: a test `@MockBean AiClient` returned null from the existing question-answer path, causing a NOT NULL database violation. Switched to `@SpyBean` and verify only that upload parsing never calls `summarize`, preserving real demo answers.
- Task 3 focused suite: 44 selected document/storage/flow tests passed.
- Task 3 full backend suite: `mvn test` -> 63 passed, 0 failures/errors/skips (BUILD SUCCESS, 9.950 s).
- Independent review found post-insert failures were inside the upload compensation catch and retry lacked per-document serialization. Added a failing storage-deletion regression, narrowed compensation to insert only, and guarded retry with `DocumentLockManager`.
- Re-review found `DocumentLockManager` could remove an entry while another handle still owned it. Deterministic `DocumentLockManagerTest` failed, then passed after atomically reference-counting holders/waiters.
- Final Task 3 verification after review fixes: focused 51/51; full backend 66/66 (BUILD SUCCESS, 9.040 s). Independent final re-review approved.
