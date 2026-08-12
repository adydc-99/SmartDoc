package com.smartdoc.reader;

import java.time.LocalDateTime;

public class ProgressView {
    private final int pageNumber;
    private final double scrollRatio;
    private final double zoom;
    private final LocalDateTime updatedAt;

    public ProgressView(int pageNumber, double scrollRatio, double zoom, LocalDateTime updatedAt) {
        this.pageNumber = pageNumber;
        this.scrollRatio = scrollRatio;
        this.zoom = zoom;
        this.updatedAt = updatedAt;
    }
    public int getPageNumber() { return pageNumber; }
    public double getScrollRatio() { return scrollRatio; }
    public double getZoom() { return zoom; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
