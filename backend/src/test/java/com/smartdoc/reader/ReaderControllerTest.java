package com.smartdoc.reader;

import com.smartdoc.auth.*;
import com.smartdoc.document.DocumentRecord;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReaderControllerTest {
    @Test void forwardsAuthenticatedOwnerToAllReaderOperations() throws Exception {
        ReaderService service=mock(ReaderService.class);MockMvc mvc=MockMvcBuilders.standaloneSetup(new ReaderController(service)).build();
        when(service.save(eq(5L),eq(7L),any())).thenReturn(new ProgressView(2,.2,1.1,null));
        when(service.get(5L,7L)).thenReturn(new ProgressView(1,0,1,null));
        when(service.search(5L,7L,"needle",3)).thenReturn(List.of(new SearchHit(1,0,"needle",0,6)));
        when(service.recent(5L,2)).thenReturn(List.of(new DocumentRecord()));
        mvc.perform(put("/api/documents/7/progress").requestAttr(AuthFilter.PRINCIPAL_ATTRIBUTE,new AuthPrincipal(5L,"user")).contentType(MediaType.APPLICATION_JSON).content("{\"pageNumber\":2,\"scrollRatio\":0.2,\"zoom\":1.1}")) .andExpect(status().isOk()).andExpect(jsonPath("$.pageNumber").value(2));
        mvc.perform(get("/api/documents/7/progress").requestAttr(AuthFilter.PRINCIPAL_ATTRIBUTE,new AuthPrincipal(5L,"user"))).andExpect(status().isOk());
        mvc.perform(get("/api/documents/7/search?q=needle&limit=3").requestAttr(AuthFilter.PRINCIPAL_ATTRIBUTE,new AuthPrincipal(5L,"user"))).andExpect(status().isOk());
        mvc.perform(get("/api/reader/recent?limit=2").requestAttr(AuthFilter.PRINCIPAL_ATTRIBUTE,new AuthPrincipal(5L,"user"))).andExpect(status().isOk());
        verify(service).save(eq(5L),eq(7L),any());verify(service).get(5L,7L);verify(service).search(5L,7L,"needle",3);verify(service).recent(5L,2);
    }
}
