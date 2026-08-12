package com.smartdoc.ai;

import com.smartdoc.auth.AuthTokenService;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:ai-action-http;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AiActionIntegrationTest {
 @Autowired MockMvc mvc;@Autowired AuthTokenService tokens;@Autowired DocumentMapper documents;@Autowired JdbcTemplate jdbc;@MockBean AiClient ai;
 @Test void authenticatesPersistsAndReusesHttpActionWithoutSecondAiCall()throws Exception{
  long id=document(7L,"READY");jdbc.update("INSERT INTO document_chunk(document_id,chunk_index,page_number,content) VALUES(?,0,1,'缓存正文')",id);
  when(ai.mode()).thenReturn(AiMode.DEMO);when(ai.model()).thenReturn("demo");when(ai.complete(anyString(),anyString())).thenReturn("# 回答");String auth="Bearer "+tokens.issue(7L,"owner");
  String request="{\"action\":\"ASK\",\"question\":\"缓存是什么？\",\"force\":false}";
  mvc.perform(post("/api/documents/{id}/ai/actions",id).header("Authorization",auth).contentType("application/json").content(request))
    .andExpect(status().isOk()).andExpect(jsonPath("$.action").value("ASK")).andExpect(jsonPath("$.cached").value(false)).andExpect(jsonPath("$.source.documentId").value(id)).andExpect(jsonPath("$.content").value("# 回答"));
  mvc.perform(post("/api/documents/{id}/ai/actions",id).header("Authorization",auth).contentType("application/json").content(request))
    .andExpect(status().isOk()).andExpect(jsonPath("$.cached").value(true));
  verify(ai,times(1)).complete(anyString(),anyString());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ai_result WHERE document_id=?",Integer.class,id));
  doThrow(new IllegalStateException("sk-secret-sentinel confidential selected text")).when(ai).complete(anyString(),contains("confidential selected text"));
  String failure=mvc.perform(post("/api/documents/{id}/ai/actions",id).header("Authorization",auth).contentType("application/json").content("{\"action\":\"EXPLAIN\",\"selectedText\":\"confidential selected text\"}"))
    .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").exists()).andReturn().getResponse().getContentAsString();
  org.junit.jupiter.api.Assertions.assertFalse(failure.contains("sk-secret-sentinel"));org.junit.jupiter.api.Assertions.assertFalse(failure.contains("confidential selected text"));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ai_result WHERE document_id=?",Integer.class,id));
  mvc.perform(post("/api/documents/{id}/ai/actions",id).contentType("application/json").content(request)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/documents/{id}/ai/actions",id).header("Authorization","Bearer "+tokens.issue(8L,"other")).contentType("application/json").content(request)).andExpect(status().isNotFound());
 }
 private long document(long owner,String status){LocalDateTime now=LocalDateTime.now();DocumentRecord d=new DocumentRecord();d.setUserId(owner);d.setName("doc.pdf");d.setSizeBytes(1L);d.setPageCount(1);d.setStorageKey("x");d.setStatus(status);d.setDocumentType("PDF");d.setFavorite(false);d.setCreatedAt(now);d.setUpdatedAt(now);documents.insert(d);return d.getId();}
 private static void assertEquals(Object expected,Object actual){org.junit.jupiter.api.Assertions.assertEquals(expected,actual);}
}
