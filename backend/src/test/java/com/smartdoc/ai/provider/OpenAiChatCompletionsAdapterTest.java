package com.smartdoc.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpStatus;

class OpenAiChatCompletionsAdapterTest {
    @Test void postsExactOpenAiMultimodalMessageShape() {
        RestTemplate client = new RestTemplate(new SimpleClientHttpRequestFactory());
        MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
        OpenAiChatCompletionsAdapter adapter = new OpenAiChatCompletionsAdapter(client, new ObjectMapper(), 2 * 1024 * 1024);
        AiProviderConfig config = AiProviderConfig.textProvider(1L, "Qwen Vision", "QWEN", "https://example.cn/v1", "qwen-vl-max");
        config.setSupportsVision(true);
        server.expect(once(), requestTo("https://example.cn/v1/chat/completions"))
                .andExpect(header("Authorization", "Bearer vision-key"))
                .andExpect(jsonPath("$.model").value("qwen-vl-max"))
                .andExpect(jsonPath("$.temperature").value(0.2))
                .andExpect(jsonPath("$.max_tokens").value(512))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content.length()").value(2))
                .andExpect(jsonPath("$.messages[0].content[0].type").value("text"))
                .andExpect(jsonPath("$.messages[0].content[0].text").value("Describe this page as JSON."))
                .andExpect(jsonPath("$.messages[0].content[1].type").value("image_url"))
                .andExpect(jsonPath("$.messages[0].content[1].image_url.url").value("data:image/png;base64,AQIDBA=="))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"description\\\":\\\"page\\\"}\"}}]}", MediaType.APPLICATION_JSON));
        ProviderResponse response = adapter.vision(config, "vision-key",
                new VisionCompletionRequest("Describe this page as JSON.", new byte[]{1, 2, 3, 4}, "image/png", 512));
        assertEquals("{\"description\":\"page\"}", response.getContent());
        server.verify();
    }

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

    @Test void mapsNotFoundAndRateLimitWithoutProviderBody() {
        RestTemplate client = new RestTemplate(); MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
        OpenAiChatCompletionsAdapter adapter = new OpenAiChatCompletionsAdapter(client, new ObjectMapper(), 1024);
        AiProviderConfig config = AiProviderConfig.textProvider(1L, "Qwen", "QWEN", "https://example.cn/v1", "model");
        server.expect(requestTo("https://example.cn/v1/chat/completions")).andRespond(withStatus(HttpStatus.NOT_FOUND).body("provider secret body"));
        ProviderHttpException missing = assertThrows(ProviderHttpException.class, () -> adapter.complete(config, "key-secret", new TextCompletionRequest("", "q", 128)));
        assertEquals("MODEL_NOT_FOUND", missing.getCode()); assertFalse(missing.getMessage().contains("secret")); server.verify();
    }
    @Test void mapsRateLimitAndStopsOversizedStream() {
        RestTemplate client = new RestTemplate(); MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
        OpenAiChatCompletionsAdapter adapter = new OpenAiChatCompletionsAdapter(client, new ObjectMapper(), 16);
        AiProviderConfig config = AiProviderConfig.textProvider(1L, "Qwen", "QWEN", "https://example.cn/v1", "model");
        server.expect(requestTo("https://example.cn/v1/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("x"));
        assertEquals("PROVIDER_RATE_LIMITED", assertThrows(ProviderHttpException.class, () -> adapter.complete(config, "key-secret", new TextCompletionRequest("", "q", 128))).getCode()); server.verify();
        server.reset(); server.expect(requestTo("https://example.cn/v1/chat/completions")).andRespond(withSuccess("x".repeat(17), MediaType.APPLICATION_JSON));
        assertEquals("RESPONSE_TOO_LARGE", assertThrows(ProviderHttpException.class, () -> adapter.complete(config, "key-secret", new TextCompletionRequest("", "q", 128))).getCode()); server.verify();
    }
    @Test void mapsProviderServerFailureWithoutBodyLeakage() {
        RestTemplate client = new RestTemplate(); MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
        OpenAiChatCompletionsAdapter adapter = new OpenAiChatCompletionsAdapter(client, new ObjectMapper(), 1024);
        AiProviderConfig config = AiProviderConfig.textProvider(1L, "Qwen", "QWEN", "https://example.cn/v1", "model");
        server.expect(requestTo("https://example.cn/v1/chat/completions")).andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("upstream secret"));
        ProviderHttpException error=assertThrows(ProviderHttpException.class,()->adapter.complete(config,"key-secret",new TextCompletionRequest("","q",128)));
        assertEquals("PROVIDER_UNAVAILABLE",error.getCode());assertFalse(error.getMessage().contains("secret"));server.verify();
    }
    @Test void capsOversizedRateLimitBodyBeforeMappingItsStatus() {
        RestTemplate client=new RestTemplate();MockRestServiceServer server=MockRestServiceServer.bindTo(client).build();OpenAiChatCompletionsAdapter adapter=new OpenAiChatCompletionsAdapter(client,new ObjectMapper(),16);AiProviderConfig config=AiProviderConfig.textProvider(1,"Qwen","QWEN","https://example.cn/v1","model");
        server.expect(requestTo("https://example.cn/v1/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("x".repeat(17)));
        assertEquals("RESPONSE_TOO_LARGE",assertThrows(ProviderHttpException.class,()->adapter.complete(config,"key-secret",new TextCompletionRequest("","q",128))).getCode());server.verify();
    }
}
