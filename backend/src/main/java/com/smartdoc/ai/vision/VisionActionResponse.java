package com.smartdoc.ai.vision;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisionActionResponse {
    private final VisionAction action;
    private final VisionObservation observation;
    private final String analysis;
    private final String visionModel;
    private final String textModel;
    private final boolean visionCacheHit;
    public VisionActionResponse(VisionAction action,VisionObservation observation,String analysis,String visionModel,String textModel,boolean cacheHit){this.action=action;this.observation=observation;this.analysis=analysis;this.visionModel=visionModel;this.textModel=textModel;this.visionCacheHit=cacheHit;}
    public VisionAction getAction(){return action;} public VisionObservation getObservation(){return observation;} public String getAnalysis(){return analysis;}
    public String getVisionModel(){return visionModel;} public String getTextModel(){return textModel;} public boolean isVisionCacheHit(){return visionCacheHit;}
}
