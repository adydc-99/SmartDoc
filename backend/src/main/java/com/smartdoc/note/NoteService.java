package com.smartdoc.note;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdoc.document.*;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.library.*;
import com.smartdoc.library.mapper.TagMapper;
import com.smartdoc.note.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class NoteService {
    private final NoteMapper notes; private final NoteTagMapper noteTags; private final DocumentMapper documents; private final TagMapper tags; private final DocumentAccessPolicy access; private final Clock clock;
    public NoteService(NoteMapper notes,NoteTagMapper noteTags,DocumentMapper documents,TagMapper tags,DocumentAccessPolicy access,Clock clock){this.notes=notes;this.noteTags=noteTags;this.documents=documents;this.tags=tags;this.access=access;this.clock=clock;}

    @Transactional public NoteView create(long userId,long documentId,NoteInput input){
        DocumentRecord document=ownedForUpdate(userId,documentId); validatePage(document,input.getPageNumber());
        String content=content(input.getContentMarkdown()); String source=optional(input.getSourceText(),5000,"Source text"); LinkedHashSet<Long>tagIds=tagIds(input.getTagIds(),false);
        NoteRecord row=new NoteRecord();row.setDocumentId(documentId);row.setPageNumber(input.getPageNumber());row.setSourceText(source);row.setContentMarkdown(content);row.setFavorite(Boolean.TRUE.equals(input.getFavorite()));
        LocalDateTime now=LocalDateTime.now(clock);row.setCreatedAt(now);row.setUpdatedAt(now);notes.insert(row);replaceTags(row.getId(),tagIds);return view(row);
    }
    public List<NoteView> documentNotes(long userId,long documentId){owned(userId,documentId);return views(notes.selectDocumentNotes(userId,documentId));}
    public List<NoteView> search(long userId,String query,Boolean favorite,Long documentId,Long tagId){if(documentId!=null)owned(userId,documentId);String pattern=query==null||query.trim().isEmpty()?null:likePattern(query.trim());return views(notes.searchOwned(userId,pattern,favorite,documentId,tagId));}
    @Transactional public NoteView update(long userId,long id,NotePatch patch){NoteRecord row=ownedNote(userId,id);if(patch.getContentMarkdown()!=null)row.setContentMarkdown(content(patch.getContentMarkdown()));if(patch.getFavorite()!=null)row.setFavorite(patch.getFavorite());if(patch.getTagIds()!=null)replaceTags(id,tagIds(patch.getTagIds(),true));row.setUpdatedAt(LocalDateTime.now(clock));notes.updateById(row);return view(row);}
    @Transactional public void delete(long userId,long id){ownedNote(userId,id);noteTags.delete(new LambdaQueryWrapper<NoteTagRecord>().eq(NoteTagRecord::getNoteId,id));notes.deleteById(id);}
    public byte[] exportMarkdown(long userId,long documentId){DocumentRecord document=owned(userId,documentId);StringBuilder out=new StringBuilder("# ").append(escape(document.getName())).append("\n\n导出时间：").append(clock.instant()).append("\n");for(NoteView note:documentNotes(userId,documentId)){out.append("\n## 第 ").append(note.getPageNumber()).append(" 页\n\n");if(Boolean.TRUE.equals(note.getFavorite()))out.append("⭐ 收藏\n\n");if(note.getSourceText()!=null&&!note.getSourceText().isBlank())out.append("摘录：\n\n").append(escape(note.getSourceText())).append("\n\n");out.append(escape(note.getContentMarkdown())).append("\n");if(!note.getTags().isEmpty()){out.append("\n标签：");for(int i=0;i<note.getTags().size();i++){if(i>0)out.append("、");out.append(escape(note.getTags().get(i).getName()));}out.append("\n");}}return out.toString().getBytes(StandardCharsets.UTF_8);}

    private DocumentRecord owned(long userId,long id){DocumentRecord row=documents.selectById(id);access.requireOwner(row,userId);return row;}
    private DocumentRecord ownedForUpdate(long userId,long id){DocumentRecord row=documents.selectOwnedForUpdate(id,userId);access.requireOwner(row,userId);return row;}
    private NoteRecord ownedNote(long userId,long id){NoteRecord row=notes.selectOwnedForUpdate(userId,id);if(row==null)throw new DocumentNotFoundException();return row;}
    private void validatePage(DocumentRecord document,Integer page){if(page==null||page<1)throw new InvalidDocumentException("Page number must be positive");if(!"PDF".equals(document.getDocumentType())&&page!=1)throw new InvalidDocumentException("Text documents use page 1");if("PDF".equals(document.getDocumentType())&&document.getPageCount()!=null&&page>document.getPageCount())throw new InvalidDocumentException("Page number exceeds document pages");}
    private String content(String value){if(value==null)throw new InvalidDocumentException("Note content is required");String normalized=value.trim();int count=normalized.codePointCount(0,normalized.length());if(count<1||count>20000)throw new InvalidDocumentException("Note content must contain 1-20000 characters");return normalized;}
    private String optional(String value,int max,String label){if(value==null)return null;String normalized=value.trim();if(normalized.codePointCount(0,normalized.length())>max)throw new InvalidDocumentException(label+" is too long");return normalized.isEmpty()?null:normalized;}
    private LinkedHashSet<Long>tagIds(List<Long> ids,boolean allowEmpty){LinkedHashSet<Long> normalized=new LinkedHashSet<>();if(ids!=null)normalized.addAll(ids);if(normalized.contains(null))throw new InvalidDocumentException("Tag does not exist");if(!normalized.isEmpty()&&tags.selectBatchIds(normalized).size()!=normalized.size())throw new InvalidDocumentException("Tag does not exist");return normalized;}
    private void replaceTags(long noteId,Set<Long> ids){noteTags.delete(new LambdaQueryWrapper<NoteTagRecord>().eq(NoteTagRecord::getNoteId,noteId));for(Long tagId:ids){NoteTagRecord link=new NoteTagRecord();link.setNoteId(noteId);link.setTagId(tagId);noteTags.insert(link);}}
    private NoteView view(NoteRecord row){List<NoteTagRecord> links=noteTags.selectList(new LambdaQueryWrapper<NoteTagRecord>().eq(NoteTagRecord::getNoteId,row.getId()));List<TagView> result=new ArrayList<>();if(!links.isEmpty()){List<Long>ids=new ArrayList<>();for(NoteTagRecord link:links)ids.add(link.getTagId());Map<Long,TagRecord>map=new HashMap<>();for(TagRecord tag:tags.selectBatchIds(ids))map.put(tag.getId(),tag);for(Long id:ids)if(map.containsKey(id))result.add(new TagView(map.get(id)));result.sort(Comparator.comparing(TagView::getName,String.CASE_INSENSITIVE_ORDER).thenComparing(TagView::getId));}return new NoteView(row,result);}
    private List<NoteView> views(List<NoteRecord> rows){List<NoteView>result=new ArrayList<>();for(NoteRecord row:rows)result.add(view(row));return result;}
    private static String likePattern(String value){return "%"+value.replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
    private static String escape(String value){if(value==null)return "";return value.replace("\\","\\\\").replace("<","&lt;").replace(">","&gt;").replace("`","\\`").replace("#","\\#").replace("*","\\*").replace("_","\\_").replace("[","\\[").replace("]","\\]").replace("|","\\|");}
}
