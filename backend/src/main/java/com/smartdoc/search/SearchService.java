package com.smartdoc.search;

import com.smartdoc.document.InvalidDocumentException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final Set<String> ALLOWED_TYPES = Set.of("document", "note", "tag");
    private final JdbcTemplate jdbc;

    public SearchService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<UnifiedSearchHit> search(long userId, String query, String requestedTypes, Integer requestedLimit) {
        String needle = requireQuery(query);
        Set<String> types = parseTypes(requestedTypes);
        int limit = requestedLimit == null ? 30 : requestedLimit;
        if (limit < 1 || limit > 30) throw new InvalidDocumentException("搜索数量必须在 1 到 30 之间");
        List<Candidate> candidates = new ArrayList<>();
        if (types.contains("document")) candidates.addAll(documents(userId, needle));
        if (types.contains("note")) candidates.addAll(notes(userId, needle));
        if (types.contains("tag")) candidates.addAll(tags(userId, needle));
        candidates.sort(Comparator.comparingInt((Candidate value) -> value.rank)
                .thenComparing((Candidate value) -> value.updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(value -> value.hit.getType()).thenComparing(value -> value.hit.getDocumentId())
                .thenComparing(value -> value.hit.getNoteId(), Comparator.nullsLast(Comparator.naturalOrder())));
        return candidates.stream().limit(limit).map(value -> value.hit).collect(Collectors.toList());
    }

    private List<Candidate> documents(long userId, String needle) {
        List<DocumentRow> rows = jdbc.query("SELECT id,name,content_text,updated_at FROM document_record WHERE user_id=?", this::documentRow, userId);
        Map<Long, Candidate> matches = new LinkedHashMap<>();
        for (DocumentRow row : rows) {
            int titleMatch = indexOf(row.name, needle);
            int bodyMatch = indexOf(row.content, needle);
            if (titleMatch >= 0) matches.put(row.id, candidate("DOCUMENT", row.id, null, null, row.name, snippet(row.name, titleMatch, needle.length()), 0, row.updatedAt));
            else if (bodyMatch >= 0) matches.put(row.id, candidate("DOCUMENT", row.id, null, pageForText(row), row.name, snippet(row.content, bodyMatch, needle.length()), 1, row.updatedAt));
        }
        List<ChunkRow> chunks = jdbc.query("SELECT c.document_id,c.page_number,c.chunk_index,c.content,d.name,d.updated_at " +
                "FROM document_chunk c JOIN document_record d ON d.id=c.document_id WHERE d.user_id=? ORDER BY c.page_number,c.chunk_index", this::chunkRow, userId);
        for (ChunkRow row : chunks) {
            if (matches.containsKey(row.documentId)) continue;
            int match = indexOf(row.content, needle);
            if (match >= 0) matches.put(row.documentId, candidate("DOCUMENT", row.documentId, null, row.pageNumber, row.name,
                    snippet(row.content, match, needle.length()), 1, row.updatedAt));
        }
        return new ArrayList<>(matches.values());
    }

    private List<Candidate> notes(long userId, String needle) {
        return jdbc.query("SELECT n.id,n.document_id,n.page_number,n.source_text,n.content_markdown,n.updated_at,d.name " +
                "FROM note n JOIN document_record d ON d.id=n.document_id WHERE d.user_id=?", (rs, row) -> {
            String source=rs.getString("source_text"), content=rs.getString("content_markdown");
            int match=indexOf(content,needle); String sourceValue=content;
            if(match<0){match=indexOf(source,needle);sourceValue=source;}
            return match<0?null:candidate("NOTE",rs.getLong("document_id"),rs.getLong("id"),(Integer)rs.getObject("page_number"),rs.getString("name"),snippet(sourceValue,match,needle.length()),1,rs.getTimestamp("updated_at").toLocalDateTime());
        }, userId).stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Candidate> tags(long userId, String needle) {
        List<Candidate> rows=jdbc.query("SELECT DISTINCT t.id,t.name,d.id document_id,NULL note_id,d.updated_at FROM tag t JOIN document_tag dt ON dt.tag_id=t.id " +
                "JOIN document_record d ON d.id=dt.document_id WHERE d.user_id=? UNION ALL " +
                "SELECT DISTINCT t.id,t.name,d.id document_id,n.id note_id,n.updated_at FROM tag t JOIN note_tag nt ON nt.tag_id=t.id " +
                "JOIN note n ON n.id=nt.note_id JOIN document_record d ON d.id=n.document_id WHERE d.user_id=? ORDER BY name,document_id,note_id", (rs,row) -> {
            String name=rs.getString("name");int match=indexOf(name,needle);
            Long noteId=(Long)rs.getObject("note_id");
            return match<0?null:candidate("TAG",rs.getLong("document_id"),noteId,null,name,snippet(name,match,needle.length()),0,rs.getTimestamp("updated_at").toLocalDateTime());
        }, userId,userId).stream().filter(Objects::nonNull).collect(Collectors.toList());
        LinkedHashMap<String,Candidate> deduplicated=new LinkedHashMap<>();for(Candidate row:rows){String key=row.hit.getDocumentId()+":"+row.hit.getTitle();Candidate existing=deduplicated.get(key);if(existing==null||row.updatedAt.isAfter(existing.updatedAt))deduplicated.put(key,row);}return new ArrayList<>(deduplicated.values());
    }

    private Set<String> parseTypes(String value) {
        if (value == null || value.trim().isEmpty()) return ALLOWED_TYPES;
        LinkedHashSet<String> result = Arrays.stream(value.split(",", -1)).map(String::trim).map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toCollection(LinkedHashSet::new));
        if (result.isEmpty() || result.stream().anyMatch(type -> !ALLOWED_TYPES.contains(type))) throw new InvalidDocumentException("不支持的搜索类型");
        return result;
    }
    private String requireQuery(String query){String value=query==null?"":query.trim();int length=value.codePointCount(0,value.length());if(length<2||length>100)throw new InvalidDocumentException("搜索关键词长度必须在 2 到 100 之间");return value;}
    private int indexOf(String text,String needle){if(text==null)return -1;for(int index=0;index+needle.length()<=text.length();index++)if(text.regionMatches(true,index,needle,0,needle.length()))return index;return -1;}
    private String snippet(String text,int match,int length){int before=Math.min(80,text.codePointCount(0,match));int start=text.offsetByCodePoints(match,-before);int endOfMatch=match+length;int after=Math.min(80,text.codePointCount(endOfMatch,text.length()));int end=text.offsetByCodePoints(endOfMatch,after);return text.substring(start,end);}
    private Candidate candidate(String type,Long documentId,Long noteId,Integer page,String title,String snippet,int rank,LocalDateTime updated){return new Candidate(new UnifiedSearchHit(type,documentId,noteId,page,title,snippet),rank,updated);}
    private Integer pageForText(DocumentRow row){return row.content==null?null:1;}
    private DocumentRow documentRow(ResultSet rs,int ignored)throws SQLException{return new DocumentRow(rs.getLong("id"),rs.getString("name"),rs.getString("content_text"),rs.getTimestamp("updated_at").toLocalDateTime());}
    private ChunkRow chunkRow(ResultSet rs,int ignored)throws SQLException{return new ChunkRow(rs.getLong("document_id"),rs.getInt("page_number"),rs.getString("content"),rs.getString("name"),rs.getTimestamp("updated_at").toLocalDateTime());}
    private static final class Candidate{final UnifiedSearchHit hit;final int rank;final LocalDateTime updatedAt;Candidate(UnifiedSearchHit hit,int rank,LocalDateTime updatedAt){this.hit=hit;this.rank=rank;this.updatedAt=updatedAt;}}
    private static final class DocumentRow{final long id;final String name,content;final LocalDateTime updatedAt;DocumentRow(long id,String name,String content,LocalDateTime updatedAt){this.id=id;this.name=name;this.content=content;this.updatedAt=updatedAt;}}
    private static final class ChunkRow{final long documentId;final int pageNumber;final String content,name;final LocalDateTime updatedAt;ChunkRow(long id,int page,String content,String name,LocalDateTime updated){documentId=id;pageNumber=page;this.content=content;this.name=name;updatedAt=updated;}}
}
