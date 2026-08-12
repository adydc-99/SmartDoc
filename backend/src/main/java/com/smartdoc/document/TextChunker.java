package com.smartdoc.document;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {
    private final int maxLength;
    private final int overlap;
    public TextChunker(int maxLength, int overlap) {
        if (maxLength < 1 || overlap < 0 || overlap >= maxLength) throw new IllegalArgumentException("切块参数无效");
        this.maxLength = maxLength; this.overlap = overlap;
    }

    public List<TextChunk> split(List<PageText> pages) {
        List<TextChunk> result = new ArrayList<>();
        int index = 1;
        for (PageText page : pages) {
            String[] paragraphs = page.getContent().split("(?:\\r?\\n){2,}");
            for (String raw : paragraphs) {
                String text = raw.trim();
                if (text.isEmpty()) continue;
                int start = 0;
                while (start < text.length()) {
                    int end = Math.min(start + maxLength, text.length());
                    result.add(new TextChunk(index++, page.getPageNumber(), text.substring(start, end)));
                    if (end == text.length()) break;
                    start = end - overlap;
                }
            }
        }
        return result;
    }
}
