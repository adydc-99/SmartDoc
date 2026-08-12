package com.smartdoc.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdoc.document.TextChunk;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;

public class OpenAiCompatibleClient implements NetworkAiClient {
    private final RestTemplate http; private final ObjectMapper json; private final String baseUrl,apiKey,model; private final int maxOutputTokens;
    public OpenAiCompatibleClient(RestTemplate http,ObjectMapper json,String baseUrl,String apiKey,String model,int maxOutputTokens){this.http=http;this.json=json;this.baseUrl=baseUrl.replaceAll("/$","");this.apiKey=apiKey;this.model=model;this.maxOutputTokens=maxOutputTokens;}
    public AiSummary summarize(String text){String result=chat("Summarize this document concisely and list up to six keywords:\n\n"+text,maxOutputTokens);String[] lines=result.split("\\R",2);return new AiSummary(lines[0].trim(),lines.length>1?Arrays.stream(lines[1].split("[,，、]")).map(String::trim).filter(s->!s.isEmpty()).limit(6).collect(Collectors.toList()):List.of());}
    public String answer(String question,List<TextChunk> references){String context=references.stream().map(r->"[page "+r.getPageNumber()+"] "+r.getContent()).collect(Collectors.joining("\n\n"));return chat("Answer only from the supplied document context and cite pages.\nQuestion: "+question+"\nContext:\n"+context,maxOutputTokens);}
    public void probe(){chat("Reply with OK only.",1);}
    private String chat(String prompt,int outputTokens){try{HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);headers.setBearerAuth(apiKey);Map<String,Object> body=new LinkedHashMap<>();body.put("model",model);body.put("temperature",0.2);body.put("max_tokens",outputTokens);body.put("messages",List.of(Map.of("role","user","content",prompt)));ResponseEntity<String> response=http.exchange(baseUrl+"/chat/completions",HttpMethod.POST,new HttpEntity<>(body,headers),String.class);JsonNode root=json.readTree(response.getBody());String content=root.path("choices").path(0).path("message").path("content").asText();if(content.isBlank())throw new IllegalStateException();return content;}catch(Exception e){throw new IllegalStateException("AI service request failed");}}
}
