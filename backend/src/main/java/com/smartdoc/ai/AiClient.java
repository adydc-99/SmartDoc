package com.smartdoc.ai;

import com.smartdoc.document.TextChunk;
import java.util.List;

public interface AiClient {
    AiSummary summarize(String text);
    String answer(String question, List<TextChunk> references);
}
