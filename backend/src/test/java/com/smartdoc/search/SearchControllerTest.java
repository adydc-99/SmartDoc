package com.smartdoc.search;

import com.smartdoc.auth.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchControllerTest {
    @Test void forwardsAuthenticatedOwnerAndQueryParameters() throws Exception {
        SearchService service=mock(SearchService.class);MockMvc mvc=MockMvcBuilders.standaloneSetup(new SearchController(service)).build();
        when(service.search(8L,"needle","document,note",4)).thenReturn(List.of(new UnifiedSearchHit("DOCUMENT",1L,null,null,"Needle","Needle")));
        mvc.perform(get("/api/search?q=needle&types=document,note&limit=4").requestAttr(AuthFilter.PRINCIPAL_ATTRIBUTE,new AuthPrincipal(8L,"user")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("DOCUMENT"));
        verify(service).search(8L,"needle","document,note",4);
    }
}
