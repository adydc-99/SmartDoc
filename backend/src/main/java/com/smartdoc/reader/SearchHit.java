package com.smartdoc.reader;

public class SearchHit {
    private final int pageNumber;
    private final int chunkIndex;
    private final String snippet;
    /** Java UTF-16 index within snippet; safe for String.substring on the returned text. */
    private final int matchStart;
    /** Java UTF-16 length within snippet. */
    private final int matchLength;
    public SearchHit(int pageNumber, int chunkIndex, String snippet, int matchStart, int matchLength) {
        this.pageNumber=pageNumber;this.chunkIndex=chunkIndex;this.snippet=snippet;this.matchStart=matchStart;this.matchLength=matchLength;
    }
    public int getPageNumber(){return pageNumber;} public int getChunkIndex(){return chunkIndex;}
    public String getSnippet(){return snippet;} public int getMatchStart(){return matchStart;} public int getMatchLength(){return matchLength;}
}
