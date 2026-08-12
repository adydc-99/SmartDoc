package com.smartdoc.document;

import com.smartdoc.auth.AuthTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.datasource.url=jdbc:h2:mem:multipart-boundary;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "smartdoc.storage.local-path=./target/multipart-boundary-files"
})
class MultipartBoundaryIntegrationTest {
    @LocalServerPort int port;
    @Autowired TestRestTemplate http;
    @Autowired AuthTokenService tokens;

    @Test
    void acceptsExactTwentyMiBPdfIncludingMultipartEnvelope() {
        byte[] content=new byte[20*1024*1024];
        byte[] header="%PDF-".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(header,0,content,0,header.length);
        ByteArrayResource file=new ByteArrayResource(content){@Override public String getFilename(){return "boundary.pdf";}};
        LinkedMultiValueMap<String,Object> body=new LinkedMultiValueMap<>();body.add("file",file);
        HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(tokens.issue(1L,"boundary"));

        ResponseEntity<String> response=http.postForEntity("http://localhost:"+port+"/api/documents",new HttpEntity<>(body,headers),String.class);

        assertEquals(HttpStatus.CREATED,response.getStatusCode(),response.getBody());
    }
}
