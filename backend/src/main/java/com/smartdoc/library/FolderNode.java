package com.smartdoc.library;

import java.util.ArrayList;
import java.util.List;

public class FolderNode {
    private final Long id;
    private final Long parentId;
    private final String name;
    private final Integer sortOrder;
    private final List<FolderNode> children = new ArrayList<>();
    public FolderNode(FolderRecord record) {
        this.id = record.getId(); this.parentId = record.getParentId(); this.name = record.getName(); this.sortOrder = record.getSortOrder();
    }
    public Long getId() { return id; } public Long getParentId() { return parentId; }
    public String getName() { return name; } public Integer getSortOrder() { return sortOrder; }
    public List<FolderNode> getChildren() { return children; }
}
