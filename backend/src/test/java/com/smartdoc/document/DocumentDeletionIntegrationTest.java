package com.smartdoc.document;

import com.smartdoc.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:document-delete;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class DocumentDeletionIntegrationTest {
 @Autowired DocumentService service;@Autowired JdbcTemplate jdbc;@Autowired com.smartdoc.document.mapper.DocumentMapper documents;@MockBean FileStorage storage;

 @Test void impactCountsCurrentOwnedRowsAndDeleteCascadesEverythingAfterCommit()throws Exception{
  long id=document(7L,"READY","objects/delete-me.pdf");seed(id,7L);
  DocumentService.DeleteImpact impact=service.deleteImpact(7L,id);
  assertEquals(2,impact.getNotes());assertEquals(1,impact.getExcerpts());assertEquals(1,impact.getQuestions());assertEquals(1,impact.getAiResults());assertEquals(0,impact.getReviewItems());
  service.delete(7L,id);
  for(String table:new String[]{"note_tag","note","reading_progress","document_tag","ai_result","question_history","document_chunk","document_record"})assertEquals(0,count(table),table);
  verify(storage).delete("objects/delete-me.pdf");
 }

 @Test void storageFailureAfterCommitDoesNotRestoreDatabase()throws Exception{
  long id=document(7L,"READY","objects/fail.pdf");doThrow(new IllegalStateException("offline")).when(storage).delete("objects/fail.pdf");
  service.delete(7L,id);
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM document_record WHERE id=?",Integer.class,id));verify(storage).delete("objects/fail.pdf");
 }

 @Test void deletingProcessingDocumentClaimsAndRemovesIt()throws Exception{
  long id=document(7L,"PROCESSING","objects/processing.pdf");service.delete(7L,id);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM document_record WHERE id=?",Integer.class,id));
 }

 @Test @org.springframework.test.annotation.DirtiesContext(methodMode=org.springframework.test.annotation.DirtiesContext.MethodMode.AFTER_METHOD)
 void rollbackPreventsStorageDeletionAndRestoresAllRows()throws Exception{
  long id=document(7L,"READY","objects/rollback.pdf");seed(id,7L);jdbc.execute("DROP TABLE document_chunk");
  assertThrows(Exception.class,()->service.delete(7L,id));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM document_record WHERE id=?",Integer.class,id));
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM note WHERE document_id=?",Integer.class,id));
  verify(storage,never()).delete("objects/rollback.pdf");
 }

 private long document(long owner,String status,String key){LocalDateTime now=LocalDateTime.now();DocumentRecord d=new DocumentRecord();d.setUserId(owner);d.setName("delete.pdf");d.setSizeBytes(1L);d.setPageCount(2);d.setStorageKey(key);d.setStatus(status);d.setDocumentType("PDF");d.setFavorite(false);d.setCreatedAt(now);d.setUpdatedAt(now);documents.insert(d);return d.getId();}
 private void seed(long id,long user){LocalDateTime now=LocalDateTime.now();String tagName="t"+id;jdbc.update("INSERT INTO tag(name,color,created_at) VALUES(?,'#111111',?)",tagName,now);Long tag=jdbc.queryForObject("SELECT id FROM tag WHERE name=?",Long.class,tagName);jdbc.update("INSERT INTO note(document_id,page_number,source_text,content_markdown,favorite,created_at,updated_at) VALUES(?,1,'quote','one',false,?,?)",id,now,now);jdbc.update("INSERT INTO note(document_id,page_number,source_text,content_markdown,favorite,created_at,updated_at) VALUES(?,2,' ','two',false,?,?)",id,now,now);Long note=jdbc.queryForObject("SELECT MIN(id) FROM note WHERE document_id=?",Long.class,id);jdbc.update("INSERT INTO note_tag(note_id,tag_id) VALUES(?,?)",note,tag);jdbc.update("INSERT INTO reading_progress(document_id,page_number,scroll_ratio,zoom,updated_at) VALUES(?,1,0,1,?)",id,now);jdbc.update("INSERT INTO document_tag(document_id,tag_id) VALUES(?,?)",id,tag);jdbc.update("INSERT INTO ai_result(document_id,action,cache_key,content_markdown,created_at) VALUES(?,'SUMMARY','k','a',?)",id,now);jdbc.update("INSERT INTO question_history(document_id,user_id,question,answer,references_json,created_at) VALUES(?,?,'q','a','[]',?)",id,user,now);jdbc.update("INSERT INTO document_chunk(document_id,chunk_index,page_number,content) VALUES(?,0,1,'c')",id);}
 private int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
}
