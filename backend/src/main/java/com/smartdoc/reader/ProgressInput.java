package com.smartdoc.reader;

public class ProgressInput {
    private Integer pageNumber;
    private Double scrollRatio;
    private Double zoom;

    public ProgressInput() {}
    public ProgressInput(Integer pageNumber, Double scrollRatio, Double zoom) {
        this.pageNumber = pageNumber;
        this.scrollRatio = scrollRatio;
        this.zoom = zoom;
    }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public Double getScrollRatio() { return scrollRatio; }
    public void setScrollRatio(Double scrollRatio) { this.scrollRatio = scrollRatio; }
    public Double getZoom() { return zoom; }
    public void setZoom(Double zoom) { this.zoom = zoom; }
}
