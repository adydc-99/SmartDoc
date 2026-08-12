package com.smartdoc.note;
import java.util.List;
public class NoteInput {
    private Integer pageNumber; private String sourceText; private String contentMarkdown; private Boolean favorite; private List<Long> tagIds;
    public NoteInput(){}
    public NoteInput(Integer pageNumber,String sourceText,String contentMarkdown,Boolean favorite,List<Long>tagIds){this.pageNumber=pageNumber;this.sourceText=sourceText;this.contentMarkdown=contentMarkdown;this.favorite=favorite;this.tagIds=tagIds;}
    public Integer getPageNumber(){return pageNumber;} public void setPageNumber(Integer value){pageNumber=value;}
    public String getSourceText(){return sourceText;} public void setSourceText(String value){sourceText=value;}
    public String getContentMarkdown(){return contentMarkdown;} public void setContentMarkdown(String value){contentMarkdown=value;}
    public Boolean getFavorite(){return favorite;} public void setFavorite(Boolean value){favorite=value;}
    public List<Long> getTagIds(){return tagIds;} public void setTagIds(List<Long> value){tagIds=value;}
}
