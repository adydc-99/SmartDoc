package com.smartdoc.ai;

import com.smartdoc.document.DocumentChunkRecord;
import com.smartdoc.document.DocumentLockManager;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.InvalidDocumentException;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.ai.mapper.AiResultMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AiActionServiceTest {
    @Test
    void askEvidenceRanksChineseAndEnglishMatchesDeduplicatesAndCapsAtEight() {
        List<DocumentChunkRecord> rows = new ArrayList<>();
        rows.add(chunk(0, 1, "Redis 缓存通过 TTL 控制过期。"));
        rows.add(chunk(1, 1, "Redis 缓存通过 TTL 控制过期。"));
        rows.add(chunk(2, 2, "Cache eviction uses an LRU policy."));
        for (int i = 3; i < 12; i++) rows.add(chunk(i, i, "缓存策略证据 " + i));

        AiEvidenceSelector.Selection selected = new AiEvidenceSelector(8, 120)
                .select(41L, rows, "Redis 缓存为什么会过期？", 24_000);

        assertEquals(8, selected.getSources().size());
        assertEquals(0, selected.getSources().get(0).getChunkIndex());
        assertEquals("HIGH", selected.getSources().get(0).getRelevance());
        assertEquals(1, selected.getSources().stream().filter(source -> source.getText().contains("TTL")).count());
        assertTrue(selected.getContext().contains("TTL"));
    }

    @Test
    void evidenceOnlyReturnsSourcesActuallyIncludedByTheUnicodeContextLimit() {
        AiEvidenceSelector.Selection selected = new AiEvidenceSelector(8, 120)
                .select(41L, List.of(chunk(0, 1, "😀相关证据"), chunk(1, 2, "不应发送")), "相关", 24);

        assertEquals(1, selected.getSources().size());
        assertFalse(selected.getContext().contains("不应发送"));
    }

    @Test
    void reusesNormalizedCacheWithoutCallingAiAndForceCreatesFreshResult() {
        DocumentMapper documents = mock(DocumentMapper.class);
        DocumentChunkMapper chunks = mock(DocumentChunkMapper.class);
        AiResultMapper results = mock(AiResultMapper.class);
        AiClient ai = mock(AiClient.class);
        DocumentRecord document = readyDocument(41L, 7L, "PDF", 1);
        when(documents.selectOwnedForUpdate(41L, 7L)).thenReturn(document);
        when(chunks.selectOwnedOrdered(7L, 41L)).thenReturn(List.of(chunk(0, 1, "缓存正文")));
        when(ai.mode(anyLong())).thenReturn(AiMode.DEMO);
        when(ai.model(anyLong())).thenReturn("demo-model");
        when(ai.providerIdentity(anyLong())).thenReturn("DEMO|demo|demo-model");
        AtomicInteger calls = new AtomicInteger();
        when(ai.complete(anyString(), anyString())).thenAnswer(invocation -> "回答-" + calls.incrementAndGet());
        when(ai.complete(anyLong(), anyString(), anyString())).thenAnswer(invocation -> "answer-" + calls.incrementAndGet());
        List<AiResultRecord> stored = new ArrayList<>();
        when(results.selectLatestByCacheKey(eq(41L), anyString())).thenAnswer(invocation -> stored.stream()
                .filter(row -> row.getCacheKey().equals(invocation.getArgument(1)))
                .reduce((first, second) -> second).orElse(null));
        when(results.insert(any(AiResultRecord.class))).thenAnswer(invocation -> {
            AiResultRecord row = invocation.getArgument(0);
            row.setId((long) stored.size() + 1);
            stored.add(row);
            return 1;
        });
        when(results.selectById(anyLong())).thenAnswer(invocation -> stored.stream()
                .filter(row -> row.getId().equals(invocation.getArgument(0))).findFirst().orElse(null));
        AiActionService service = new AiActionService(documents, chunks, results, ai, new DocumentLockManager(), 24_000, 4_000);

        AiActionResponse first = service.execute(7L, 41L,
                new AiActionRequest(AiAction.ASK, "  什么是缓存？\r\n", null, null, false));
        AiActionResponse cached = service.execute(7L, 41L,
                new AiActionRequest(AiAction.ASK, "什么是缓存？\n", null, null, false));
        AiActionResponse forced = service.execute(7L, 41L,
                new AiActionRequest(AiAction.ASK, "什么是缓存？\n", null, null, true));

        assertFalse(first.isCached());
        assertTrue(cached.isCached());
        assertEquals(first.getId(), cached.getId());
        assertFalse(forced.isCached());
        assertNotEquals(first.getId(), forced.getId());
        assertEquals(2, calls.get());
        assertEquals(2, stored.size());
    }

    @ParameterizedTest
    @MethodSource("promptContracts")
    void everyActionUsesItsFixedChinesePromptContract(AiAction action, String marker) {
        Harness h=new Harness("CODE",2,24_000,4_000);
        h.execute(new AiActionRequest(action,action==AiAction.ASK?"为什么这样？":null,
                selected(action)?"public int add(int a, int b) { return a + b; }":null,
                action==AiAction.CURRENT_PAGE_SUMMARY?2:null,false));
        assertTrue(h.system.get().contains("证据不足"));
        assertTrue(h.system.get().contains("不得声称已执行代码"));
        assertTrue(h.prompt.get().contains(marker),h.prompt.get());
    }

    @Test
    void validatesUnicodeLengthsSelectionPageAndCodeHeuristicBeforeAiCall() {
        Harness h=new Harness("PDF",2,24_000,4_000);
        assertThrows(InvalidDocumentException.class,()->h.execute(new AiActionRequest(AiAction.ASK,"😀",null,null,false)));
        assertDoesNotThrow(()->h.execute(new AiActionRequest(AiAction.ASK,"😀😀",null,null,false)));
        assertThrows(InvalidDocumentException.class,()->h.execute(new AiActionRequest(AiAction.EXPLAIN,null,"",null,false)));
        assertThrows(InvalidDocumentException.class,()->h.execute(new AiActionRequest(AiAction.SUMMARIZE,null,"x".repeat(12_001),null,false)));
        assertThrows(InvalidDocumentException.class,()->h.execute(new AiActionRequest(AiAction.CURRENT_PAGE_SUMMARY,null,null,3,false)));
        assertThrows(InvalidDocumentException.class,()->h.execute(new AiActionRequest(AiAction.EXPLAIN,null,"valid selection",3,false)));
        assertThrows(InvalidDocumentException.class,()->h.execute(new AiActionRequest(AiAction.EXPLAIN_CODE,null,"ordinary prose",null,false)));
        assertThrows(InvalidDocumentException.class,()->h.execute(new AiActionRequest(AiAction.EXPLAIN_CODE,null,"ordinary prose; still prose",null,false)));
        assertDoesNotThrow(()->h.execute(new AiActionRequest(AiAction.EXPLAIN_CODE,null,"if (ready) { return value; }",null,false)));
        verify(h.ai,times(2)).complete(anyString(),anyString());
    }

    @Test
    void currentPageAndSelectionUseBoundedExactSourceAndContextAtCodePointBoundaries() {
        Harness page=new Harness("PDF",2,12,5);
        AiActionResponse current=page.execute(new AiActionRequest(AiAction.CURRENT_PAGE_SUMMARY,null,null,2,false));
        assertEquals(2,current.getSource().getPageNumber());
        assertEquals("第二页😀",current.getSource().getText());
        assertTrue(page.prompt.get().codePointCount(0,page.prompt.get().length())<200);
        assertFalse(page.prompt.get().contains("第一页内容"));

        Harness selection=new Harness("TEXT",2,8,5);
        AiActionResponse selected=selection.execute(new AiActionRequest(AiAction.EXPLAIN,null,"  A😀BCDE  ",2,false));
        assertEquals("  A😀B",selected.getSource().getText());
        assertTrue(selection.prompt.get().contains("  A😀BCDE"));
    }

    @Test
    void trimsOversizedOutputWithoutSplittingSurrogatePairAndNeverPersistsFailures() {
        Harness h=new Harness("PDF",2,24_000,4_000);
        when(h.ai.complete(anyString(),anyString())).thenReturn("x".repeat(99_999)+"😀tail");
        AiActionResponse response=h.execute(new AiActionRequest(AiAction.DOCUMENT_SUMMARY,null,null,null,false));
        assertEquals(100_000,response.getContent().codePointCount(0,response.getContent().length()));
        assertTrue(response.getContent().endsWith("😀"));

        Harness failed=new Harness("PDF",2,24_000,4_000);
        when(failed.ai.complete(anyLong(),anyString(),anyString())).thenThrow(new IllegalStateException("safe request failure"));
        assertThrows(IllegalStateException.class,()->failed.execute(new AiActionRequest(AiAction.EXPLAIN,null,"confidential selected text",null,false)));
        verify(failed.results,never()).insert(any());
        assertEquals("READY",failed.document.getStatus());
        doReturn("retry ok").when(failed.ai).complete(anyLong(),anyString(),anyString());
        assertEquals("retry ok",failed.execute(new AiActionRequest(AiAction.EXPLAIN,null,"confidential selected text",null,false)).getContent());
    }

    @Test
    void concurrentIdenticalNonForcedRequestsMakeOneCallAndReuseDurableResult() throws Exception {
        Harness h=new Harness("PDF",2,24_000,4_000);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        when(h.ai.complete(anyLong(),anyString(),anyString())).thenAnswer(call->{entered.countDown();assertTrue(release.await(2,TimeUnit.SECONDS));return "answer";});
        ExecutorService pool=Executors.newFixedThreadPool(2);try{
            Callable<AiActionResponse> call=()->h.execute(new AiActionRequest(AiAction.ASK,"并发问题",null,null,false));
            Future<AiActionResponse> first=pool.submit(call);assertTrue(entered.await(2,TimeUnit.SECONDS));Future<AiActionResponse> second=pool.submit(call);
            Thread.sleep(80);release.countDown();AiActionResponse a=first.get(2,TimeUnit.SECONDS),b=second.get(2,TimeUnit.SECONDS);
            assertEquals(a.getId(),b.getId());assertNotEquals(a.isCached(),b.isCached());verify(h.ai,times(1)).complete(anyLong(),anyString(),anyString());
        } finally {pool.shutdownNow();}
    }

    @Test
    void insertMustBeReadableAndModeAndModelParticipateInCacheKey() {
        Harness h=new Harness("PDF",2,24_000,4_000);
        h.execute(new AiActionRequest(AiAction.ASK,"模式问题",null,null,false));
        when(h.ai.mode(anyLong())).thenReturn(AiMode.DEEPSEEK);when(h.ai.model(anyLong())).thenReturn("deepseek-reasoner");when(h.ai.providerIdentity(anyLong())).thenReturn("OPENAI_CHAT_COMPLETIONS|7|deepseek-reasoner");
        AiActionResponse deep=h.execute(new AiActionRequest(AiAction.ASK,"模式问题",null,null,false));
        assertEquals("DEEPSEEK",deep.getMode());verify(h.ai,times(2)).complete(anyLong(),anyString(),anyString());

        Harness missing=new Harness("PDF",2,24_000,4_000);when(missing.results.selectById(anyLong())).thenReturn(null);
        assertThrows(IllegalStateException.class,()->missing.execute(new AiActionRequest(AiAction.DOCUMENT_SUMMARY,null,null,null,false)));
    }

    @Test
    void durableCacheHitBypassesRoutingQuotaAndNetwork() {
        DailyAiQuota quota=new DailyAiQuota(Clock.systemUTC());AiSettingsService settings=new AiSettingsService(AiSettings.defaults(),"",new SecretStore(){public boolean isAvailable(){return false;}public Optional<String> load(){return Optional.empty();}public void save(String secret){throw new UnsupportedOperationException();}public void clear(){}},quota);
        settings.update(new AiSettingsUpdate(AiMode.DEEPSEEK,"https://api.deepseek.com/v1","deepseek-chat","sk-test-quota-sentinel",false,256,2));AtomicInteger network=new AtomicInteger();
        RoutingAiClient routing=new RoutingAiClient(settings,quota,new DemoAiClient(),(snapshot,key)->new NetworkAiClient(){public AiSummary summarize(String text){throw new UnsupportedOperationException();}public String answer(String q,List<com.smartdoc.document.TextChunk> refs){throw new UnsupportedOperationException();}public String complete(String system,String prompt){network.incrementAndGet();return "deep answer";}public void probe(){}});
        DocumentMapper documents=mock(DocumentMapper.class);DocumentChunkMapper chunks=mock(DocumentChunkMapper.class);AiResultMapper results=mock(AiResultMapper.class);DocumentRecord document=readyDocument(51L,7L,"PDF",1);when(documents.selectOwnedForUpdate(51L,7L)).thenReturn(document);when(chunks.selectOwnedOrdered(7L,51L)).thenReturn(List.of(chunk(0,1,"quota context")));List<AiResultRecord> stored=new ArrayList<>();when(results.selectLatestByCacheKey(eq(51L),anyString())).thenAnswer(call->stored.stream().filter(r->r.getCacheKey().equals(call.getArgument(1))).findFirst().orElse(null));when(results.insert(any())).thenAnswer(call->{AiResultRecord row=call.getArgument(0);row.setId(1L);stored.add(row);return 1;});when(results.selectById(1L)).thenAnswer(call->stored.get(0));AiActionService service=new AiActionService(documents,chunks,results,routing,new DocumentLockManager(),100,50);
        service.execute(7L,51L,new AiActionRequest(AiAction.ASK,"quota question",null,null,false));service.execute(7L,51L,new AiActionRequest(AiAction.ASK,"quota question",null,null,false));
        assertEquals(1,network.get());assertEquals(1,quota.used());
    }

    @Test
    void demoCompletionDoesNotSplitUnicodeCodePoints() {
        String result=new DemoAiClient().complete("system","x".repeat(319)+"😀tail");
        assertEquals(320,result.substring("演示模式结果：".length()).codePointCount(0,result.substring("演示模式结果：".length()).length()));
        assertTrue(result.endsWith("😀"));
    }

    static Stream<Arguments> promptContracts(){return Stream.of(
      Arguments.of(AiAction.ASK,"仅根据文档证据回答问题"),Arguments.of(AiAction.DOCUMENT_SUMMARY,"总结整篇文档"),
      Arguments.of(AiAction.CURRENT_PAGE_SUMMARY,"总结当前页"),Arguments.of(AiAction.EXPLAIN,"解释选中内容"),
      Arguments.of(AiAction.SUMMARIZE,"概括选中内容"),Arguments.of(AiAction.EXPLAIN_CODE,"解释代码"),
      Arguments.of(AiAction.LINE_BY_LINE,"Markdown 表格逐行"),Arguments.of(AiAction.COMPLEXITY,"时间复杂度和空间复杂度"),
      Arguments.of(AiAction.FIND_ISSUES,"严重程度列表"),Arguments.of(AiAction.GENERATE_EXAMPLE,"生成使用示例"),
      Arguments.of(AiAction.INTERVIEW_QUESTION,"生成面试题"));}
    private static boolean selected(AiAction a){return a!=AiAction.ASK&&a!=AiAction.DOCUMENT_SUMMARY&&a!=AiAction.CURRENT_PAGE_SUMMARY;}

    private static final class Harness {
      final DocumentMapper documents=mock(DocumentMapper.class);final DocumentChunkMapper chunks=mock(DocumentChunkMapper.class);final AiResultMapper results=mock(AiResultMapper.class);final AiClient ai=mock(AiClient.class);
      final DocumentRecord document;final List<AiResultRecord> stored=new CopyOnWriteArrayList<>();final java.util.concurrent.atomic.AtomicReference<String> system=new java.util.concurrent.atomic.AtomicReference<>(),prompt=new java.util.concurrent.atomic.AtomicReference<>();final AiActionService service;
      Harness(String type,int pages,int context,int source){document=readyDocument(41L,7L,type,pages);when(documents.selectOwnedForUpdate(41L,7L)).thenReturn(document);when(chunks.selectOwnedOrdered(7L,41L)).thenReturn(List.of(chunk(0,1,"第一页内容😀"),chunk(1,2,"第二页😀")));when(ai.mode()).thenReturn(AiMode.DEMO);when(ai.model()).thenReturn("demo-model");when(ai.complete(anyString(),anyString())).thenAnswer(call->{system.set(call.getArgument(0));prompt.set(call.getArgument(1));return "ok";});when(results.selectLatestByCacheKey(eq(41L),anyString())).thenAnswer(call->stored.stream().filter(r->r.getCacheKey().equals(call.getArgument(1))).reduce((a,b)->b).orElse(null));when(results.insert(any())).thenAnswer(call->{AiResultRecord r=call.getArgument(0);r.setId((long)stored.size()+1);stored.add(r);return 1;});when(results.selectById(anyLong())).thenAnswer(call->stored.stream().filter(r->r.getId().equals(call.getArgument(0))).findFirst().orElse(null));service=new AiActionService(documents,chunks,results,ai,new DocumentLockManager(),context,source);}
      AiActionResponse execute(AiActionRequest request){return service.execute(7L,41L,request);}
    }

    private static DocumentRecord readyDocument(long id, long owner, String type, int pages) {
        DocumentRecord document = new DocumentRecord();
        document.setId(id);
        document.setUserId(owner);
        document.setStatus("READY");
        document.setDocumentType(type);
        document.setPageCount(pages);
        return document;
    }

    private static DocumentChunkRecord chunk(int index, int page, String content) {
        DocumentChunkRecord chunk = new DocumentChunkRecord();
        chunk.setChunkIndex(index);
        chunk.setPageNumber(page);
        chunk.setContent(content);
        return chunk;
    }
}
