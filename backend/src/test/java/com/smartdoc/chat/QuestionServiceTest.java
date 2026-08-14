package com.smartdoc.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdoc.ai.AiClient;
import com.smartdoc.document.*;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.chat.mapper.QuestionMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuestionServiceTest {
 @Test void asksAiWithTheAuthenticatedUserId() throws Exception {
  DocumentService documents=mock(DocumentService.class);DocumentChunkMapper chunks=mock(DocumentChunkMapper.class);QuestionMapper questions=mock(QuestionMapper.class);AiClient ai=mock(AiClient.class);
  DocumentRecord doc=new DocumentRecord();doc.setId(4L);doc.setStatus("READY");when(documents.getForUpdate(7L,4L)).thenReturn(doc);
  DocumentChunkRecord chunk=new DocumentChunkRecord();chunk.setChunkIndex(0);chunk.setPageNumber(1);chunk.setContent("evidence");when(chunks.selectList(any())).thenReturn(List.of(chunk));when(ai.answer(eq(7L),eq("what"),anyList())).thenReturn("answer");
  QuestionService service=new QuestionService(documents,chunks,questions,new KeywordRetriever(),ai,new ObjectMapper(),new DocumentLockManager());
  service.ask(7L,4L,"what");
  verify(ai).answer(eq(7L),eq("what"),anyList()); verify(ai,never()).answer(eq("what"),anyList());
 }
}
