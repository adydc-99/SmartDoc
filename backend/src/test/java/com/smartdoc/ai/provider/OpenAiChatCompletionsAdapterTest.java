package com.smartdoc.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiChatCompletionsAdapterTest {
    @Test void postsCompatibleChatRequestAndReadsTheFirstChoice() {
        RestTemplate client = new RestTemplate(new SimpleClientHttpRequestFactory());
        MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
        OpenAiChatCompletionsAdapter adapter = new OpenAiChatCompletionsAdapter(client, new ObjectMapper(), 2 * 1024 * 1024);
        AiProviderConfig config = AiProviderConfig.textProvider(1L, "Qwen", "QWEN", "https://example.cn/v1/", "domestic-model");
        server.expect(once(), requestTo("https://example.cn/v1/chat/completions"))
                .andExpect(header("Authorization", "Bearer key-value"))
                .andExpect(jsonPath("$.model").value("domestic-model"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("question"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"answer\"}}]}", MediaType.APPLICATION_JSON));
        assertEquals("answer", adapter.complete(config, "key-value", new TextCompletionRequest("system", "question", 128)).getContent());
        server.verify();
    }
}
