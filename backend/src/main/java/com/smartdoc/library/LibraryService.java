package com.smartdoc.library;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdoc.document.*;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.library.mapper.DocumentTagMapper;
import com.smartdoc.library.mapper.FolderMapper;
import com.smartdoc.library.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LibraryService {
    private final FolderMapper folders; private final TagMapper tags; private final DocumentTagMapper documentTags; private final DocumentMapper documents; private final DocumentAccessPolicy access;
    public LibraryService(FolderMapper folders, TagMapper tags, DocumentTagMapper documentTags, DocumentMapper documents, DocumentAccessPolicy access) {
        this.folders=folders; this.tags=tags; this.documentTags=documentTags; this.documents=documents; this.access=access;
    }
    public List<FolderNode> folderTree() {
        List<FolderRecord> records=folders.selectList(null);
        Comparator<FolderNode> order=Comparator.comparing((FolderNode n) -> n.getSortOrder()==null?0:n.getSortOrder()).thenComparing(FolderNode::getName,String.CASE_INSENSITIVE_ORDER);
        Map<Long,FolderNode> nodes=new HashMap<>(); for(FolderRecord record:records) nodes.put(record.getId(),new FolderNode(record));
        List<FolderNode> roots=new ArrayList<>();
        for(FolderRecord record:records){ FolderNode node=nodes.get(record.getId()); FolderNode parent=nodes.get(record.getParentId()); if(parent==null) roots.add(node); else parent.getChildren().add(node); }
        roots.sort(order); nodes.values().forEach(node -> node.getChildren().sort(order)); return roots;
    }
    public FolderNode updateFolder(long id, UpdateFolderRequest request) {
        FolderRecord folder=requireFolder(id);
        if(request.getName()!=null) { requireName(request.getName(),"Folder name is required"); folder.setName(request.getName().trim()); }
        if(request.isParentIdSupplied()) {
            Long parentId=request.getParentId();
            if(parentId!=null){ if(parentId==id || isDescendant(parentId,id)) throw new InvalidDocumentException("Folder cannot be moved below itself or its descendant"); requireFolder(parentId); }
            folder.setParentId(parentId);
        }
        folder.setUpdatedAt(LocalDateTime.now()); folders.updateById(folder); return new FolderNode(folder);
    }
    public FolderNode createFolder(CreateFolderRequest request){
        requireName(request.getName(),"Folder name is required"); if(request.getParentId()!=null)requireFolder(request.getParentId());
        FolderRecord record=new FolderRecord(); record.setName(request.getName().trim()); record.setParentId(request.getParentId()); record.setSortOrder(0);
        record.setCreatedAt(LocalDateTime.now()); record.setUpdatedAt(record.getCreatedAt()); folders.insert(record); return new FolderNode(record);
    }
    public void deleteFolder(long id){
        requireFolder(id);
        long childCount=folders.selectCount(new LambdaQueryWrapper<FolderRecord>().eq(FolderRecord::getParentId,id));
        long documentCount=documents.selectCount(new LambdaQueryWrapper<DocumentRecord>().eq(DocumentRecord::getFolderId,id));
        if(childCount>0 || documentCount>0)throw new InvalidDocumentException("Move folder contents before deleting it");
        folders.deleteById(id);
    }
    public List<TagView> listTags(){List<TagRecord> records=tags.selectList(new LambdaQueryWrapper<TagRecord>().orderByAsc(TagRecord::getName)); List<TagView> result=new ArrayList<>();for(TagRecord tag:records)result.add(new TagView(tag));return result;}
    public TagView createTag(CreateTagRequest request){
        requireName(request.getName(),"Tag name is required"); if(request.getColor()==null || !request.getColor().matches("#[0-9A-Fa-f]{6}"))throw new InvalidDocumentException("Tag color must be a six-digit hex color");
        if(tags.selectCount(new LambdaQueryWrapper<TagRecord>().eq(TagRecord::getName,request.getName().trim()))>0)throw new InvalidDocumentException("Tag name already exists");
        TagRecord tag=new TagRecord();tag.setName(request.getName().trim());tag.setColor(request.getColor());tag.setCreatedAt(LocalDateTime.now());tags.insert(tag);return new TagView(tag);
    }
    @Transactional public void deleteTag(long id){if(tags.selectById(id)==null)throw new InvalidDocumentException("Tag does not exist");documentTags.delete(new LambdaQueryWrapper<DocumentTagRecord>().eq(DocumentTagRecord::getTagId,id));tags.deleteById(id);}
    @Transactional public DocumentListItem organize(long userId,long documentId,OrganizationRequest request){
        DocumentRecord document=documents.selectById(documentId);access.requireOwner(document,userId);
        if(request.isFolderIdSupplied()){if(request.getFolderId()!=null)requireFolder(request.getFolderId());document.setFolderId(request.getFolderId());}
        if(request.isFavoriteSupplied()){if(request.getFavorite()==null)throw new InvalidDocumentException("Favorite must be true or false");document.setFavorite(request.getFavorite());}
        if(request.isFolderIdSupplied() || request.isFavoriteSupplied()){document.setUpdatedAt(LocalDateTime.now());documents.updateById(document);}
        if(request.getTagIds()!=null){
            LinkedHashSet<Long> requested=new LinkedHashSet<>(request.getTagIds());
            if(requested.contains(null))throw new InvalidDocumentException("Tag does not exist");
            if(!requested.isEmpty() && tags.selectBatchIds(requested).size()!=requested.size())throw new InvalidDocumentException("Tag does not exist");
            documentTags.delete(new LambdaQueryWrapper<DocumentTagRecord>().eq(DocumentTagRecord::getDocumentId,documentId));
            for(Long tagId:requested){DocumentTagRecord link=new DocumentTagRecord();link.setDocumentId(documentId);link.setTagId(tagId);documentTags.insert(link);}
        }
        return item(document);
    }
    public List<DocumentListItem> listDocuments(long userId,Long folderId,Long tagId,Boolean favorite,String type,String sort,String query){
        SortOrder order=parseSort(sort);
        LambdaQueryWrapper<DocumentRecord> wrapper=new LambdaQueryWrapper<DocumentRecord>().eq(DocumentRecord::getUserId,userId);
        if(folderId!=null)wrapper.eq(DocumentRecord::getFolderId,folderId); if(favorite!=null)wrapper.eq(DocumentRecord::getFavorite,favorite);
        if(type!=null && !type.trim().isEmpty())wrapper.eq(DocumentRecord::getDocumentType,type);
        if(query!=null && !query.trim().isEmpty())wrapper.apply("LOWER(name) LIKE {0}","%"+query.trim().toLowerCase(Locale.ROOT)+"%");
        if(tagId!=null)wrapper.inSql(DocumentRecord::getId,"SELECT document_id FROM document_tag WHERE tag_id = "+tagId);
        applySort(wrapper,order); List<DocumentListItem> result=new ArrayList<>();for(DocumentRecord document:documents.selectList(wrapper))result.add(item(document));return result;
    }
    private DocumentListItem item(DocumentRecord document){
        List<DocumentTagRecord> links=documentTags.selectList(new LambdaQueryWrapper<DocumentTagRecord>().eq(DocumentTagRecord::getDocumentId,document.getId()));
        if(links.isEmpty())return new DocumentListItem(document,Collections.emptyList()); List<Long> ids=new ArrayList<>();for(DocumentTagRecord link:links)ids.add(link.getTagId());
        List<TagView> views=new ArrayList<>();for(TagRecord tag:tags.selectBatchIds(ids))views.add(new TagView(tag));views.sort(Comparator.comparing(TagView::getName,String.CASE_INSENSITIVE_ORDER));return new DocumentListItem(document,views);
    }
    private SortOrder parseSort(String value){
        String normalized=value==null||value.trim().isEmpty()?"updated,desc":value.trim().toLowerCase(Locale.ROOT).replace(":",",").replace("_",",");
        String[] parts=normalized.split(",");if(parts.length!=2 || !(parts[1].equals("asc")||parts[1].equals("desc")))throw new InvalidDocumentException("Unsupported library sort");
        String field=parts[0]; if(field.equals("updatedat"))field="updated"; if(field.equals("createdat"))field="created";
        if(!(field.equals("updated")||field.equals("name")||field.equals("created")))throw new InvalidDocumentException("Unsupported library sort");return new SortOrder(field,parts[1].equals("asc"));
    }
    private void applySort(LambdaQueryWrapper<DocumentRecord> wrapper,SortOrder order){boolean asc=order.asc;if(order.field.equals("name"))wrapper.orderBy(true,asc,DocumentRecord::getName);else if(order.field.equals("created"))wrapper.orderBy(true,asc,DocumentRecord::getCreatedAt);else wrapper.orderBy(true,asc,DocumentRecord::getUpdatedAt);}
    private void requireName(String value,String message){if(value==null||value.trim().isEmpty())throw new InvalidDocumentException(message);}
    private static class SortOrder{final String field;final boolean asc;SortOrder(String field,boolean asc){this.field=field;this.asc=asc;}}
    private boolean isDescendant(long candidate,long ancestor){ FolderRecord current=folders.selectById(candidate); while(current!=null){ if(current.getId()==ancestor)return true; current=current.getParentId()==null?null:folders.selectById(current.getParentId()); } return false; }
    private FolderRecord requireFolder(long id){ FolderRecord record=folders.selectById(id); if(record==null)throw new InvalidDocumentException("Folder does not exist"); return record; }
}
