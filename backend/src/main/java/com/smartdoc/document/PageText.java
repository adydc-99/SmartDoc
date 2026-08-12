package com.smartdoc.document;

public class PageText {
    private final int pageNumber;
    private final String content;
    public PageText(int pageNumber, String content) { this.pageNumber = pageNumber; this.content = content; }
    public int getPageNumber() { return pageNumber; }
    public String getContent() { return content; }
}
