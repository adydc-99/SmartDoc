package com.smartdoc.ai;
public class AiActionRequest {
 private AiAction action;private String question,selectedText;private Integer pageNumber;private boolean force;
 public AiActionRequest(){}
 public AiActionRequest(AiAction action,String question,String selectedText,Integer pageNumber,boolean force){this.action=action;this.question=question;this.selectedText=selectedText;this.pageNumber=pageNumber;this.force=force;}
 public AiAction getAction(){return action;}public void setAction(AiAction v){action=v;}public String getQuestion(){return question;}public void setQuestion(String v){question=v;}public String getSelectedText(){return selectedText;}public void setSelectedText(String v){selectedText=v;}public Integer getPageNumber(){return pageNumber;}public void setPageNumber(Integer v){pageNumber=v;}public boolean isForce(){return force;}public void setForce(boolean v){force=v;}
}
