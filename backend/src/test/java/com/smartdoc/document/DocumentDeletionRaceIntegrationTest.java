package com.smartdoc.document;

import com.smartdoc.ai.AiClient;
import com.smartdoc.chat.QuestionService;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:delete-races;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class DocumentDeletionRaceIntegrationTest {
 @Autowired DocumentMapper documents;@Autowired DocumentProcessor processor;@Autowired DocumentService service;@Autowired QuestionService questions;@Autowired JdbcTemplate jdbc;
 @MockBean FileStorage storage;@MockBean AiClient ai;

 @Test void parserHoldingDocumentRowFinishesBeforeDeleteAndLeavesNoChunks()throws Exception{
  long id=document("PROCESSING","TEXT");CountDownLatch opened=new CountDownLatch(1),release=new CountDownLatch(1);
  when(storage.open("race-key")).thenAnswer(call->{opened.countDown();assertTrue(release.await(2,TimeUnit.SECONDS));return new ByteArrayInputStream("parsed text".getBytes(java.nio.charset.StandardCharsets.UTF_8));});
  processor.process(id);assertTrue(opened.await(2,TimeUnit.SECONDS));
  ExecutorService pool=Executors.newSingleThreadExecutor();try{Future<?> deletion=pool.submit(()->delete(id));Thread.sleep(100);assertFalse(deletion.isDone(),"delete must wait for parser DB row lock");release.countDown();deletion.get(3,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
  assertEquals(0,count("document_record",id));assertEquals(0,count("document_chunk",id));
 }

 @Test void questionHoldingDocumentRowCommitsBeforeDeleteAndHistoryIsThenRemoved()throws Exception{
  long id=document("READY","PDF");jdbc.update("INSERT INTO document_chunk(document_id,chunk_index,page_number,content) VALUES(?,0,1,'G1 collector')",id);
  CountDownLatch asked=new CountDownLatch(1),release=new CountDownLatch(1);when(ai.answer(anyString(),anyList())).thenAnswer(call->{asked.countDown();assertTrue(release.await(2,TimeUnit.SECONDS));return "answer";});
  ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<?> question=pool.submit(()->ask(id));assertTrue(asked.await(2,TimeUnit.SECONDS));Future<?> deletion=pool.submit(()->delete(id));Thread.sleep(100);assertFalse(deletion.isDone(),"delete must wait for question DB row lock");release.countDown();question.get(3,TimeUnit.SECONDS);deletion.get(3,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
  assertEquals(0,count("document_record",id));assertEquals(0,count("question_history",id));
 }
 private long document(String status,String type){LocalDateTime now=LocalDateTime.now();DocumentRecord d=new DocumentRecord();d.setUserId(7L);d.setName("race.txt");d.setSizeBytes(1L);d.setPageCount(1);d.setStorageKey("race-key");d.setStatus(status);d.setDocumentType(type);d.setFavorite(false);d.setCreatedAt(now);d.setUpdatedAt(now);documents.insert(d);return d.getId();}
 private int count(String table,long id){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+(table.equals("document_record")?"id":"document_id")+"=?",Integer.class,id);}
 private void delete(long id){try{service.delete(7L,id);}catch(Exception e){throw new CompletionException(e);}}
 private void ask(long id){try{questions.ask(7L,id,"collector?");}catch(Exception e){throw new CompletionException(e);}}
}
