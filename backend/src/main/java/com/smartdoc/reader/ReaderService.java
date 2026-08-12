package com.smartdoc.reader;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdoc.document.*;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.reader.mapper.ReadingProgressMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

@Service
public class ReaderService {
    private final ReadingProgressMapper progress;
    private final DocumentMapper documents;
    private final DocumentChunkMapper chunks;
    private final DocumentAccessPolicy access;

    public ReaderService(ReadingProgressMapper progress, DocumentMapper documents, DocumentChunkMapper chunks, DocumentAccessPolicy access) {
        this.progress = progress; this.documents = documents; this.chunks = chunks; this.access = access;
    }

    @Transactional
    public ProgressView save(long userId, long documentId, ProgressInput input) {
        DocumentRecord document = documents.selectOwned(documentId, userId);
        access.requireOwner(document, userId);
        validate(document, input);
        LocalDateTime now = LocalDateTime.now();
        int updated = progress.updateOwned(userId, documentId, input.getPageNumber(), input.getScrollRatio(), input.getZoom(), now);
        if (updated == 0) {
            try {
                if (progress.insertOwned(userId, documentId, input.getPageNumber(), input.getScrollRatio(), input.getZoom(), now) == 0) {
                    throw new DocumentNotFoundException();
                }
            } catch (DuplicateKeyException race) {
                if (progress.updateOwned(userId, documentId, input.getPageNumber(), input.getScrollRatio(), input.getZoom(), now) == 0) throw race;
            }
        }
        if (documents.touchLastOpened(documentId, userId, now) == 0) throw new DocumentNotFoundException();
        return new ProgressView(input.getPageNumber(), input.getScrollRatio(), input.getZoom(), now);
    }

    public ProgressView get(long userId, long documentId) {
        DocumentRecord document = documents.selectOwned(documentId, userId);
        access.requireOwner(document, userId);
        ReadingProgressRecord record = progress.selectOwned(userId, documentId);
        return record == null ? new ProgressView(1, 0, 1, null)
                : new ProgressView(record.getPageNumber(), record.getScrollRatio(), record.getZoom(), record.getUpdatedAt());
    }

    public List<DocumentRecord> recent(long userId, Integer requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit == null ? 8 : requestedLimit, 50));
        return documents.selectList(new LambdaQueryWrapper<DocumentRecord>()
                .eq(DocumentRecord::getUserId, userId).isNotNull(DocumentRecord::getLastOpenedAt)
                .orderByDesc(DocumentRecord::getLastOpenedAt).last("LIMIT " + limit));
    }

    public List<SearchHit> search(long userId, long documentId, String query, Integer requestedLimit) {
        DocumentRecord document = documents.selectOwned(documentId, userId);
        access.requireOwner(document, userId);
        String needle = requireQuery(query);
        int limit = requestedLimit == null ? 50 : requestedLimit;
        if (limit < 1 || limit > 50) throw new InvalidDocumentException("搜索数量必须在 1 到 50 之间");
        List<DocumentChunkRecord> records = chunks.selectOwnedOrdered(userId, documentId);
        List<SearchHit> result = new ArrayList<>();
        for (DocumentChunkRecord record : records) {
            int match = indexOfIgnoreCase(record.getContent(), needle);
            if (match >= 0) {
                result.add(snippet(record, match, needle.length()));
                if (result.size() == limit) break;
            }
        }
        return result;
    }

    private String requireQuery(String query) {
        String trimmed = query == null ? "" : query.trim();
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length < 2 || length > 100) throw new InvalidDocumentException("搜索关键词长度必须在 2 到 100 之间");
        return trimmed;
    }
    private int indexOfIgnoreCase(String text, String needle) {
        if (text == null) return -1;
        for (int index=0; index+needle.length()<=text.length(); index++) if (text.regionMatches(true,index,needle,0,needle.length())) return index;
        return -1;
    }
    private SearchHit snippet(DocumentChunkRecord chunk, int match, int matchLength) {
        String text = chunk.getContent();
        int startCodePoint = Math.max(0, text.codePointCount(0, match) - 80);
        int start = text.offsetByCodePoints(0, startCodePoint);
        int matchEnd = match + matchLength;
        int end = text.offsetByCodePoints(matchEnd, Math.min(80, text.codePointCount(matchEnd, text.length())));
        return new SearchHit(chunk.getPageNumber(), chunk.getChunkIndex(), text.substring(start, end), match - start, matchLength);
    }

    private void validate(DocumentRecord document, ProgressInput input) {
        if (input == null || input.getPageNumber() == null || input.getPageNumber() < 1) invalid();
        if (!"PDF".equals(document.getDocumentType()) && input.getPageNumber() != 1) invalid();
        if (document.getPageCount() != null && input.getPageNumber() > document.getPageCount()) invalid();
        if (input.getScrollRatio() == null || !Double.isFinite(input.getScrollRatio()) || input.getScrollRatio() < 0 || input.getScrollRatio() > 1) invalid();
        if (input.getZoom() == null || !Double.isFinite(input.getZoom()) || input.getZoom() < 0.5 || input.getZoom() > 3) invalid();
    }
    private void invalid() { throw new InvalidDocumentException("阅读进度参数不正确"); }
}
