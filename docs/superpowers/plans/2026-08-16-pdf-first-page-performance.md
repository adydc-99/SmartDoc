# PDF First-Page Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Display the active PDF page before thumbnail rendering and text extraction, while preventing stale async work from overwriting the current page.

**Architecture:** Split PDF.js Canvas rendering from text extraction in the reader utility. `ReaderPdf` awaits only the active-page Canvas for visible readiness, starts text extraction afterward, then renders thumbnails serially with stale-work guards.

**Tech Stack:** Vue 3, TypeScript, PDF.js 5, Vitest, Vue Test Utils.

## Global Constraints

- Keep PDF.js; do not introduce kkFileView or another preview service.
- Preserve page navigation, zoom, thumbnails, text emission, `PDFDocumentProxy.destroy()`, and object URL cleanup.
- Render at most 20 thumbnails.
- Do not modify upload, parsing, storage, backend APIs, database, or SQL.
- Do not allow stale page work to update current text, loading, or error state.

---

### Task 1: Separate Canvas Rendering from Text Extraction

**Files:**
- Create: `frontend/src/reader/__tests__/pdf.spec.ts`
- Modify: `frontend/src/reader/pdf.ts`

**Interfaces:**
- Produces: `renderPdfCanvas(document,pageNumber,canvas,scale): Promise<{width:number;height:number}>`.
- Produces: `extractPdfText(document,pageNumber): Promise<string>`.
- Keeps: `loadPdf(blob): Promise<LoadedPdf>`.

- [ ] **Step 1: Write failing utility tests**

Create a fake `PDFDocumentProxy` whose page exposes independent controlled `render().promise` and `getTextContent()` promises. Assert:

```ts
const pending=renderPdfCanvas(document,1,canvas,1)
expect(page.getTextContent).not.toHaveBeenCalled()
finishCanvas()
await expect(pending).resolves.toMatchObject({width:600,height:800})
```

Then call `extractPdfText(document,1)`, resolve items `[{str:'Hello'},{str:'PDF'}]`, and expect `Hello PDF`.

- [ ] **Step 2: Run utility test and verify RED**

Run from `frontend`:

```powershell
pnpm test -- src/reader/__tests__/pdf.spec.ts
```

Expected: FAIL because `renderPdfCanvas` and `extractPdfText` are not exported.

- [ ] **Step 3: Implement the split functions**

Use `document.getPage(pageNumber)` in both functions. `renderPdfCanvas` obtains the viewport, sizes the canvas, awaits only `page.render(...).promise`, and returns dimensions. `extractPdfText` awaits only `getTextContent()` and joins `str` items with spaces. Remove `renderPdfPage` after all call sites move in Task 2.

- [ ] **Step 4: Verify utility GREEN**

Run the focused utility test and expect both behaviors to pass.

---

### Task 2: Prioritize Active Page and Guard Stale Work

**Files:**
- Modify: `frontend/src/components/reader/ReaderPdf.vue`
- Modify: `frontend/src/components/reader/__tests__/ReaderPdf.spec.ts`
- Modify: `frontend/src/reader/pdf.ts`

**Interfaces:**
- Consumes: `renderPdfCanvas` and `extractPdfText` from Task 1.
- Emits unchanged events: `pages`, `page`, and `text`.

- [ ] **Step 1: Write failing component scheduling tests**

Mock both new utility functions. Add tests proving:

```ts
expect(renderPdfCanvas.mock.calls[0].slice(1)).toEqual([1,expect.anything(),1])
expect(renderPdfCanvas.mock.calls.some(call=>call[3]===.18)).toBe(false)
```

before the controlled active-page Canvas resolves; after it resolves, expect the first thumbnail call. Keep text extraction unresolved and assert the visible `正在渲染` status has already disappeared.

Add a rapid-page test: leave page 1 text unresolved, set prop `page=2`, resolve page 2 text first and page 1 last, then assert the last emitted text remains page 2.

- [ ] **Step 2: Run component tests and verify RED**

Run:

```powershell
pnpm test -- src/components/reader/__tests__/ReaderPdf.spec.ts
```

Expected: FAIL because current mounting renders thumbnails before the active page and blocks the active page on text extraction.

- [ ] **Step 3: Implement active-page-first scheduling**

In `ReaderPdf.vue`:

- replace `renderPdfPage` with `renderPdfCanvas` and `extractPdfText`;
- increment `renderRequest` for each page/zoom draw;
- await active Canvas, then clear `busy` only if its request is current;
- start text extraction without holding `busy`, and emit/update only if request is current;
- start a serial thumbnail loop only after the first active Canvas resolves;
- check `disposed` and the loaded PDF identity between thumbnail iterations;
- catch current active-page errors into a safe `renderError` message;
- on unmount set `disposed=true`, increment the request id, destroy the PDF, and revoke the URL.

- [ ] **Step 4: Verify component GREEN**

Run ReaderPdf utility/component tests and expect all scheduling, stale guard, and cleanup tests to pass.

- [ ] **Step 5: Run full frontend verification**

Run:

```powershell
pnpm test
pnpm build
```

Expected: all tests pass and Vite production build exits 0.

- [ ] **Step 6: Review and commit**

Run `git diff --check`, inspect the final diff for unbounded concurrency and cleanup regressions, then:

```powershell
git add frontend/src/reader/pdf.ts frontend/src/reader/__tests__/pdf.spec.ts frontend/src/components/reader/ReaderPdf.vue frontend/src/components/reader/__tests__/ReaderPdf.spec.ts docs/superpowers/plans/2026-08-16-pdf-first-page-performance.md
git commit -m "perf: prioritize active PDF page rendering"
```

- [ ] **Step 7: Push and update Draft PR #1**

Push `codex/showcase-a`, update PR verification counts, and wait for Backend, Frontend, and Secret Scan checks to complete successfully.
