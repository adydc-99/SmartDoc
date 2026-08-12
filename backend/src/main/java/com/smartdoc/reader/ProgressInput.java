package com.smartdoc.reader;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.AssertTrue;

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
    @JsonIgnore
    @AssertTrue(message = "阅读进度参数不正确")
    public boolean isFiniteNumbers() {
        return scrollRatio != null && Double.isFinite(scrollRatio) && zoom != null && Double.isFinite(zoom);
    }
}
