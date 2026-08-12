package com.smartdoc.ai;
public final class AiTestResult { private final long latency; private final String model; private final AiMode mode; public AiTestResult(long latency,String model,AiMode mode){this.latency=latency;this.model=model;this.mode=mode;} public long getLatency(){return latency;} public String getModel(){return model;} public AiMode getMode(){return mode;} }
