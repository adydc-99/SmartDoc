package com.smartdoc.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdoc.document.TextChunk;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;

public class OpenAiCompatibleClient implements AiClient {
    private final RestTemplate http; private final ObjectMapper json; private final String baseUrl,apiKey,model;
    public OpenAiCompatibleClient(RestTemplate http,ObjectMapper json,String baseUrl,String apiKey,String model){
        this.http=http;this.json=json;this.baseUrl=baseUrl.replaceAll("/$","");this.apiKey=apiKey;this.model=model;
    }
    public AiSummary summarize(String text){
        String prompt="请用中文概括以下文档，第一行给出不超过200字的摘要，第二行以‘关键词：’开头列出5个关键词。\n\n"+text;
        String result=chat(prompt);String[] lines=result.split("\\R",2);String summary=lines[0].trim();
        String keywordLine=lines.length>1?lines[1].replaceFirst("^关键词[：:]?",""):"";
        List<String> keywords=Arrays.stream(keywordLine.split("[,，、]")).map(String::trim).filter(s->!s.isEmpty()).limit(6).collect(Collectors.toList());
        return new AiSummary(summary,keywords);
    }
    public String answer(String question,List<TextChunk> references){
        String context=references.stream().map(r->"[第"+r.getPageNumber()+"页] "+r.getContent()).collect(Collectors.joining("\n\n"));
        return chat("你是文档问答助手。只能依据给定原文回答；原文不足时明确说明。回答简洁并标注页码。\n问题："+question+"\n原文：\n"+context);
    }
    private String chat(String prompt){
        try{
            HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);headers.setBearerAuth(apiKey);
            Map<String,Object> body=new LinkedHashMap<>();body.put("model",model);body.put("temperature",0.2);
            body.put("messages",List.of(Map.of("role","user","content",prompt)));
            ResponseEntity<String> response=http.exchange(baseUrl+"/chat/completions",HttpMethod.POST,new HttpEntity<>(body,headers),String.class);
            JsonNode root=json.readTree(response.getBody());return root.path("choices").path(0).path("message").path("content").asText();
        }catch(Exception e){throw new IllegalStateException("AI 服务调用失败，请稍后重试",e);}
    }
}
