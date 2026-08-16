# Library Sort and Upload Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate the default library 400 response so document lists and upload completion refresh correctly, while preserving existing UI/URL sort values.

**Architecture:** Keep hyphenated sort values inside Vue route/store state, normalize them only at the frontend API boundary, and make the backend parser accept the four documented separators. Reuse the shared Axios `messageOf` helper so list and upload errors expose safe backend messages.

**Tech Stack:** Vue 3, TypeScript, Pinia, Axios, Vitest, Spring Boot 2.7, JUnit 5, Mockito.

## Global Constraints

- Do not change upload size, type validation, parsing, database schema, or stored files.
- Do not execute SQL.
- Keep backend sort field/direction allowlists unchanged.
- Do not expose stack traces, paths, tokens, or API keys in frontend errors.

---

### Task 1: Backend Sort Compatibility

**Files:**
- Modify: `backend/src/test/java/com/smartdoc/library/LibraryServiceTest.java`
- Modify: `backend/src/main/java/com/smartdoc/library/LibraryService.java`

**Interfaces:**
- Consumes: `LibraryService.listDocuments(..., String sort, ...)`.
- Produces: parsing support for `updated-desc`, `updated_desc`, `updated:desc`, and `updated,desc` without changing rejection of unknown fields/directions.

- [x] **Step 1: Write the failing compatibility test**

Add a parameterized test that calls the real service parser through `listDocuments`:

```java
@ParameterizedTest
@ValueSource(strings={"updated-desc","updated_desc","updated:desc","updated,desc"})
void acceptsDocumentedSortSeparators(String sort) {
    when(documents.selectList(any())).thenReturn(Collections.emptyList());
    assertEquals(Collections.emptyList(), service.listDocuments(1L,null,null,null,null,sort,null));
    verify(documents).selectList(any());
}
```

- [x] **Step 2: Run the backend test and verify RED**

Run from `backend`:

```powershell
mvn -q -Dtest=LibraryServiceTest test
```

Expected: `updated-desc` fails with `InvalidDocumentException: Unsupported library sort`.

- [x] **Step 3: Implement the minimal parser compatibility**

In `parseSort`, normalize all documented separators to comma before splitting:

```java
String normalized=value==null||value.trim().isEmpty()?"updated,desc":value.trim().toLowerCase(Locale.ROOT)
        .replace(":",",").replace("_",",").replace("-",",");
```

Do not change the existing field and direction allowlists.

- [x] **Step 4: Verify backend GREEN**

Run `mvn -q -Dtest=LibraryServiceTest test` and expect all tests to pass.

- [x] **Step 5: Commit backend compatibility**

```powershell
git add backend/src/main/java/com/smartdoc/library/LibraryService.java backend/src/test/java/com/smartdoc/library/LibraryServiceTest.java
git commit -m "fix: accept library sort separators"
```

---

### Task 2: Frontend API Normalization and Safe Errors

**Files:**
- Create: `frontend/src/api/__tests__/libraryContract.spec.ts`
- Modify: `frontend/src/api/library.ts`
- Modify: `frontend/src/stores/documents.ts`
- Modify: `frontend/src/stores/__tests__/documents.spec.ts`

**Interfaces:**
- Consumes: `LibraryFilters.sort` values such as `updated-desc`.
- Produces: `normalizeLibrarySort(sort?: string): string|undefined`; safe list/upload error strings through `messageOf(error)`.

- [x] **Step 1: Write the failing API-boundary test**

Mock `api.get`, call `listLibraryDocuments`, and assert the original object is unchanged while the request is normalized:

```ts
it('normalizes UI sort values only at the API boundary',async()=>{
  request.get.mockResolvedValue({data:[]})
  const filters={sort:'updated-desc',favorite:true}
  await listLibraryDocuments(filters)
  expect(request.get).toHaveBeenCalledWith('/library/documents',{params:{sort:'updated,desc',favorite:true}})
  expect(filters.sort).toBe('updated-desc')
})
```

- [x] **Step 2: Run the API test and verify RED**

Run from `frontend`:

```powershell
pnpm test -- src/api/__tests__/libraryContract.spec.ts
```

Expected: FAIL because the request still contains `updated-desc`.

- [x] **Step 3: Add minimal API-boundary normalization**

In `library.ts`:

```ts
export const normalizeLibrarySort=(sort?:string)=>sort?.replace(/-([^-]+)$/g,',$1')
export async function listLibraryDocuments(filters:LibraryFilters){
  return (await api.get<LibraryDocument[]>('/library/documents',{params:{...filters,sort:normalizeLibrarySort(filters.sort)}})).data
}
```

- [x] **Step 4: Write failing safe-error tests**

Extend the store tests by mocking the library API rejection with an Axios-shaped 400 response and assert that list and upload item errors equal `Unsupported library sort`, not the generic status string.

- [x] **Step 5: Run store tests and verify RED**

Run `pnpm test -- src/stores/__tests__/documents.spec.ts`.

Expected: FAIL because store catches currently read `error.message`.

- [x] **Step 6: Reuse the shared safe error extractor**

Import `messageOf` from `../api` and replace both store catch branches:

```ts
catch(error){this.error=messageOf(error)}
// upload item catch
catch(error){item.state='failure';item.error=messageOf(error)}
```

- [x] **Step 7: Verify frontend GREEN**

Run the API and store focused tests and expect all to pass.

- [x] **Step 8: Run full verification**

Run:

```powershell
cd backend; mvn -q test
cd ..\frontend; pnpm test
pnpm build
cd ..; git diff --check
```

Expected: zero failures, successful production build, and no whitespace errors.

- [x] **Step 9: Commit, push, and update Draft PR #1**

```powershell
git add frontend/src/api/library.ts frontend/src/api/__tests__/libraryContract.spec.ts frontend/src/stores/documents.ts frontend/src/stores/__tests__/documents.spec.ts docs/superpowers/plans/2026-08-16-library-sort-upload-refresh.md
git commit -m "fix: restore library upload refresh"
git push github codex/showcase-a
```

Update PR #1 with the root cause, verification counts, and instruction not to re-upload files until the list refresh is fixed. Wait for Backend, Frontend, and Secret Scan checks to pass.
