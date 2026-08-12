package com.smartdoc.ai;

import com.smartdoc.document.TextChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DemoAiClientTest {
    @Test
    void producesDeterministicSummaryKeywordsAndGroundedAnswer() {
        DemoAiClient client = new DemoAiClient();
        String text = "JVM 使用垃圾回收器管理堆内存。G1 会将堆划分为多个区域。";
        AiSummary summary = client.summarize(text);
        assertFalse(summary.getSummary().isBlank());
        assertFalse(summary.getKeywords().isEmpty());
        String answer = client.answer("G1 是什么？", List.of(new TextChunk(1, 2, text)));
        assertTrue(answer.contains("G1"));
    }
}
