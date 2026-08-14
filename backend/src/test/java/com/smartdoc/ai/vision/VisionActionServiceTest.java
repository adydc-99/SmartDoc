package com.smartdoc.ai.vision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdoc.ai.DailyAiQuota;
import com.smartdoc.ai.provider.*;
import com.smartdoc.document.*;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VisionActionServiceTest {
    private static final String OBSERVATION = "{\"description\":\"architecture diagram\",\"ocrText\":\"cache\",\"codeOrDiagram\":\"A -> B\",\"uncertainties\":[\"small label\"]}";

    @Test void ownerIsolationAndInvalidPdfPagesFailBeforeStorageRoutingQuotaOrProvider() throws Exception {
        Harness h = new Harness();
        when(h.documents.selectOwned(41L, 8L)).thenReturn(null);
        assertThrows(DocumentNotFoundException.class, () -> h.execute(8L, direct(1), null));
        assertThrows(InvalidDocumentException.class, () -> h.execute(7L, direct(0), null));
        assertThrows(InvalidDocumentException.class, () -> h.execute(7L, direct(3), null));
        verify(h.storage, never()).open(anyString());
        verify(h.router, never()).require(anyLong(), any());
        verifyNoInteractions(h.visionAdapter, h.textAdapter);
        assertEquals(0, h.quota.used(7L));
    }

    @Test void pdfUsesOwnedSourceAndRendersExactlyOneSelectedOneBasedPage() throws Exception {
        Harness h = new Harness();
        when(h.storage.open("owned/source.pdf")).thenReturn(new ByteArrayInputStream(new byte[]{9, 8, 7}));
        when(h.renderer.render(any(InputStream.class), eq(2))).thenReturn(new NormalizedImage(new byte[]{1, 2, 3}, "image/png"));
        VisionActionResponse response = h.execute(7L, direct(2), null);
        assertEquals("architecture diagram", response.getObservation().getDescription());
        verify(h.documents).selectOwned(41L, 7L);
        verify(h.storage).open("owned/source.pdf");
        verify(h.renderer, times(1)).render(any(InputStream.class), eq(2));
        verify(h.visionAdapter, times(1)).vision(eq(h.visionProvider), eq("vision-key"), argThat(r ->
                r.getImageBytes().length == 3 && "image/png".equals(r.getMediaType())));
    }

    @Test void screenshotRejectsMagicMismatchOversizePayloadAndDecodedPixelBombBeforeRoutingOrQuota() throws Exception {
        Harness h = new Harness();
        h.document.setDocumentType("TXT");
        MockMultipartFile fake = new MockMultipartFile("screenshot", "fake.png", "image/png", "not an image".getBytes());
        assertThrows(InvalidDocumentException.class, () -> h.execute(7L, direct(null), fake));

        byte[] oversized = new byte[8 * 1024 * 1024 + 1];
        oversized[0]=(byte)0x89; oversized[1]=0x50; oversized[2]=0x4e; oversized[3]=0x47;
        MockMultipartFile tooLarge = new MockMultipartFile("screenshot", "large.png", "image/png", oversized);
        assertThrows(InvalidDocumentException.class, () -> h.execute(7L, direct(null), tooLarge));

        BufferedImage bomb = new BufferedImage(4_001, 4_000, BufferedImage.TYPE_BYTE_BINARY);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream(); ImageIO.write(bomb, "png", encoded); bomb.flush();
        MockMultipartFile tooManyPixels = new MockMultipartFile("screenshot", "pixels.png", "image/png", encoded.toByteArray());
        assertThrows(InvalidDocumentException.class, () -> h.execute(7L, direct(null), tooManyPixels));

        verify(h.router, never()).require(anyLong(), any());
        verifyNoInteractions(h.visionAdapter, h.textAdapter);
        assertEquals(0, h.quota.used(7L));
    }

    @Test void durableCacheHitSkipsVisionQuotaAndProviderWhileDirectMissCallsVisionOnly() throws Exception {
        Harness cached = new Harness();
        cached.document.setDocumentType("TXT");
        when(cached.cache.selectOwned(eq(7L), anyString(), eq(11L), eq("vision-model"), eq("vision-v1"), any()))
                .thenReturn(cacheRow(OBSERVATION));
        VisionActionResponse hit = cached.execute(7L, direct(null), png());
        assertTrue(hit.isVisionCacheHit());
        assertEquals("vision-model", hit.getVisionModel());
        assertNull(hit.getTextModel());
        assertEquals(0, cached.quota.used(7L));
        verify(cached.visionAdapter, never()).vision(any(), anyString(), any());
        verify(cached.textAdapter, never()).complete(any(), anyString(), any());

        Harness miss = new Harness(); miss.document.setDocumentType("TXT");
        VisionActionResponse fresh = miss.execute(7L, direct(null), png());
        assertFalse(fresh.isVisionCacheHit());
        assertEquals("architecture diagram", fresh.getObservation().getDescription());
        assertNull(fresh.getAnalysis()); assertNull(fresh.getTextModel());
        assertEquals(1, miss.quota.used(7L));
        verify(miss.visionAdapter, times(1)).vision(any(), anyString(), any());
        verify(miss.textAdapter, never()).complete(any(), anyString(), any());
    }

    @Test void deepAnalysisCallsVisionOnceThenTextOnceAndBoundsPromptByUnicodeCodePoints() throws Exception {
        Harness h = new Harness(); h.document.setDocumentType("TXT"); h.document.setContentText("current document text");
        String longQuestion = "问".repeat(500);
        VisionActionResponse result = h.execute(7L, new VisionActionRequest(VisionAction.DEEP_ANALYSIS, longQuestion, null), png());
        assertEquals("deep synthesis", result.getAnalysis());
        assertEquals("vision-model", result.getVisionModel()); assertEquals("text-model", result.getTextModel());
        assertFalse(result.isVisionCacheHit()); assertEquals(2, h.quota.used(7L));
        verify(h.visionAdapter, times(1)).vision(any(), anyString(), any());
        verify(h.textAdapter, times(1)).complete(eq(h.textProvider), eq("text-key"), any());
        String prompt=h.textRequest.get().getPrompt();
        assertTrue(prompt.contains("architecture diagram")); assertTrue(prompt.contains(longQuestion)); assertTrue(prompt.contains("current document text"));
        assertThrows(InvalidDocumentException.class, () -> h.execute(7L,
                new VisionActionRequest(VisionAction.DEEP_ANALYSIS, "问".repeat(501), null), png()));
    }

    @Test void textFailureKeepsSuccessfullyInsertedVisionCache() throws Exception {
        Harness h = new Harness(); h.document.setDocumentType("TXT");
        when(h.textAdapter.complete(any(), anyString(), any())).thenThrow(new ProviderHttpException("PROVIDER_UNAVAILABLE", 502));
        assertThrows(ProviderHttpException.class, () -> h.execute(7L,
                new VisionActionRequest(VisionAction.DEEP_ANALYSIS, "请深入解释", null), png()));
        verify(h.cache, times(1)).insert(argThat(row -> OBSERVATION.equals(row.getObservation())));
        verify(h.visionAdapter, times(1)).vision(any(), anyString(), any());
        verify(h.textAdapter, times(1)).complete(any(), anyString(), any());
    }

    @Test void acceptsOnlyValidStructuredJsonWithAnOptionalSingleOuterFenceAndDoesNotCacheInvalidResponses() throws Exception {
        Harness fenced = new Harness(); fenced.document.setDocumentType("TXT");
        when(fenced.visionAdapter.vision(any(), anyString(), any())).thenReturn(new ProviderResponse("```json\n"+OBSERVATION+"\n```"));
        assertEquals("cache", fenced.execute(7L, direct(null), png()).getObservation().getOcrText());
        verify(fenced.cache).insert(any());

        Harness invalid = new Harness(); invalid.document.setDocumentType("TXT");
        when(invalid.visionAdapter.vision(any(), anyString(), any())).thenReturn(new ProviderResponse("```json\n{}\n``` trailing"));
        ProviderHttpException failure=assertThrows(ProviderHttpException.class, () -> invalid.execute(7L, direct(null), png()));
        assertEquals("INVALID_PROVIDER_RESPONSE", failure.getCode());
        verify(invalid.cache, never()).insert(any());
    }

    @Test void duplicateCacheInsertRaceRereadsAndUsesWinningDatabaseRow() throws Exception {
        Harness h=new Harness();h.document.setDocumentType("TXT");VisionCacheRecord winner=cacheRow(OBSERVATION);
        when(h.cache.selectOwned(eq(7L),anyString(),eq(11L),eq("vision-model"),eq("vision-v1"),any()))
                .thenReturn(null,winner);
        doThrow(new DuplicateKeyException("unique race")).when(h.cache).insert(any());
        VisionActionResponse response=h.execute(7L,direct(null),png());
        assertEquals("architecture diagram",response.getObservation().getDescription());
        verify(h.cache,times(2)).selectOwned(eq(7L),anyString(),eq(11L),eq("vision-model"),eq("vision-v1"),any());
    }

    private static VisionActionRequest direct(Integer page){return new VisionActionRequest(VisionAction.DIRECT, null, page);}
    private static MockMultipartFile png() throws IOException {BufferedImage image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return new MockMultipartFile("screenshot","shot.png","text/plain",out.toByteArray());}
    private static VisionCacheRecord cacheRow(String observation){VisionCacheRecord row=new VisionCacheRecord();row.setId(1L);row.setObservation(observation);row.setExpiresAt(LocalDateTime.now().plusHours(1));return row;}

    private static final class Harness {
        final DocumentMapper documents=mock(DocumentMapper.class); final FileStorage storage=mock(FileStorage.class);
        final VisionCacheMapper cache=mock(VisionCacheMapper.class); final ModelRouter router=mock(ModelRouter.class);
        final ProviderAdapter visionAdapter=mock(ProviderAdapter.class),textAdapter=mock(ProviderAdapter.class);
        final PdfPageImageRenderer renderer=mock(PdfPageImageRenderer.class); final DailyAiQuota quota=new DailyAiQuota(Clock.systemUTC());
        final AiProviderConfig visionProvider=provider(11L,"vision-model",false,true),textProvider=provider(12L,"text-model",true,false);
        final DocumentRecord document=readyDocument(); final AtomicReference<TextCompletionRequest> textRequest=new AtomicReference<>();
        final VisionActionService service;
        Harness(){
            when(documents.selectOwned(41L,7L)).thenReturn(document);
            when(router.require(7L,ProviderCapability.VISION)).thenReturn(new ProviderSession(visionProvider,"vision-key",visionAdapter));
            when(router.require(7L,ProviderCapability.TEXT)).thenReturn(new ProviderSession(textProvider,"text-key",textAdapter));
            AiRoutingConfig routing=new AiRoutingConfig();routing.setDailyLimit(10);routing.setMaxOutputTokens(512);when(router.routingFor(7L)).thenReturn(routing);
            when(visionAdapter.vision(any(),anyString(),any())).thenReturn(new ProviderResponse(OBSERVATION));
            when(textAdapter.complete(any(),anyString(),any())).thenAnswer(call->{textRequest.set(call.getArgument(2));return new ProviderResponse("deep synthesis");});
            when(cache.insert(any())).thenAnswer(call->{((VisionCacheRecord)call.getArgument(0)).setId(9L);return 1;});
            service=new VisionActionService(documents,storage,cache,router,quota,new ObjectMapper(),renderer,Clock.systemUTC());
        }
        VisionActionResponse execute(long user,VisionActionRequest request,MockMultipartFile file)throws Exception{return service.execute(user,41L,request,file);}
        private static DocumentRecord readyDocument(){DocumentRecord d=new DocumentRecord();d.setId(41L);d.setUserId(7L);d.setStatus("READY");d.setDocumentType("PDF");d.setPageCount(2);d.setStorageKey("owned/source.pdf");d.setContentText("document");return d;}
        private static AiProviderConfig provider(long id,String model,boolean text,boolean vision){AiProviderConfig p=AiProviderConfig.textProvider(7L,model,"CUSTOM","https://example.cn/v1",model);p.setId(id);p.setSupportsText(text);p.setSupportsVision(vision);return p;}
    }
}
