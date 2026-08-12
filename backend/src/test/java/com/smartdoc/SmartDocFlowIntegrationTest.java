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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"smartdoc.storage.local-path=./target/test-files", "spring.datasource.url=jdbc:h2:mem:flow;MODE=MySQL;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class SmartDocFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired QuestionMapper questionMapper;

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

        JsonNode detail = null;
        for (int i = 0; i < 30; i++) {
            String body = mvc.perform(get("/api/documents/{id}", id).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            detail = json.readTree(body);
            if (!"PROCESSING".equals(detail.path("status").asText())) break;
            Thread.sleep(100);
        }
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
