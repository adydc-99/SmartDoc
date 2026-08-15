# SmartDoc Source Citations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a trial-ready document Q&A experience that returns aligned, expandable source citations and navigates users back to PDF pages or matching non-PDF text.

**Architecture:** Add a deterministic, owner-scoped evidence selector over existing document chunks; the selector produces both bounded model context and the exact sources returned to the client. Preserve the legacy single `source` while adding `sources`, then extract the answer/result presentation into a focused Vue component and keep document-location behavior in `ReaderView`.

**Tech Stack:** Java 11, Spring Boot 2.7, MyBatis Plus, JUnit 5, Mockito, Vue 3 Composition API, TypeScript 5.7, Vitest, Vue Test Utils, existing CSS variables.

## Global Constraints

- ASK returns at most 8 owner-scoped, deduplicated citations and the UI shows 3 initially.
- PDF citations navigate by page; TXT, Markdown, and code citations locate visible text and briefly highlight it.
- Current-page and selected-text actions remain single-source.
- Keep the existing `source` response field and add `sources`; do not add a database table or MySQL migration.
- Context remains bounded by `smartdoc.ai.context-code-points` (default 24,000 Unicode code points).
- Citation excerpts are at most 120 Unicode code points and never render as HTML.
- Use test-first RED/GREEN cycles; do not expose or commit API keys.

---

## File Structure

- Create `backend/src/main/java/com/smartdoc/ai/AiEvidenceSelector.java`: deterministic tokenization, scoring, deduplication, bounded context, and source construction.
- Modify `backend/src/main/java/com/smartdoc/ai/AiActionResponse.java`: extend `Source` and add compatible `sources` getter.
- Modify `backend/src/main/java/com/smartdoc/ai/AiActionService.java`: use one evidence selection for model input and response, including cache hits.
- Modify `backend/src/test/java/com/smartdoc/ai/AiActionServiceTest.java`: selector/service behavior, cache stability, single-source compatibility.
- Modify `backend/src/test/java/com/smartdoc/ai/AiActionIntegrationTest.java`: HTTP serialization and ownership regression.
- Create `frontend/src/components/reader/AiAnswerCard.vue`: answer metadata, safe Markdown, citation cards, expand/collapse, regenerate action.
- Create `frontend/src/components/reader/__tests__/AiAnswerCard.spec.ts`: result presentation, accessibility, and expansion tests.
- Modify `frontend/src/api/study.ts`: additive citation types and optional `sources` compatibility.
- Modify `frontend/src/views/ReaderView.vue`: visible loading/error feedback and citation navigation/highlighting.
- Modify `frontend/src/views/__tests__/ReaderView.spec.ts`: PDF jump, compact drawer close, text highlight, and honest fallback.
- Modify `frontend/src/styles.css`: scoped AI form/result/citation/loading/highlight and responsive styles.
- Modify `README.md`: trial behavior and explicit lightweight-retrieval boundary.

---

### Task 1: Deterministic Evidence Selection and Response Contract

**Files:**
- Create: `backend/src/main/java/com/smartdoc/ai/AiEvidenceSelector.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/AiActionResponse.java`
- Test: `backend/src/test/java/com/smartdoc/ai/AiActionServiceTest.java`

**Interfaces:**
- Consumes: `List<DocumentChunkRecord>`, question, document ID, and context limit.
- Produces: `AiEvidenceSelector.Selection select(long documentId, List<DocumentChunkRecord> chunks, String question, int contextLimit)` with `getContext()` and `getSources()`.
- Produces: `AiActionResponse.Source(long documentId, Integer pageNumber, Integer chunkIndex, String text, String relevance)`.

- [ ] **Step 1: Write failing selector tests**

Add tests that call the wished-for selector directly:

```java
@Test void askEvidenceRanksChineseAndEnglishMatchesDeduplicatesAndCapsAtEight() {
    List<DocumentChunkRecord> rows=new ArrayList<>();
    rows.add(chunk(0,1,"Redis 缓存通过 TTL 控制过期。"));
    rows.add(chunk(1,1,"Redis 缓存通过 TTL 控制过期。"));
    rows.add(chunk(2,2,"Cache eviction uses an LRU policy."));
    for(int i=3;i<12;i++)rows.add(chunk(i,i,"缓存策略证据 "+i));
    AiEvidenceSelector.Selection selected=new AiEvidenceSelector(8,120)
            .select(41L,rows,"Redis 缓存为什么会过期？",24_000);
    assertEquals(8,selected.getSources().size());
    assertEquals(0,selected.getSources().get(0).getChunkIndex());
    assertEquals("HIGH",selected.getSources().get(0).getRelevance());
    assertEquals(1,selected.getSources().stream().filter(s->s.getText().contains("TTL")).count());
    assertTrue(selected.getContext().contains("TTL"));
}

@Test void evidenceOnlyReturnsSourcesActuallyIncludedByTheUnicodeContextLimit() {
    AiEvidenceSelector.Selection selected=new AiEvidenceSelector(8,120)
            .select(41L,List.of(chunk(0,1,"😀相关证据"),chunk(1,2,"不应发送")),"相关",24);
    assertEquals(1,selected.getSources().size());
    assertFalse(selected.getContext().contains("不应发送"));
}
```

- [ ] **Step 2: Run selector tests and verify RED**

Run:

```powershell
cd backend
mvn -q -Dtest=AiActionServiceTest test
```

Expected: test compilation fails because `AiEvidenceSelector`, `Selection`, expanded `Source`, and `getSources()` do not exist.

- [ ] **Step 3: Implement response additions and minimal selector**

`AiActionResponse` keeps `getSource()` and adds:

```java
private final List<Source> sources;
public List<Source> getSources(){return sources;}
public static class Source {
  private final long documentId;
  private final Integer pageNumber,chunkIndex;
  private final String text,relevance;
  public Source(long documentId,Integer pageNumber,Integer chunkIndex,String text,String relevance){this.documentId=documentId;this.pageNumber=pageNumber;this.chunkIndex=chunkIndex;this.text=text;this.relevance=relevance;}
  public long getDocumentId(){return documentId;}
  public Integer getPageNumber(){return pageNumber;}
  public Integer getChunkIndex(){return chunkIndex;}
  public String getText(){return text;}
  public String getRelevance(){return relevance;}
}
```

`AiEvidenceSelector` is package-private and final. Its constructor is `AiEvidenceSelector(int maximumSources,int excerptLimit)`. Its package-private method is `Selection select(long documentId,List<DocumentChunkRecord> chunks,String question,int contextLimit)`. The nested immutable `Selection` constructor takes `String context,List<AiActionResponse.Source> sources`; `getContext()` returns the bounded prompt context and `getSources()` returns an unmodifiable list.

Implement `tokens(question)` with lowercase English `[a-z0-9]+` terms of at least 2 code points and overlapping two-character grams for contiguous Han text. Remove `the`, `and`, `how`, `what`, `why`, `的`, `了`, `是`, `什么`, `为什么`, and `如何`. Score each candidate as `8` for one normalized full-question occurrence plus `min(3, occurrences) * (2 + tokenCodePoints)` per unique token. Sort by score descending, then null-safe page and chunk index ascending. When any candidate has a positive score, discard zero-score candidates; otherwise fall back to document order. Treat equal normalized text, or containment where the shorter text is at least 80% of the longer text, as duplicate. Use code-point-safe `truncate`. Mark positive candidates at least 60% of the top score as `HIGH`; all others are `RELATED`.

- [ ] **Step 4: Run selector tests and verify GREEN**

Run `mvn -q -Dtest=AiActionServiceTest test` from `backend`.

Expected: selector tests pass and existing service tests compile after constructor call sites are adjusted only as required by the additive `Source` signature.

- [ ] **Step 5: Commit Task 1**

```powershell
git add backend/src/main/java/com/smartdoc/ai/AiEvidenceSelector.java backend/src/main/java/com/smartdoc/ai/AiActionResponse.java backend/src/test/java/com/smartdoc/ai/AiActionServiceTest.java
git commit -m "feat: select bounded document evidence"
```

---

### Task 2: Align Model Context, Cache Hits, and HTTP Sources

**Files:**
- Modify: `backend/src/main/java/com/smartdoc/ai/AiActionService.java`
- Modify: `backend/src/test/java/com/smartdoc/ai/AiActionServiceTest.java`
- Modify: `backend/src/test/java/com/smartdoc/ai/AiActionIntegrationTest.java`

**Interfaces:**
- Consumes: `AiEvidenceSelector.Selection` from Task 1.
- Produces: ASK responses where `source == sources[0]`, cached and fresh requests return the same sources, and prompt context contains only selected evidence.

- [ ] **Step 1: Write failing service and HTTP tests**

Add a service test that verifies the prompt and response share evidence:

```java
@Test void askUsesSameRankedEvidenceForPromptFreshResponseAndCacheHit() {
    Harness h=new Harness("PDF",3,24_000,4_000);
    when(h.chunks.selectOwnedOrdered(7L,41L)).thenReturn(List.of(
      chunk(0,1,"unrelated introduction"),
      chunk(1,2,"Redis TTL expires cached values"),
      chunk(2,3,"TTL cleanup can be lazy")));
    AiActionRequest request=new AiActionRequest(AiAction.ASK,"How does Redis TTL cleanup work?",null,null,false);
    AiActionResponse fresh=h.execute(request),cached=h.execute(request);
    assertEquals(2,fresh.getSources().size());
    assertEquals(fresh.getSources().get(0).getText(),fresh.getSource().getText());
    assertEquals(fresh.getSources().get(0).getChunkIndex(),cached.getSources().get(0).getChunkIndex());
    assertTrue(h.prompt.get().contains("Redis TTL"));
    assertFalse(h.prompt.get().contains("unrelated introduction"));
    verify(h.ai,times(1)).complete(anyString(),anyString());
}
```

Extend the HTTP integration expectation:

```java
.andExpect(jsonPath("$.source.chunkIndex").value(0))
.andExpect(jsonPath("$.sources.length()").value(1))
.andExpect(jsonPath("$.sources[0].relevance").value("HIGH"));
```

- [ ] **Step 2: Run service/integration tests and verify RED**

Run:

```powershell
cd backend
mvn -q "-Dtest=AiActionServiceTest,AiActionIntegrationTest" test
```

Expected: assertions fail because ASK still uses document order/aggregated source and cached responses do not rebuild citations.

- [ ] **Step 3: Integrate the selector minimally**

In `execute`:

```java
List<DocumentChunkRecord> ordered=chunks.selectOwnedOrdered(userId,documentId);
AiEvidenceSelector.Selection evidence=action==AiAction.ASK
    ? evidenceSelector.select(documentId,ordered,question,contextLimit)
    : null;
if(!request.isForce()){
  AiResultRecord cached=results.selectLatestByCacheKey(documentId,key);
  if(cached!=null)return response(cached,true,sourcesFor(action,cached,evidence,ordered,request));
}
String context=action==AiAction.ASK?evidence.getContext():
    SELECTED_ACTIONS.contains(action)?truncate(selectedOriginal,contextLimit):boundedContext(relevant,contextLimit);
```

For ASK, persist the first citation excerpt/page in the existing `sourceText/sourcePage` columns. For non-ASK actions, build a singleton compatible source with `RELATED`. The response factory must always use the first supplied source as legacy `source` when the list is non-empty.

If the selector yields no source, throw the same safe invalid-document error used for missing text context before calling the model.

- [ ] **Step 4: Run backend focused tests and verify GREEN**

Run `mvn -q "-Dtest=AiActionServiceTest,AiActionIntegrationTest" test`.

Expected: all focused tests pass; cached duplicate request invokes the model once.

- [ ] **Step 5: Commit Task 2**

```powershell
git add backend/src/main/java/com/smartdoc/ai/AiActionService.java backend/src/test/java/com/smartdoc/ai/AiActionServiceTest.java backend/src/test/java/com/smartdoc/ai/AiActionIntegrationTest.java
git commit -m "feat: return aligned answer citations"
```

---

### Task 3: Accessible Answer and Citation Card UI

**Files:**
- Create: `frontend/src/components/reader/AiAnswerCard.vue`
- Create: `frontend/src/components/reader/__tests__/AiAnswerCard.spec.ts`
- Modify: `frontend/src/api/study.ts`
- Modify: `frontend/src/views/ReaderView.vue`
- Modify: `frontend/src/views/__tests__/ReaderView.spec.ts`
- Modify: `frontend/src/styles.css`

**Interfaces:**
- Consumes: `AiResult.sources?: AiSource[]` with fallback to `[source]`.
- Produces: emits `navigate(source: AiSource)` and `regenerate()`.

- [ ] **Step 1: Write failing component tests**

Create tests for safe Markdown, default source count, expansion, semantic controls, and events:

```ts
it('shows three citations initially and expands the remaining sources', async () => {
  const wrapper=mount(AiAnswerCard,{props:{result, busy:false}})
  expect(wrapper.findAll('[data-test="citation-card"]')).toHaveLength(3)
  expect(wrapper.get('[data-test="toggle-citations"]').text()).toContain('另外 2 条')
  await wrapper.get('[data-test="toggle-citations"]').trigger('click')
  expect(wrapper.findAll('[data-test="citation-card"]')).toHaveLength(5)
  expect(wrapper.get('[data-test="citation-card"]').element.tagName).toBe('BUTTON')
})

it('renders sanitized markdown and emits citation navigation', async () => {
  const wrapper=mount(AiAnswerCard,{props:{result:{...result,content:'**答案**<script>alert(1)</script>'},busy:false}})
  expect(wrapper.get('[data-test="answer-content"]').html()).toContain('<strong>答案</strong>')
  expect(wrapper.html()).not.toContain('<script>')
  await wrapper.get('[data-test="citation-card"]').trigger('click')
  expect(wrapper.emitted('navigate')?.[0]).toEqual([result.sources[0]])
})
```

- [ ] **Step 2: Run component tests and verify RED**

Run:

```powershell
cd frontend
pnpm test -- src/components/reader/__tests__/AiAnswerCard.spec.ts
```

Expected: FAIL because the component and `AiSource` type do not exist.

- [ ] **Step 3: Implement types and answer component**

Add:

```ts
export interface AiSource {documentId:number;pageNumber:number|null;chunkIndex:number|null;text:string;relevance:'HIGH'|'RELATED'}
export interface AiResult {id:number;action:AiAction;content:string;mode:string;cached:boolean;createdAt:string;source:AiSource;sources?:AiSource[]}
```

`AiAnswerCard.vue` computes `allSources = result.sources?.length ? result.sources : [result.source]`, displays 3 unless expanded, uses `renderDocument('markdown', result.content)`, and emits typed events. Citation text remains escaped interpolation, never `v-html`.

- [ ] **Step 4: Integrate loading/error/result states in ReaderView**

Write a ReaderView test first asserting `role="status"` during a deferred request and `role="alert"` after rejection. Verify RED, then add `aiError`, an accessible loading block, and `AiAnswerCard` integration:

```vue
<p v-if="aiError" class="ai-inline-error" role="alert">{{aiError}}</p>
<div v-if="aiBusy" class="ai-loading" role="status" aria-live="polite">正在检索文档证据并生成回答…</div>
<AiAnswerCard v-if="aiResult" :result="aiResult" :busy="aiBusy" @navigate="jumpCitation" @regenerate="askAi(true)" />
```

Clear the prior result error on every new request and keep the existing global live status message.

- [ ] **Step 5: Add scoped visual styles and verify GREEN**

Use existing variables for `.ai-query-panel`, `.ai-loading`, `.ai-answer-card`, `.citation-list`, `.citation-card`, `.citation-rank`, `.citation-relevance`, and `.ai-inline-error`. Provide visible `:focus-visible`, 44px minimum targets, 150–250ms border/background transitions, no raw component hex colors, and no horizontal overflow at 375px.

Run:

```powershell
pnpm test -- src/components/reader/__tests__/AiAnswerCard.spec.ts src/views/__tests__/ReaderView.spec.ts
```

Expected: all focused frontend tests pass.

- [ ] **Step 6: Commit Task 3**

```powershell
git add frontend/src/api/study.ts frontend/src/components/reader/AiAnswerCard.vue frontend/src/components/reader/__tests__/AiAnswerCard.spec.ts frontend/src/views/ReaderView.vue frontend/src/views/__tests__/ReaderView.spec.ts frontend/src/styles.css
git commit -m "feat: present expandable answer citations"
```

---

### Task 4: Citation Navigation, Trial Documentation, and Full Verification

**Files:**
- Modify: `frontend/src/views/ReaderView.vue`
- Modify: `frontend/src/views/__tests__/ReaderView.spec.ts`
- Modify: `frontend/src/styles.css`
- Modify: `README.md`

**Interfaces:**
- Consumes: `jumpCitation(source: AiSource)` emitted by `AiAnswerCard`.
- Produces: PDF page jumps and non-PDF DOM location/highlight with honest fallback status.

- [ ] **Step 1: Write failing navigation tests**

Add tests that:

```ts
it('jumps a PDF citation page, persists it, and closes the compact drawer', async () => {
  window.matchMedia=vi.fn().mockReturnValue({matches:true}) as never
  readerApi.getReaderDocument.mockResolvedValue({id:7,name:'guide.pdf',documentType:'PDF',pageCount:4,status:'READY'})
  readerApi.getReaderContent.mockResolvedValue(new Blob(['pdf']))
  studyApi.runAi.mockResolvedValue(answerWithSources([{documentId:7,pageNumber:3,chunkIndex:4,text:'第三页证据',relevance:'HIGH'}]))
  const wrapper=await mountOpenAiReader()
  await submitAsk(wrapper,'缓存在哪一页？')
  await wrapper.get('[data-test="citation-card"]').trigger('click')
  expect(readerApi.saveProgress).toHaveBeenCalledWith(7,expect.objectContaining({pageNumber:3}))
  expect(wrapper.get('.reader-ai').classes()).not.toContain('open')
})

it('marks matching text and reports an honest fallback when no excerpt is visible', async () => {
  studyApi.runAi.mockResolvedValueOnce(answerWithSources([{documentId:7,pageNumber:1,chunkIndex:0,text:'Readable content',relevance:'HIGH'}]))
  const wrapper=await mountOpenAiReader();await submitAsk(wrapper,'哪里可读？')
  await wrapper.get('[data-test="citation-card"]').trigger('click')
  expect(wrapper.find('mark[data-source-highlight]').text()).toContain('Readable content')
  studyApi.runAi.mockResolvedValueOnce(answerWithSources([{documentId:7,pageNumber:1,chunkIndex:1,text:'missing excerpt',relevance:'RELATED'}]))
  await submitAsk(wrapper,'缺失来源？');await wrapper.get('[data-test="citation-card"]').trigger('click')
  expect(wrapper.get('[data-test="reader-status"]').text()).toContain('来源附近')
})
```

- [ ] **Step 2: Run ReaderView tests and verify RED**

Run `pnpm test -- src/views/__tests__/ReaderView.spec.ts`.

Expected: FAIL because citation navigation/highlighting is not implemented.

- [ ] **Step 3: Implement PDF and non-PDF navigation**

Add `clearSourceHighlight`, `citationNeedles`, `markVisibleCitation`, and `jumpCitation` in `ReaderView`:

```ts
const jumpCitation=async(source:AiSource)=>{
  if(type.value==='PDF'&&source.pageNumber){changePage(source.pageNumber);closeCompactAi();return}
  await nextTick()
  const marked=markVisibleCitation(source.text)
  status.value=marked?'已定位并高亮引用原文':'已打开来源附近，请根据摘录核对'
  if(marked)window.setTimeout(clearSourceHighlight,2000)
  closeCompactAi()
}
```

Use `TreeWalker` over `.reader-document` text nodes, skip script/style/mark nodes, wrap only the matching substring in a newly created `<mark data-source-highlight>`, call `scrollIntoView({block:'center',behavior:'auto'})`, and restore the original text node on cleanup/unmount. Candidate needles must include a trimmed 48-code-point prefix and meaningful individual lines/tokens so Markdown syntax does not prevent matching.

- [ ] **Step 4: Apply responsive/highlight polish and update README**

Add a source highlight style using existing accent/soft variables and a two-second nonessential transition that the global reduced-motion rule disables. Add README bullets describing up to 8 aligned ASK citations, default 3-card display, PDF/non-PDF navigation, and the deliberate no-vector-DB boundary.

- [ ] **Step 5: Run focused tests then all verification**

Run in order:

```powershell
cd backend
mvn test
cd ../frontend
pnpm test
pnpm build
cd ..
git diff --check
```

Expected: backend and frontend suites pass with zero failures; production build succeeds; diff check has no content errors.

Run a high-confidence changed-file credential scan and `docker compose config --quiet` with only the disposable in-process `SMARTDOC_AUTH_SECRET` placeholder used previously. Do not create `.env` and do not execute any MySQL SQL.

- [ ] **Step 6: UI pre-delivery review**

Read `C:/Users/34845/.codex/skills/ui-ux-pro-max/references/pro-rules.md` and verify semantic buttons, visible focus, 44px targets, contrast tokens, 375/768/1024/1440 layouts, no hover-only behavior, stable Vue keys, and reduced motion. Fix every Critical/High issue with a failing regression test where behavior changes.

- [ ] **Step 7: Commit, push, update Draft PR, and wait for CI**

```powershell
git add README.md frontend/src/views/ReaderView.vue frontend/src/views/__tests__/ReaderView.spec.ts frontend/src/styles.css
git commit -m "feat: navigate answer sources in reader"
git push github codex/showcase-a
```

Update Draft PR #1 with the citation feature, exact test counts, and trial steps. Wait until Backend, Frontend, and Secret Scan checks complete successfully before reporting readiness.
