package com.smartdoc.chat;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("question_history")
public class QuestionRecord {
    @TableId(type=IdType.AUTO) private Long id; private Long documentId; private Long userId;
    private String question; private String answer; private String referencesJson; private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getQuestion(){return question;} public void setQuestion(String v){question=v;}
    public String getAnswer(){return answer;} public void setAnswer(String v){answer=v;} public String getReferencesJson(){return referencesJson;} public void setReferencesJson(String v){referencesJson=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
