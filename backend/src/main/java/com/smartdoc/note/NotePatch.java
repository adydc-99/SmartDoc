package com.smartdoc.note;
import java.util.List;
public class NotePatch {
    private String contentMarkdown; private Boolean favorite; private List<Long> tagIds;
    public String getContentMarkdown(){return contentMarkdown;} public void setContentMarkdown(String value){contentMarkdown=value;}
    public Boolean getFavorite(){return favorite;} public void setFavorite(Boolean value){favorite=value;}
    public List<Long> getTagIds(){return tagIds;} public void setTagIds(List<Long> value){tagIds=value;}
}
