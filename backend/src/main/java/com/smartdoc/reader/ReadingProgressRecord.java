package com.smartdoc.reader;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("reading_progress")
public class ReadingProgressRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private Long documentId;
    private Integer pageNumber;
    private Double scrollRatio;
    private Double zoom;
    private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; } public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Integer getPageNumber() { return pageNumber; } public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public Double getScrollRatio() { return scrollRatio; } public void setScrollRatio(Double scrollRatio) { this.scrollRatio = scrollRatio; }
    public Double getZoom() { return zoom; } public void setZoom(Double zoom) { this.zoom = zoom; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
