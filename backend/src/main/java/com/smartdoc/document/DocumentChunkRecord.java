package com.smartdoc.document;
import com.baomidou.mybatisplus.annotation.*;
@TableName("document_chunk")
public class DocumentChunkRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private Long documentId; private Integer chunkIndex; private Integer pageNumber; private String content;
    @TableField(exist = false) private Integer matchPosition;
    @TableField(exist = false) private String matchPrefix;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;}
    public Integer getChunkIndex(){return chunkIndex;} public void setChunkIndex(Integer v){chunkIndex=v;}
    public Integer getPageNumber(){return pageNumber;} public void setPageNumber(Integer v){pageNumber=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;}
    public Integer getMatchPosition(){return matchPosition;} public void setMatchPosition(Integer v){matchPosition=v;}
    public String getMatchPrefix(){return matchPrefix;} public void setMatchPrefix(String v){matchPrefix=v;}
}
