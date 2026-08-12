package com.smartdoc.ai;

import com.smartdoc.document.TextChunk;
import java.util.List;

public interface AiClient {
    AiSummary summarize(String text);
    String answer(String question, List<TextChunk> references);
    default String complete(String systemInstruction, String userPrompt) { throw new UnsupportedOperationException("Generic AI completion is unavailable"); }
    default AiMode mode() { return AiMode.DEMO; }
    default String model() { return "demo"; }
}
