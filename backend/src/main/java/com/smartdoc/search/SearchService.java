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
        String pattern = likePattern(needle);
        Set<String> types = parseTypes(requestedTypes);
        int limit = requestedLimit == null ? 30 : requestedLimit;
        if (limit < 1 || limit > 30) throw new InvalidDocumentException("搜索数量必须在 1 到 30 之间");
        List<Candidate> candidates = new ArrayList<>();
        if (types.contains("document")) candidates.addAll(documents(userId, needle, pattern, limit));
        if (types.contains("note")) candidates.addAll(notes(userId, needle, pattern, limit));
        if (types.contains("tag")) candidates.addAll(tags(userId, needle, pattern, limit));
        candidates.sort(Comparator.comparingInt((Candidate value) -> value.rank)
                .thenComparing((Candidate value) -> value.updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(value -> value.hit.getType()).thenComparing(value -> value.hit.getDocumentId())
                .thenComparing(value -> value.hit.getNoteId(), Comparator.nullsLast(Comparator.naturalOrder())));
        return candidates.stream().limit(limit).map(value -> value.hit).collect(Collectors.toList());
    }

    private List<Candidate> documents(long userId, String needle, String pattern, int limit) {
        List<DocumentRow> rows = jdbc.query("SELECT id,name,content_text,updated_at,match_rank,match_position," +
                "CASE WHEN match_rank=0 THEN SUBSTRING(name,1,match_position-1) " +
                "ELSE SUBSTRING(content_text,1,match_position-1) END match_prefix FROM (" +
                "SELECT id,name,content_text,updated_at,CASE WHEN LOWER(name) LIKE LOWER(?) ESCAPE '!' THEN 0 ELSE 1 END match_rank," +
                "CASE WHEN LOWER(name) LIKE LOWER(?) ESCAPE '!' THEN LOCATE(LOWER(?),LOWER(name)) " +
                "ELSE LOCATE(LOWER(?),LOWER(content_text)) END match_position FROM document_record WHERE user_id=? " +
                "AND (LOWER(name) LIKE LOWER(?) ESCAPE '!' OR LOWER(content_text) LIKE LOWER(?) ESCAPE '!')) matched " +
                "ORDER BY match_rank,updated_at DESC LIMIT ?",
                this::documentRow, pattern, pattern, needle, needle, userId, pattern, pattern, limit);
        Map<Long, Candidate> matches = new LinkedHashMap<>();
        for (DocumentRow row : rows) {
            String source = row.matchRank == 0 ? row.name : row.content;
            matches.put(row.id, candidate("DOCUMENT", row.id, null, row.matchRank == 0 ? null : pageForText(row), row.name,
                    snippet(source, prefixLength(row.matchPrefix), needle.length()), row.matchRank, row.updatedAt));
        }
        List<ChunkRow> chunks = jdbc.query("SELECT document_id,page_number,chunk_index,content,name,updated_at,match_rank,match_position,match_prefix FROM (" +
                "SELECT c.document_id,c.page_number,c.chunk_index,c.content,d.name,d.updated_at,1 match_rank," +
                "LOCATE(LOWER(?),LOWER(c.content)) match_position," +
                "SUBSTRING(c.content,1,LOCATE(LOWER(?),LOWER(c.content))-1) match_prefix," +
                "ROW_NUMBER() OVER(PARTITION BY c.document_id ORDER BY c.page_number,c.chunk_index) match_number " +
                "FROM document_chunk c JOIN document_record d ON d.id=c.document_id WHERE d.user_id=? " +
                "AND LOWER(c.content) LIKE LOWER(?) ESCAPE '!') matched WHERE match_number=1 ORDER BY updated_at DESC LIMIT ?",
                this::chunkRow, needle, needle, userId, pattern, limit);
        for (ChunkRow row : chunks) {
            if (matches.containsKey(row.documentId)) continue;
            matches.put(row.documentId, candidate("DOCUMENT", row.documentId, null, row.pageNumber, row.name,
                    snippet(row.content, prefixLength(row.matchPrefix), needle.length()), row.matchRank, row.updatedAt));
        }
        return new ArrayList<>(matches.values());
    }

    private List<Candidate> notes(long userId, String needle, String pattern, int limit) {
        return jdbc.query("SELECT n.id,n.document_id,n.page_number,n.source_text,n.content_markdown,n.updated_at,d.name,1 match_rank," +
                "CASE WHEN LOWER(n.content_markdown) LIKE LOWER(?) ESCAPE '!' THEN 0 ELSE 1 END match_source," +
                "CASE WHEN LOWER(n.content_markdown) LIKE LOWER(?) ESCAPE '!' THEN LOCATE(LOWER(?),LOWER(n.content_markdown)) " +
                "ELSE LOCATE(LOWER(?),LOWER(n.source_text)) END match_position," +
                "CASE WHEN LOWER(n.content_markdown) LIKE LOWER(?) ESCAPE '!' " +
                "THEN SUBSTRING(n.content_markdown,1,LOCATE(LOWER(?),LOWER(n.content_markdown))-1) " +
                "ELSE SUBSTRING(n.source_text,1,LOCATE(LOWER(?),LOWER(n.source_text))-1) END match_prefix " +
                "FROM note n JOIN document_record d ON d.id=n.document_id WHERE d.user_id=? " +
                "AND (LOWER(n.content_markdown) LIKE LOWER(?) ESCAPE '!' OR LOWER(n.source_text) LIKE LOWER(?) ESCAPE '!') " +
                "ORDER BY n.updated_at DESC LIMIT ?", (rs, row) -> {
            String source=rs.getString("source_text"), content=rs.getString("content_markdown");
            boolean contentMatch = rs.getInt("match_source") == 0;
            String sourceValue = contentMatch ? content : source;
            return candidate("NOTE",rs.getLong("document_id"),rs.getLong("id"),(Integer)rs.getObject("page_number"),rs.getString("name"),
                    snippet(sourceValue,prefixLength(rs.getString("match_prefix")),needle.length()),rs.getInt("match_rank"),rs.getTimestamp("updated_at").toLocalDateTime());
        }, pattern,pattern,needle,needle,pattern,needle,needle,userId,pattern,pattern,limit);
    }

    private List<Candidate> tags(long userId, String needle, String pattern, int limit) {
        List<Candidate> rows=jdbc.query("SELECT id,name,document_id,note_id,updated_at,0 match_rank," +
                "LOCATE(LOWER(?),LOWER(name)) match_position,SUBSTRING(name,1,LOCATE(LOWER(?),LOWER(name))-1) match_prefix FROM (" +
                "SELECT associations.*,ROW_NUMBER() OVER(PARTITION BY document_id,id ORDER BY updated_at DESC,note_id) association_number FROM (" +
                "SELECT DISTINCT t.id,t.name,d.id document_id,NULL note_id,d.updated_at FROM tag t JOIN document_tag dt ON dt.tag_id=t.id " +
                "JOIN document_record d ON d.id=dt.document_id WHERE d.user_id=? AND LOWER(t.name) LIKE LOWER(?) ESCAPE '!' UNION ALL " +
                "SELECT DISTINCT t.id,t.name,d.id document_id,n.id note_id,n.updated_at FROM tag t JOIN note_tag nt ON nt.tag_id=t.id " +
                "JOIN note n ON n.id=nt.note_id JOIN document_record d ON d.id=n.document_id WHERE d.user_id=? " +
                "AND LOWER(t.name) LIKE LOWER(?) ESCAPE '!') associations) ranked WHERE association_number=1 " +
                "ORDER BY updated_at DESC,name,document_id,note_id LIMIT ?", (rs,row) -> {
            String name=rs.getString("name");
            Long noteId=(Long)rs.getObject("note_id");
            return candidate("TAG",rs.getLong("document_id"),noteId,null,name,snippet(name,prefixLength(rs.getString("match_prefix")),needle.length()),rs.getInt("match_rank"),rs.getTimestamp("updated_at").toLocalDateTime());
        }, needle,needle,userId,pattern,userId,pattern,limit);
        LinkedHashMap<String,Candidate> deduplicated=new LinkedHashMap<>();for(Candidate row:rows){String key=row.hit.getDocumentId()+":"+row.hit.getTitle();Candidate existing=deduplicated.get(key);if(existing==null||row.updatedAt.isAfter(existing.updatedAt))deduplicated.put(key,row);}return new ArrayList<>(deduplicated.values());
    }

    private Set<String> parseTypes(String value) {
        if (value == null || value.trim().isEmpty()) return ALLOWED_TYPES;
        LinkedHashSet<String> result = Arrays.stream(value.split(",", -1)).map(String::trim).map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toCollection(LinkedHashSet::new));
        if (result.isEmpty() || result.stream().anyMatch(type -> !ALLOWED_TYPES.contains(type))) throw new InvalidDocumentException("不支持的搜索类型");
        return result;
    }
    private String requireQuery(String query){String value=query==null?"":query.trim();int length=value.codePointCount(0,value.length());if(length<2||length>100)throw new InvalidDocumentException("搜索关键词长度必须在 2 到 100 之间");return value;}
    private String likePattern(String value){return "%"+value.replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
    private int prefixLength(String prefix){return prefix == null ? 0 : prefix.length();}
    private String snippet(String text,int match,int length){if(text==null)return "";int safeMatch=Math.min(match,text.length());int before=Math.min(80,text.codePointCount(0,safeMatch));int start=text.offsetByCodePoints(safeMatch,-before);int endOfMatch=Math.min(text.length(),safeMatch+length);int after=Math.min(80,text.codePointCount(endOfMatch,text.length()));int end=text.offsetByCodePoints(endOfMatch,after);return text.substring(start,end);}
    private Candidate candidate(String type,Long documentId,Long noteId,Integer page,String title,String snippet,int rank,LocalDateTime updated){return new Candidate(new UnifiedSearchHit(type,documentId,noteId,page,title,snippet),rank,updated);}
    private Integer pageForText(DocumentRow row){return row.content==null?null:1;}
    private DocumentRow documentRow(ResultSet rs,int ignored)throws SQLException{return new DocumentRow(rs.getLong("id"),rs.getString("name"),rs.getString("content_text"),rs.getInt("match_rank"),rs.getString("match_prefix"),rs.getTimestamp("updated_at").toLocalDateTime());}
    private ChunkRow chunkRow(ResultSet rs,int ignored)throws SQLException{return new ChunkRow(rs.getLong("document_id"),rs.getInt("page_number"),rs.getString("content"),rs.getString("name"),rs.getInt("match_rank"),rs.getString("match_prefix"),rs.getTimestamp("updated_at").toLocalDateTime());}
    private static final class Candidate{final UnifiedSearchHit hit;final int rank;final LocalDateTime updatedAt;Candidate(UnifiedSearchHit hit,int rank,LocalDateTime updatedAt){this.hit=hit;this.rank=rank;this.updatedAt=updatedAt;}}
    private static final class DocumentRow{final long id;final String name,content,matchPrefix;final int matchRank;final LocalDateTime updatedAt;DocumentRow(long id,String name,String content,int matchRank,String matchPrefix,LocalDateTime updatedAt){this.id=id;this.name=name;this.content=content;this.matchRank=matchRank;this.matchPrefix=matchPrefix;this.updatedAt=updatedAt;}}
    private static final class ChunkRow{final long documentId;final int pageNumber;final String content,name,matchPrefix;final int matchRank;final LocalDateTime updatedAt;ChunkRow(long id,int page,String content,String name,int matchRank,String matchPrefix,LocalDateTime updated){documentId=id;pageNumber=page;this.content=content;this.name=name;this.matchRank=matchRank;this.matchPrefix=matchPrefix;updatedAt=updated;}}
}
