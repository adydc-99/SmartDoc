# Task 1 report: study workspace schema and document types

## Implementation summary

- Added `DocumentType` with case-insensitive, `Locale.ROOT` filename classification for the exact supported extension whitelist; PDF is the only non-text type.
- Added `documentType`, `mimeType`, `favorite`, `folderId`, `lastOpenedAt`, and `contentText` to `DocumentRecord`.
- Extended the H2 schema with idempotent document columns plus folder, tag, document tag, reading progress, note, note tag, and AI result tables, including all requested unique constraints and indexes.
- Added a manual-only, one-time MySQL migration with pre/post verification commands. It was not executed.

## RED evidence

Command, run from `backend/`:

```text
mvn -q -Dtest=DocumentTypeTest test
```

Result: exit code 1. The test compiler reported `找不到符号` for `DocumentType` at all references in `DocumentTypeTest`. This is the expected RED state because `DocumentType` did not yet exist.

Exact relevant compiler output observed before implementation:

```text
[ERROR] COMPILATION ERROR :
[ERROR] /C:/Users/34845/smartdoc/backend/src/test/java/com/smartdoc/document/DocumentTypeTest.java:[12,22] 找不到符号
  符号:   变量 DocumentType
  位置: 类 com.smartdoc.document.DocumentTypeTest
[ERROR] /C:/Users/34845/smartdoc/backend/src/test/java/com/smartdoc/document/DocumentTypeTest.java:[13,22] 找不到符号
  符号:   变量 DocumentType
  位置: 类 com.smartdoc.document.DocumentTypeTest
[ERROR] /C:/Users/34845/smartdoc/backend/src/test/java/com/smartdoc/document/DocumentTypeTest.java:[14,22] 找不到符号
  符号:   变量 DocumentType
  位置: 类 com.smartdoc.document.DocumentTypeTest
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.10.1:testCompile (default-testCompile) on project smartdoc-backend: Compilation failure
```

The original command output also reported the same missing `DocumentType` symbol at the corresponding method-call positions `[12,40]`, `[13,45]`, `[14,41]`, and `[15,60]`. This is verbatim evidence retained from the original task run, rather than a recreated failure.

## GREEN and full-suite evidence

Focused GREEN command, run from `backend/`:

```text
mvn -q -Dtest=DocumentTypeTest test
```

Result: exit code 0.

Latest focused run after adding the contract characterization tests:

```text
Command: mvn -q -Dtest=DocumentTypeTest test
Result: exit code 0
```

Full-suite command, run from `backend/`:

```text
mvn test
```

Result: exit code 0; `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`.

Latest full-suite run after adding the contract characterization tests:

```text
Command: mvn test
[INFO] Running com.smartdoc.document.DocumentTypeTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0 s - in com.smartdoc.document.DocumentTypeTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
Result: exit code 0
```

## Files changed

- `backend/src/main/java/com/smartdoc/document/DocumentRecord.java`
- `backend/src/main/java/com/smartdoc/document/DocumentType.java`
- `backend/src/main/resources/schema.sql`
- `backend/src/test/java/com/smartdoc/document/DocumentTypeTest.java`
- `scripts/alter_smartdoc_study_workspace.sql`
- `.superpowers/sdd/task-1-report.md`

## Self-review

- Confirmed the extension whitelist and Chinese exception messages exactly match the task brief.
- Confirmed `Locale.ROOT` is used and `isText()` is false only for `PDF`.
- Confirmed H2 uses `ADD COLUMN IF NOT EXISTS`, `CLOB`, `BOOLEAN`, and `TIMESTAMP` and includes each requested table, constraint, and index.
- Confirmed the MySQL script is explicitly manual/one-time, uses `DATETIME` and `LONGTEXT`, and was not executed.
- Ran `git diff --check`; it returned no whitespace errors.
- Added characterization/contract coverage for every allowed extension, case-insensitive matching, both exact Chinese exception messages, null input, and `isText()`. These tests targeted pre-existing production behavior, so no new RED claim is made.

## Concerns

The MySQL migration is deliberately a one-time, non-idempotent migration as required; run its commented pre-verification commands before executing it manually.
