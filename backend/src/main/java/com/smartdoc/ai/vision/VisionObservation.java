package com.smartdoc.ai.vision;

import java.util.List;

public class VisionObservation {
    private String description;
    private String ocrText;
    private String codeOrDiagram;
    private List<String> uncertainties;
    public VisionObservation() {}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public String getOcrText(){return ocrText;} public void setOcrText(String value){ocrText=value;}
    public String getCodeOrDiagram(){return codeOrDiagram;} public void setCodeOrDiagram(String value){codeOrDiagram=value;}
    public List<String> getUncertainties(){return uncertainties;} public void setUncertainties(List<String> value){uncertainties=value;}
}
