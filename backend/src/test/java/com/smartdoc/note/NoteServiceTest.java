package com.smartdoc.note;

import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.DocumentNotFoundException;
import com.smartdoc.document.InvalidDocumentException;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.library.TagRecord;
import com.smartdoc.library.mapper.TagMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:note-service;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class NoteServiceTest {
    @Autowired NoteService service;
    @Autowired DocumentMapper documents;
    @Autowired TagMapper tags;

    @Test
    void savesSourceLinkedNoteAndExportsMarkdown() {
        DocumentRecord document = document(7L, "JVM 学习.md", "MARKDOWN", 1);
        TagRecord tag = tag("JVM", "#123456");

        NoteView note = service.create(7L, document.getId(),
                new NoteInput(1, "GC 原理", "> G1 divides heap into regions", false, List.of(tag.getId())));
        byte[] markdown = service.exportMarkdown(7L, document.getId());
        String text = new String(markdown, java.nio.charset.StandardCharsets.UTF_8);

        assertEquals(1, note.getPageNumber());
        assertEquals(List.of(tag.getId()), List.of(note.getTags().get(0).getId()));
        assertTrue(text.contains("# JVM 学习.md"));
        assertTrue(text.contains("导出时间：2026-08-12T10:15:30Z"));
        assertTrue(text.contains("## 第 1 页"));
        assertTrue(text.contains("GC 原理"));
        assertTrue(text.contains("G1 divides heap into regions"));
        assertTrue(text.contains("JVM"));
    }

    @Test
    void validatesUnicodeBoundsOwnershipAndPages() {
        DocumentRecord pdf=document(7L,"book.pdf","PDF",2); DocumentRecord text=document(7L,"code.java","CODE",1);
        assertThrows(DocumentNotFoundException.class,()->service.create(8L,pdf.getId(),new NoteInput(1,null,"x",false,List.of())));
        assertThrows(InvalidDocumentException.class,()->service.create(7L,pdf.getId(),new NoteInput(3,null,"x",false,List.of())));
        assertThrows(InvalidDocumentException.class,()->service.create(7L,text.getId(),new NoteInput(2,null,"x",false,List.of())));
        assertThrows(InvalidDocumentException.class,()->service.create(7L,pdf.getId(),new NoteInput(1,null,"   ",false,List.of())));
        service.create(7L,pdf.getId(),new NoteInput(1,"😀".repeat(5000),"😀".repeat(20000),false,List.of()));
        assertThrows(InvalidDocumentException.class,()->service.create(7L,pdf.getId(),new NoteInput(1,"😀".repeat(5001),"x",false,List.of())));
        assertThrows(InvalidDocumentException.class,()->service.create(7L,pdf.getId(),new NoteInput(1,null,"😀".repeat(20001),false,List.of())));
    }

    @Test
    void updatePreservesReplacesAndClearsTagsAndCrossOwnerLooksMissing() {
        DocumentRecord document=document(7L,"tags.pdf","PDF",2);TagRecord first=tag("first","#111111"),second=tag("second","#222222");
        NoteView created=service.create(7L,document.getId(),new NoteInput(1,null,"before",false,List.of(first.getId(),first.getId())));
        NotePatch preserve=new NotePatch();preserve.setContentMarkdown("after");
        assertEquals(List.of(first.getId()),ids(service.update(7L,created.getId(),preserve)));
        NotePatch replace=new NotePatch();replace.setFavorite(true);replace.setTagIds(List.of(second.getId()));
        NoteView replaced=service.update(7L,created.getId(),replace);assertTrue(replaced.getFavorite());assertEquals(List.of(second.getId()),ids(replaced));
        NotePatch clear=new NotePatch();clear.setTagIds(List.of());assertTrue(service.update(7L,created.getId(),clear).getTags().isEmpty());
        assertThrows(DocumentNotFoundException.class,()->service.update(8L,created.getId(),new NotePatch()));
        assertThrows(DocumentNotFoundException.class,()->service.delete(8L,created.getId()));
        service.delete(7L,created.getId());assertTrue(service.documentNotes(7L,document.getId()).isEmpty());
    }

    @Test
    void filtersUseLiteralMatchingOwnerScopeAndCapResults() {
        DocumentRecord owned=document(7L,"owned.pdf","PDF",1),other=document(8L,"other.pdf","PDF",1);TagRecord tag=tag("filter","#333333");
        service.create(7L,owned.getId(),new NoteInput(1,"literal 100%_", "wanted",true,List.of(tag.getId())));
        service.create(8L,other.getId(),new NoteInput(1,"literal 100%_", "hidden",true,List.of(tag.getId())));
        assertEquals(1,service.search(7L,"100%_",true,owned.getId(),tag.getId()).size());
        assertTrue(service.search(7L,"100xx",null,null,null).isEmpty());
        for(int i=0;i<205;i++)service.create(7L,owned.getId(),new NoteInput(1,null,"bulk "+i,false,List.of()));
        assertEquals(200,service.search(7L,null,null,null,null).size());
    }

    @Test
    void exportEscapesStructureHtmlAndFencesAndKeepsUtf8() {
        DocumentRecord document=document(7L,"# <script>标题</script>.pdf","PDF",1);
        service.create(7L,document.getId(),new NoteInput(1,"> quote\n```evil","# heading\n<img src=x>\n```",true,List.of()));
        String text=new String(service.exportMarkdown(7L,document.getId()),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("标题"));assertFalse(text.contains("<script>"));assertFalse(text.contains("<img"));assertFalse(text.contains("```"));
        assertEquals(1,text.lines().filter(line->line.startsWith("# ")).count());assertEquals(1,text.lines().filter(line->line.startsWith("## ")).count());
    }

    private List<Long> ids(NoteView view){List<Long> ids=new java.util.ArrayList<>();view.getTags().forEach(t->ids.add(t.getId()));return ids;}

    private DocumentRecord document(long owner, String name, String type, int pages) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 10, 0);
        DocumentRecord row = new DocumentRecord();
        row.setUserId(owner); row.setName(name); row.setSizeBytes(1L); row.setPageCount(pages);
        row.setStorageKey("test/" + name); row.setStatus("READY"); row.setDocumentType(type);
        row.setMimeType("text/plain"); row.setFavorite(false); row.setCreatedAt(now); row.setUpdatedAt(now);
        documents.insert(row); return row;
    }

    private TagRecord tag(String name, String color) {
        TagRecord row = new TagRecord(); row.setName(name); row.setColor(color);
        row.setCreatedAt(LocalDateTime.of(2026, 8, 12, 10, 0)); tags.insert(row); return row;
    }

    @TestConfiguration
    static class FixedTime {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-08-12T10:15:30Z"), ZoneOffset.UTC);
        }
    }
}
