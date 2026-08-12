package com.smartdoc.document;

public class TextChunk {
    private final int index;
    private final int pageNumber;
    private final String content;
    public TextChunk(int index, int pageNumber, String content) {
        this.index = index; this.pageNumber = pageNumber; this.content = content;
    }
    public int getIndex() { return index; }
    public int getPageNumber() { return pageNumber; }
    public String getContent() { return content; }
}
