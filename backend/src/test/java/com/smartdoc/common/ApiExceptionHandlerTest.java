package com.smartdoc.common;

import com.smartdoc.auth.*;
import com.smartdoc.reader.*;
import com.smartdoc.search.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {
    @ParameterizedTest
    @ValueSource(strings={
            "{\"pageNumber\":1,\"scrollRatio\":NaN,\"zoom\":1}",
            "{\"pageNumber\":1,\"scrollRatio\":\"NaN\",\"zoom\":1}",
            "{\"pageNumber\":1,\"scrollRatio\":Infinity,\"zoom\":1}",
            "{\"pageNumber\":1,\"scrollRatio\":1e9999,\"zoom\":1}",
            "{\"pageNumber\":\"bad\",\"scrollRatio\":0.5,\"zoom\":1}"
    })
    void malformedProgressBodiesReturn400WithoutCallingPersistence(String body) throws Exception {
        ReaderService service=mock(ReaderService.class);MockMvc mvc=readerMvc(service);
        mvc.perform(put("/api/documents/7/progress").requestAttr(AuthFilter.PRINCIPAL_ATTRIBUTE,new AuthPrincipal(5L,"user"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings={"/api/search","/api/search?q=ok&limit=bad","/api/documents/7/search?q=ok&limit=bad","/api/documents/bad/progress"})
    void missingOrMalformedRequestParametersReturn400(String path) throws Exception {
        ReaderService reader=mock(ReaderService.class);SearchService search=mock(SearchService.class);
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new ReaderController(reader),new SearchController(search)).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(get(path).requestAttr(AuthFilter.PRINCIPAL_ATTRIBUTE,new AuthPrincipal(5L,"user"))).andExpect(status().isBadRequest());
        verifyNoInteractions(reader,search);
    }

    private MockMvc readerMvc(ReaderService service){return MockMvcBuilders.standaloneSetup(new ReaderController(service)).setControllerAdvice(new ApiExceptionHandler()).build();}
}
