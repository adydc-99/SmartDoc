package com.smartdoc.ai;

import java.util.List;

public class AiSummary {
    private final String summary;
    private final List<String> keywords;
    public AiSummary(String summary, List<String> keywords) { this.summary = summary; this.keywords = keywords; }
    public String getSummary() { return summary; }
    public List<String> getKeywords() { return keywords; }
}
