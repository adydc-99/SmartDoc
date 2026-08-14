package com.smartdoc.ai;

import com.smartdoc.document.TextChunk;
import java.util.List;

public interface AiClient {
    AiSummary summarize(String text);
    String answer(String question, List<TextChunk> references);
    default String complete(String systemInstruction, String userPrompt) { throw new UnsupportedOperationException("Generic AI completion is unavailable"); }
    default AiMode mode() { return AiMode.DEMO; }
    default String model() { return "demo"; }
    default String complete(long userId, String systemInstruction, String userPrompt) { return complete(systemInstruction, userPrompt); }
    default AiMode mode(long userId) { return mode(); }
    default String model(long userId) { return model(); }
    default String providerIdentity(long userId) { return mode(userId).name() + "|legacy|" + model(userId); }
    default AiSummary summarize(long userId, String text) { return summarize(text); }
    default String answer(long userId, String question, List<TextChunk> references) { return answer(question, references); }
}
