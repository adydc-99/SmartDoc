# Tasks 10-11 implementation report

## Delivered

- Added typed reader, progress, note, AI action and AI settings API clients aligned with backend controllers.
- Replaced reader/notes/settings/AI placeholders with usable data-loading, filtering, export and settings flows.
- Added the required first ReaderView test; observed RED because the prior placeholder never loaded APIs.
- Converted all workspace views to route-level lazy imports and added Vue/UI vendor chunks.
- Documented H2 start, supported formats, Demo/DeepSeek boundary, key handling, test/build commands, architecture, screenshot placeholder and manual MySQL migrations.
- Included `design-system/smartdoc/MASTER.md`.

## Verification

- Initial RED: `pnpm exec vitest run src/views/__tests__/ReaderView.spec.ts` failed as expected: `getReaderDocument` had zero calls.
- Required full `pnpm test`: timed out after 124 seconds with no test output.
- Required `pnpm build`: timed out after 124 seconds with no build output.

## External blocker and honest limitations

`pnpm add pdfjs-dist markdown-it dompurify highlight.js @types/markdown-it` twice reached the npm registry but timed out downloading `pdfjs-dist` and `@napi-rs/canvas-win32-x64-msvc`. The dependency declarations were added, but the lockfile/node_modules installation did not complete.

Consequently this is a focused, reviewable frontend increment, not a completed acceptance candidate. The current reader restores typed progress and loads plain content, but PDF.js rendering, thumbnails, selectable PDF text, Markdown sanitization/highlighting, selection toolbar, note CRUD inside the reader, AI reader panel, progress debounce/flush, cleanup verification and resizable/mobile dual sidebars remain incomplete. No unsafe `v-html`, real AI call, backend change or database operation was added. The API key is transient and cleared after submit; it is never sent to browser persistence.

## Manual database reminder

No SQL was generated or executed in this task. Existing scripts remain manual: `scripts/alter_smartdoc_study_workspace.sql`, and `scripts/alter_document_record_processing_version.sql` only if the first migration was already applied.

## Follow-up completion after dependency recovery

The external registry blocker was resolved with `pnpm install --no-optional --network-concurrency=1 --fetch-timeout=300000`; the resulting `pnpm-lock.yaml` is committed.

Completed the bounded production reader:

- PDF.js worker, current-page canvas rendering, first-20-page thumbnails, page controls, zoom/fit/fullscreen, selectable extracted current-page text, backend document search, and deterministic PDF destroy/object-URL revoke.
- Markdown-it rendering passed through DOMPurify, plus explicit Highlight.js languages, line numbers and copy.
- Restored progress, 800 ms scroll/zoom debounce, immediate page save, and leave/unmount flush.
- Selection toolbar; note editor/sanitized preview/CRUD/tags/favorite/source/page/jump; responsive drawers and resizable 240–480 px desktop panels.
- Typed AI actions for ask, document/page/selection summaries and selection/code explanation, with duplicate-submit blocking, mode/cache/time/source display, source jumps and explicit forced regeneration.
- Typed AI settings Pinia store with a component-local transient key, immediate input clearing on both success and error, masked state only, DPAPI availability explanation, clear/test/error/quota UI, and a browser-persistence regression test.
- Literal note search, favorite/document/tag filters and Markdown export with URL cleanup.

Verification evidence:

- Focused TDD RED exposed missing render/PDF/progress behavior; focused GREEN finished with 3 files and 5 tests passing.
- `pnpm test`: exit 0; 11 test files and 16 tests passed in 2.44 s.
- Initial `pnpm build` exposed TypeScript-only integration issues (PDF.js subpath declarations, DTO typing, template navigator access). After minimal type-boundary fixes, final `pnpm build`: exit 0; 1,786 modules transformed and production assets emitted in 6.45 s.

Honest remaining concern: the PDF reader intentionally renders one full page at a time and bounds eager thumbnails to the first 20 pages; all pages remain reachable through page controls/search, and this compromise is stated visibly in the reader. The existing global Element Plus vendor chunk is still large (926.95 kB, 299.60 kB gzip) despite route and reader-vendor splitting; replacing global Element Plus registration with per-component imports is a separate optimization.

## Final API contract correction

Final merge review identified and corrected two frontend/backend mismatches without changing the backend:

- Visible AI actions and TypeScript payloads now use the exact `AiAction` enum names: `ASK`, `DOCUMENT_SUMMARY`, `CURRENT_PAGE_SUMMARY`, `EXPLAIN`, `SUMMARIZE`, and `EXPLAIN_CODE`. The shared whitelist also includes all remaining backend actions and is guarded by a contract test.
- Note creation now sends the current page for PDF documents and page `1` for Markdown, text, and code documents, matching `NoteService.validatePage`.

Verification:

- Focused Vitest: 2 files, 6 tests passed.
- Full `pnpm test`: exit 0; 12 files, 19 tests passed in 2.60 s.
- `pnpm build`: exit 0; 1,786 modules transformed and assets emitted in 6.31 s. The previously documented Element Plus chunk-size warning remains unchanged.
