package com.smartdoc.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdoc.ai.*;
import com.smartdoc.document.mapper.*;
import com.smartdoc.storage.FileStorage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentProcessor {
    private final DocumentMapper documents; private final DocumentChunkMapper chunks;
    private final FileStorage storage; private final PdfTextExtractor extractor; private final TextChunker chunker; private final AiClient ai;
    public DocumentProcessor(DocumentMapper documents, DocumentChunkMapper chunks, FileStorage storage,
                             PdfTextExtractor extractor, TextChunker chunker, AiClient ai) {
        this.documents=documents; this.chunks=chunks; this.storage=storage; this.extractor=extractor; this.chunker=chunker; this.ai=ai;
    }
    @Async("documentExecutor") public void process(long documentId) {
        DocumentRecord doc = documents.selectById(documentId);
        try (InputStream input = storage.open(doc.getStorageKey())) {
            List<PageText> pages = extractor.extract(input);
            List<TextChunk> split = chunker.split(pages);
            if (split.isEmpty()) throw new InvalidDocumentException("PDF 没有可提取文本，扫描件暂不支持");
            chunks.delete(new LambdaQueryWrapper<DocumentChunkRecord>().eq(DocumentChunkRecord::getDocumentId, documentId));
            for (TextChunk chunk : split) {
                DocumentChunkRecord row = new DocumentChunkRecord(); row.setDocumentId(documentId);
                row.setChunkIndex(chunk.getIndex()); row.setPageNumber(chunk.getPageNumber()); row.setContent(chunk.getContent()); chunks.insert(row);
            }
            String fullText = pages.stream().map(PageText::getContent).collect(Collectors.joining("\n"));
            AiSummary result = ai.summarize(fullText.substring(0, Math.min(fullText.length(), 12000)));
            doc.setPageCount(pages.size()); doc.setSummary(result.getSummary()); doc.setKeywords(String.join(",", result.getKeywords()));
            doc.setStatus("READY"); doc.setErrorMessage(null);
        } catch (Exception e) {
            doc.setStatus("FAILED"); doc.setErrorMessage(safeMessage(e));
        }
        doc.setUpdatedAt(LocalDateTime.now()); documents.updateById(doc);
    }
    private String safeMessage(Exception e) { return e.getMessage() == null ? "文档解析失败" : e.getMessage().substring(0, Math.min(500, e.getMessage().length())); }
}
