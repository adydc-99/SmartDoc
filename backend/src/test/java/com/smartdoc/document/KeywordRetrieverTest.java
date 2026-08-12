package com.smartdoc.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordRetrieverTest {
    @Test
    void ranksChineseAndEnglishTerms() {
        List<TextChunk> chunks = List.of(
                new TextChunk(1, 1, "Spring Bean 生命周期包括实例化和初始化。"),
                new TextChunk(2, 2, "JVM garbage collection includes G1 and ZGC."));
        List<TextChunk> result = new KeywordRetriever().retrieve("JVM 有哪些 garbage collection？", chunks, 1);
        assertEquals(2, result.get(0).getIndex());
    }

    @Test
    void returnsNoEvidenceWhenNothingMatches() {
        List<TextChunk> chunks = List.of(new TextChunk(1, 1, "Spring Bean lifecycle and dependency injection."));
        assertTrue(new KeywordRetriever().retrieve("量子纠缠是什么", chunks, 3).isEmpty());
    }
}
