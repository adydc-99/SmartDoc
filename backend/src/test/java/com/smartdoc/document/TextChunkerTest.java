package com.smartdoc.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {
    @Test
    void splitsParagraphsAndPreservesPageNumber() {
        TextChunker chunker = new TextChunker(20, 5);
        List<TextChunk> chunks = chunker.split(List.of(new PageText(3, "第一段介绍垃圾回收。\n\n第二段介绍内存模型。")));
        assertEquals(2, chunks.size());
        assertEquals(3, chunks.get(0).getPageNumber());
    }

    @Test
    void splitsLongParagraphWithOverlap() {
        TextChunker chunker = new TextChunker(10, 2);
        List<TextChunk> chunks = chunker.split(List.of(new PageText(1, "0123456789ABCDE")));
        assertEquals("0123456789", chunks.get(0).getContent());
        assertTrue(chunks.get(1).getContent().startsWith("89"));
    }
}
