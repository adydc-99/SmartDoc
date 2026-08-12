package com.smartdoc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.smartdoc.chat.mapper.QuestionMapper;
import com.smartdoc.ai.AiClient;
import com.smartdoc.auth.AuthTokenService;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.mapper.DocumentMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"smartdoc.storage.local-path=./target/test-files", "spring.datasource.url=jdbc:h2:mem:flow;MODE=MySQL;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class SmartDocFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired QuestionMapper questionMapper;
    @Autowired DocumentMapper documentMapper;
    @Autowired AuthTokenService tokenService;
    @org.springframework.boot.test.mock.mockito.SpyBean AiClient ai;

    @Test
    void markdownUploadBecomesReadyAndReturnsValidatedContentWithoutAi() throws Exception {
        String token=tokenService.issue(1L,"demo");
        clearInvocations(ai);
        MockMultipartFile markdown=new MockMultipartFile("file","README.MD","text/markdown","# Hello\n\nSmartDoc".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String upload=mvc.perform(multipart("/api/documents").file(markdown).param("folderId","12").header("Authorization","Bearer "+token))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.documentType").value("MARKDOWN"))
                .andExpect(jsonPath("$.folderId").value(12)).andReturn().getResponse().getContentAsString();
        long id=json.readTree(upload).path("id").asLong();
        JsonNode ready=awaitTerminal(id,token);
        assertEquals("READY",ready.path("status").asText(),ready.path("errorMessage").asText());

        mvc.perform(get("/api/documents/{id}/content",id).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("markdown"))
                .andExpect(jsonPath("$.language").value("markdown")).andExpect(jsonPath("$.content").value("# Hello\n\nSmartDoc"));
        verify(ai,never()).summarize(anyString());
    }

    @Test
    void textAndCodeContentUseExplicitLanguageMapping() throws Exception {
        String token=tokenService.issue(1L,"demo");
        assertLanguage(token,"notes.TXT","plain text","text");
        assertLanguage(token,"Demo.JAVA","class Demo {}","java");
        assertLanguage(token,"config.YML","enabled: true","yaml");
        assertLanguage(token,"tool.PS1","Write-Host ok","powershell");
    }

    @Test
    void forgedPdfIsRejectedAndPdfContentHasSecurityHeaders() throws Exception {
        String token=tokenService.issue(1L,"demo");
        mvc.perform(multipart("/api/documents").file(new MockMultipartFile("file","fake.pdf","application/pdf","not pdf".getBytes())).header("Authorization","Bearer "+token))
                .andExpect(status().isBadRequest());
        String upload=mvc.perform(multipart("/api/documents").file(new MockMultipartFile("file","safe.pdf","application/pdf",samplePdf())).header("Authorization","Bearer "+token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(upload).path("id").asLong();assertEquals("READY",awaitTerminal(id,token).path("status").asText());
        mvc.perform(get("/api/documents/{id}/content",id).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type","application/pdf"))
                .andExpect(header().string("Content-Disposition","inline; filename=\"document.pdf\""))
                .andExpect(header().string("X-Content-Type-Options","nosniff"));
    }

    @Test
    void retryDeleteImpactAndContentAreOwnerProtected() throws Exception {
        String owner=tokenService.issue(1L,"owner"),other=tokenService.issue(2L,"other");
        String upload=mvc.perform(multipart("/api/documents").file(new MockMultipartFile("file","retry.md","text/markdown","retry me".getBytes())).header("Authorization","Bearer "+owner))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(upload).path("id").asLong();awaitTerminal(id,owner);
        DocumentRecord document=documentMapper.selectById(id);document.setStatus("FAILED");document.setErrorMessage("forced");documentMapper.updateById(document);
        mvc.perform(get("/api/documents/{id}/content",id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/documents/{id}/retry",id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/documents/{id}/delete-impact",id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/documents/{id}/delete-impact",id).header("Authorization","Bearer "+owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").value(0)).andExpect(jsonPath("$.notes").value(0));
        mvc.perform(post("/api/documents/{id}/retry",id).header("Authorization","Bearer "+owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        assertEquals("READY",awaitTerminal(id,owner).path("status").asText());
    }

    @Test
    void wrongPasswordReturnsUnauthorizedInsteadOfServerError() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"demo\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginUploadParseAndAskFlow() throws Exception {
        String loginBody = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"demo\",\"password\":\"smartdoc123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(loginBody).path("token").asText();

        MockMultipartFile pdf = new MockMultipartFile("file", "jvm.pdf", "application/pdf", samplePdf());
        String uploadBody = mvc.perform(multipart("/api/documents").file(pdf).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(uploadBody).path("id").asLong();
        assertTrue(!json.readTree(uploadBody).has("storageKey"));
        assertTrue(!json.readTree(uploadBody).has("userId"));

        JsonNode detail = awaitTerminal(id, token);
        assertEquals("READY", detail.path("status").asText(), detail.path("errorMessage").asText());

        String answer = mvc.perform(post("/api/documents/{id}/questions", id).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"question\":\"Which collector is described?\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(json.readTree(answer).path("answer").asText().contains("G1"));
        assertEquals(1, json.readTree(answer).path("references").size());

        mvc.perform(delete("/api/documents/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertEquals(0, questionMapper.selectCount(new QueryWrapper<>()));
    }

    private void assertLanguage(String token,String name,String content,String language)throws Exception{
        String upload=mvc.perform(multipart("/api/documents").file(new MockMultipartFile("file",name,"text/plain",content.getBytes(java.nio.charset.StandardCharsets.UTF_8))).header("Authorization","Bearer "+token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();long id=json.readTree(upload).path("id").asLong();
        assertEquals("READY",awaitTerminal(id,token).path("status").asText());
        mvc.perform(get("/api/documents/{id}/content",id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.language").value(language));
    }

    private JsonNode awaitTerminal(long id,String token)throws Exception{
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);JsonNode detail;
        do{String body=mvc.perform(get("/api/documents/{id}",id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();detail=json.readTree(body);if(!"PROCESSING".equals(detail.path("status").asText()))return detail;java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10));}while(System.nanoTime()<deadline);
        throw new AssertionError("document remained PROCESSING after bounded wait");
    }

    private byte[] samplePdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(); document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText(); content.setFont(PDType1Font.HELVETICA, 12); content.newLineAtOffset(50, 700);
                content.showText("JVM garbage collection uses the G1 collector for region based heap management."); content.endText();
            }
            document.save(output); return output.toByteArray();
        }
    }
}
