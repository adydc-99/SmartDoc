package com.smartdoc.document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KeywordRetriever {
    private static final Pattern TOKEN = Pattern.compile("[a-zA-Z0-9]{2,}|[\\u4e00-\\u9fff]{2,}");

    public List<TextChunk> retrieve(String question, List<TextChunk> chunks, int limit) {
        Set<String> terms = tokens(question);
        return chunks.stream()
                .map(c -> new Scored(c, score(c.getContent(), terms)))
                .filter(scored -> scored.score > 0)
                .sorted(Comparator.comparingInt(Scored::getScore).reversed().thenComparingInt(s -> s.chunk.getIndex()))
                .limit(Math.max(0, limit)).map(s -> s.chunk).collect(Collectors.toList());
    }

    private int score(String content, Set<String> terms) {
        String lower = content.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (lower.contains(term)) score += term.matches(".*[\\u4e00-\\u9fff].*") ? term.length() : 2;
        }
        return score;
    }

    private Set<String> tokens(String text) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            result.add(token);
            if (token.matches("[\\u4e00-\\u9fff]+")) {
                for (int i = 0; i < token.length() - 1; i++) result.add(token.substring(i, i + 2));
            }
        }
        return result;
    }

    private static class Scored {
        private final TextChunk chunk; private final int score;
        private Scored(TextChunk chunk, int score) { this.chunk = chunk; this.score = score; }
        private int getScore() { return score; }
    }
}
