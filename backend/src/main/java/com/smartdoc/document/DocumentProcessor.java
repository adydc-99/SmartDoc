package com.smartdoc.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdoc.ai.AiClient;
import com.smartdoc.document.mapper.*;
import com.smartdoc.storage.FileStorage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentProcessor {
    private final DocumentMapper documents; private final DocumentChunkMapper chunks;
    private final FileStorage storage; private final PdfTextExtractor pdfExtractor; private final TextDocumentExtractor textExtractor; private final TextChunker chunker;
    public DocumentProcessor(DocumentMapper documents, DocumentChunkMapper chunks, FileStorage storage,
                             PdfTextExtractor pdfExtractor, TextDocumentExtractor textExtractor, TextChunker chunker, AiClient ai) {
        this.documents=documents; this.chunks=chunks; this.storage=storage; this.pdfExtractor=pdfExtractor; this.textExtractor=textExtractor; this.chunker=chunker;
    }
    @Async("documentExecutor") public void process(long documentId) {
        DocumentRecord doc = documents.selectById(documentId);
        try (InputStream input = storage.open(doc.getStorageKey())) {
            DocumentType type=DocumentType.valueOf(doc.getDocumentType());
            List<PageText> pages;
            if(type==DocumentType.PDF){pages=pdfExtractor.extract(input);doc.setContentText(null);}
            else {String content=textExtractor.read(input.readAllBytes());pages=List.of(new PageText(1,content));doc.setContentText(content);}
            List<TextChunk> split = chunker.split(pages);
            if (split.isEmpty()) throw new InvalidDocumentException(type==DocumentType.PDF?"PDF 没有可提取文本，扫描件暂不支持":"文本文件没有可处理内容");
            chunks.delete(new LambdaQueryWrapper<DocumentChunkRecord>().eq(DocumentChunkRecord::getDocumentId, documentId));
            for (TextChunk chunk : split) {
                DocumentChunkRecord row = new DocumentChunkRecord(); row.setDocumentId(documentId);
                row.setChunkIndex(chunk.getIndex()); row.setPageNumber(chunk.getPageNumber()); row.setContent(chunk.getContent()); chunks.insert(row);
            }
            doc.setPageCount(pages.size());
            doc.setStatus("READY"); doc.setErrorMessage(null);
        } catch (Exception e) {
            doc.setStatus("FAILED"); doc.setErrorMessage(safeMessage(e));
        }
        doc.setUpdatedAt(LocalDateTime.now()); documents.updateById(doc);
    }
    private String safeMessage(Exception e) { return e.getMessage() == null ? "文档解析失败" : e.getMessage().substring(0, Math.min(500, e.getMessage().length())); }
}
