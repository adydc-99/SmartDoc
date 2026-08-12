package com.smartdoc.note;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdoc.auth.AuthTokenService;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:note-http;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class NoteControllerIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired AuthTokenService tokens;@Autowired DocumentMapper documents;
 @Test void exposesCrudListAndSafeMarkdownDownload()throws Exception{
  DocumentRecord document=document();String auth="Bearer "+tokens.issue(7L,"owner");
  String body=mvc.perform(post("/api/documents/{id}/notes",document.getId()).header("Authorization",auth).contentType("application/json")
    .content("{\"pageNumber\":1,\"sourceText\":\"source\",\"contentMarkdown\":\"body\",\"favorite\":false,\"tagIds\":[]}"))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.contentMarkdown").value("body")).andReturn().getResponse().getContentAsString();
  long id=json.readTree(body).path("id").asLong();
  mvc.perform(get("/api/documents/{id}/notes",document.getId()).header("Authorization",auth)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
  mvc.perform(patch("/api/notes/{id}",id).header("Authorization",auth).contentType("application/json").content("{\"favorite\":true}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.favorite").value(true));
  mvc.perform(get("/api/notes").param("documentId",document.getId().toString()).param("favorite","true").header("Authorization",auth))
    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
  mvc.perform(get("/api/notes/export").param("documentId",document.getId().toString()).header("Authorization",auth))
    .andExpect(status().isOk()).andExpect(content().contentType("text/markdown;charset=UTF-8"))
    .andExpect(header().string("Content-Disposition","attachment; filename=notes.md")).andExpect(header().string("X-Content-Type-Options","nosniff"));
  mvc.perform(delete("/api/notes/{id}",id).header("Authorization",auth)).andExpect(status().isNoContent());
 }
 private DocumentRecord document(){LocalDateTime now=LocalDateTime.now();DocumentRecord d=new DocumentRecord();d.setUserId(7L);d.setName("evil\"\r\nX: y.pdf");d.setSizeBytes(1L);d.setPageCount(1);d.setStorageKey("x");d.setStatus("READY");d.setDocumentType("PDF");d.setFavorite(false);d.setCreatedAt(now);d.setUpdatedAt(now);documents.insert(d);return d;}
}
