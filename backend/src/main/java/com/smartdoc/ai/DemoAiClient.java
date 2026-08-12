package com.smartdoc.ai;

import com.smartdoc.document.TextChunk;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DemoAiClient implements AiClient {
    private static final Pattern TERM = Pattern.compile("[A-Za-z][A-Za-z0-9+#.-]{1,}|[\\u4e00-\\u9fff]{2,6}");

    @Override
    public AiSummary summarize(String text) {
        String clean = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        String summary = clean.isEmpty() ? "文档没有可提取文本。" : clean.substring(0, Math.min(clean.length(), 220));
        if (clean.length() > 220) summary += "…";
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        Matcher matcher = TERM.matcher(clean);
        while (matcher.find() && keywords.size() < 6) keywords.add(matcher.group());
        return new AiSummary("演示模式摘要：" + summary, new ArrayList<>(keywords));
    }

    @Override
    public String answer(String question, List<TextChunk> references) {
        if (references.isEmpty()) return "文档中没有找到足够相关的信息。";
        String evidence = references.stream().map(TextChunk::getContent).collect(Collectors.joining(" "));
        return "根据文档相关内容：" + evidence.substring(0, Math.min(320, evidence.length()));
    }

    @Override public String complete(String systemInstruction, String userPrompt) {
        String clean=userPrompt==null?"":userPrompt.trim();
        int count=clean.codePointCount(0,clean.length()),end=count<=320?clean.length():clean.offsetByCodePoints(0,320);
        return "演示模式结果："+clean.substring(0,end);
    }
    @Override public AiMode mode(){return AiMode.DEMO;}
    @Override public String model(){return "demo";}
}
