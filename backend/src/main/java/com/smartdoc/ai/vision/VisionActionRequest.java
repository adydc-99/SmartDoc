package com.smartdoc.ai.vision;

public class VisionActionRequest {
    private VisionAction action;
    private String question;
    private Integer pageNumber;

    public VisionActionRequest() {}
    public VisionActionRequest(VisionAction action, String question, Integer pageNumber) { this.action=action;this.question=question;this.pageNumber=pageNumber; }
    public VisionAction getAction(){return action;} public void setAction(VisionAction value){action=value;}
    public String getQuestion(){return question;} public void setQuestion(String value){question=value;}
    public Integer getPageNumber(){return pageNumber;} public void setPageNumber(Integer value){pageNumber=value;}
}
