package com.smartdoc.library;

public class UpdateFolderRequest {
    private String name;
    private Long parentId;
    private boolean parentIdSupplied;
    public UpdateFolderRequest() {}
    public UpdateFolderRequest(String name, Long parentId, boolean parentIdSupplied) { this.name=name; this.parentId=parentId; this.parentIdSupplied=parentIdSupplied; }
    public String getName() { return name; } public void setName(String name) { this.name=name; }
    public Long getParentId() { return parentId; } public void setParentId(Long parentId) { this.parentId=parentId; this.parentIdSupplied=true; }
    public boolean isParentIdSupplied() { return parentIdSupplied; }
}
