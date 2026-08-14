package com.smartdoc.ai.vision;

import com.smartdoc.ai.provider.*;
import com.smartdoc.auth.AuthTokenService;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:vision-action-http;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class VisionActionIntegrationTest {
    @Autowired MockMvc mvc; @Autowired AuthTokenService tokens; @Autowired DocumentMapper documents;
    @MockBean ModelRouter router; @MockBean FileStorage storage;

    @Test void multipartEndpointIsOwnerScopedAndPersistsVisionBeforeDeepTextFailure() throws Exception {
        ProviderAdapter adapter=mock(ProviderAdapter.class);
        AiProviderConfig vision=provider(11L,"vision-model",false,true),text=provider(12L,"text-model",true,false);
        when(router.require(7L,ProviderCapability.VISION)).thenReturn(new ProviderSession(vision,"vision-key",adapter));
        when(router.require(7L,ProviderCapability.TEXT)).thenReturn(new ProviderSession(text,"text-key",adapter));
        AiRoutingConfig routing=new AiRoutingConfig();routing.setDailyLimit(10);routing.setMaxOutputTokens(512);when(router.routingFor(7L)).thenReturn(routing);
        when(adapter.vision(any(),anyString(),any())).thenReturn(new ProviderResponse("{\"description\":\"page\",\"ocrText\":\"text\",\"codeOrDiagram\":\"\",\"uncertainties\":[]}"));
        when(adapter.complete(any(),anyString(),any())).thenThrow(new ProviderHttpException("PROVIDER_UNAVAILABLE",502));
        long id=document(7L);String owner="Bearer "+tokens.issue(7L,"owner"),other="Bearer "+tokens.issue(8L,"other");
        byte[] png=png();

        mvc.perform(multipart("/api/documents/{id}/vision-actions",id).file("screenshot",png).param("action","DIRECT").header("Authorization",other))
                .andExpect(status().isNotFound());
        verifyNoInteractions(adapter);

        mvc.perform(multipart("/api/documents/{id}/vision-actions",id).file("screenshot",png).param("action","DEEP_ANALYSIS").param("question","请深入解释").header("Authorization",owner))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ai_vision_cache WHERE user_id=? AND document_id=?",Integer.class,7L,id));
        verify(adapter,times(1)).vision(any(),anyString(),any());
        verify(adapter,times(1)).complete(any(),anyString(),any());
    }

    @Test void directMultipartReturnsStructuredObservationAndMetadata() throws Exception {
        ProviderAdapter adapter=mock(ProviderAdapter.class);AiProviderConfig vision=provider(21L,"qwen-vl",false,true);
        when(router.require(7L,ProviderCapability.VISION)).thenReturn(new ProviderSession(vision,"vision-key",adapter));
        AiRoutingConfig routing=new AiRoutingConfig();routing.setDailyLimit(10);routing.setMaxOutputTokens(256);when(router.routingFor(7L)).thenReturn(routing);
        when(adapter.vision(any(),anyString(),any())).thenReturn(new ProviderResponse("{\"description\":\"diagram\",\"ocrText\":\"hello\",\"codeOrDiagram\":\"A->B\",\"uncertainties\":[]}"));
        long id=document(7L);
        mvc.perform(multipart("/api/documents/{id}/vision-actions",id).file("screenshot",png()).param("action","DIRECT").header("Authorization","Bearer "+tokens.issue(7L,"owner")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.observation.description").value("diagram"))
                .andExpect(jsonPath("$.visionModel").value("qwen-vl")).andExpect(jsonPath("$.textModel").doesNotExist())
                .andExpect(jsonPath("$.visionCacheHit").value(false));
        verify(adapter,times(1)).vision(any(),anyString(),any());verify(adapter,never()).complete(any(),anyString(),any());
    }

    private long document(long owner){LocalDateTime now=LocalDateTime.now();DocumentRecord d=new DocumentRecord();d.setUserId(owner);d.setName("note.txt");d.setSizeBytes(10L);d.setPageCount(1);d.setStorageKey("text");d.setStatus("READY");d.setDocumentType("TXT");d.setContentText("document text");d.setFavorite(false);d.setCreatedAt(now);d.setUpdatedAt(now);documents.insert(d);return d.getId();}
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private static AiProviderConfig provider(long id,String model,boolean text,boolean vision){AiProviderConfig p=AiProviderConfig.textProvider(7L,model,"CUSTOM","https://example.cn/v1",model);p.setId(id);p.setSupportsText(text);p.setSupportsVision(vision);return p;}
    private static byte[] png() throws Exception {java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);return out.toByteArray();}
}
