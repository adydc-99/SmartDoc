package com.smartdoc.ai;

import com.smartdoc.document.DocumentChunkRecord;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AiEvidenceSelector {
    private static final Pattern ENGLISH=Pattern.compile("[a-z0-9]+");
    private static final Set<String> STOP=Set.of("the","and","how","what","why","的","了","是","什么","为什么","如何");
    private final int maximumSources,excerptLimit;

    AiEvidenceSelector(int maximumSources,int excerptLimit){
        if(maximumSources<1||excerptLimit<1)throw new IllegalArgumentException("Evidence limits must be positive");
        this.maximumSources=maximumSources;this.excerptLimit=excerptLimit;
    }

    Selection select(long documentId,List<DocumentChunkRecord> chunks,String question,int contextLimit){
        if(contextLimit<1||chunks==null||chunks.isEmpty())return new Selection("",Collections.emptyList());
        String normalizedQuestion=normalize(question);Set<String> tokens=tokens(question);
        List<Candidate> candidates=new ArrayList<>();int topScore=0;
        for(DocumentChunkRecord chunk:chunks){
            if(chunk==null||chunk.getContent()==null||chunk.getContent().trim().isEmpty())continue;
            int score=score(normalize(chunk.getContent()),normalizedQuestion,tokens);topScore=Math.max(topScore,score);candidates.add(new Candidate(chunk,score));
        }
        final int maximumScore=topScore;
        candidates.sort(Comparator.comparingInt(Candidate::getScore).reversed()
                .thenComparing(candidate->candidate.chunk.getPageNumber(),Comparator.nullsLast(Integer::compareTo))
                .thenComparing(candidate->candidate.chunk.getChunkIndex(),Comparator.nullsLast(Integer::compareTo)));
        if(maximumScore>0)candidates.removeIf(candidate->candidate.score==0);

        StringBuilder context=new StringBuilder();List<AiActionResponse.Source> sources=new ArrayList<>();List<String> accepted=new ArrayList<>();
        for(Candidate candidate:candidates){
            if(sources.size()>=maximumSources)break;
            String normalizedContent=normalize(candidate.chunk.getContent());if(duplicate(normalizedContent,accepted))continue;
            String header="[第 "+value(candidate.chunk.getPageNumber())+" 页，段落 "+value(candidate.chunk.getChunkIndex())+"]\n";
            int separator=context.length()==0?0:2,remaining=contextLimit-points(context.toString())-separator;
            if(remaining<=points(header))break;
            String transmitted=candidate.chunk.getContent().trim();int required=points(header)+points(transmitted);
            if(required>remaining){if(!sources.isEmpty())break;transmitted=truncate(transmitted,remaining-points(header));}
            if(transmitted.isEmpty())break;
            if(separator>0)context.append("\n\n");context.append(header).append(transmitted);
            accepted.add(normalizedContent);
            String relevance=maximumScore>0&&candidate.score*10>=maximumScore*6?"HIGH":"RELATED";
            sources.add(new AiActionResponse.Source(documentId,candidate.chunk.getPageNumber(),candidate.chunk.getChunkIndex(),truncate(transmitted,excerptLimit),relevance));
        }
        return new Selection(context.toString(),sources);
    }

    private static int score(String content,String question,Set<String> tokens){
        int score=!question.isEmpty()&&content.contains(question)?8:0;
        for(String token:tokens)score+=Math.min(3,occurrences(content,token))*(2+points(token));
        return score;
    }
    private static int occurrences(String value,String token){int count=0,from=0,index;while((index=value.indexOf(token,from))>=0){count++;from=index+token.length();}return count;}
    private static Set<String> tokens(String value){
        LinkedHashSet<String> out=new LinkedHashSet<>();String lower=value==null?"":value.toLowerCase(Locale.ROOT);
        Matcher english=ENGLISH.matcher(lower);while(english.find())add(out,english.group());
        StringBuilder han=new StringBuilder();lower.codePoints().forEach(codePoint->{if(Character.UnicodeScript.of(codePoint)==Character.UnicodeScript.HAN)han.appendCodePoint(codePoint);else{addHanBigrams(out,han.toString());han.setLength(0);}});addHanBigrams(out,han.toString());
        return out;
    }
    private static void add(Set<String> out,String token){if(points(token)>=2&&!STOP.contains(token))out.add(token);}
    private static void addHanBigrams(Set<String> out,String value){int count=points(value);for(int i=0;i<count-1;i++){int start=value.offsetByCodePoints(0,i),end=value.offsetByCodePoints(start,2);add(out,value.substring(start,end));}}
    private static boolean duplicate(String content,List<String> accepted){for(String value:accepted){if(content.equals(value))return true;String shorter=content.length()<=value.length()?content:value,longer=content.length()<=value.length()?value:content;if(!shorter.isEmpty()&&longer.contains(shorter)&&points(shorter)*10>=points(longer)*8)return true;}return false;}
    private static String normalize(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim().replaceAll("\\s+"," ");}
    private static String value(Integer value){return value==null?"-":value.toString();}
    private static int points(String value){return value.codePointCount(0,value.length());}
    private static String truncate(String value,int maximum){if(maximum<=0)return "";if(points(value)<=maximum)return value;return value.substring(0,value.offsetByCodePoints(0,maximum));}

    static final class Selection {
        private final String context;private final List<AiActionResponse.Source> sources;
        Selection(String context,List<AiActionResponse.Source> sources){this.context=context;this.sources=Collections.unmodifiableList(new ArrayList<>(sources));}
        String getContext(){return context;}List<AiActionResponse.Source> getSources(){return sources;}
    }
    private static final class Candidate {private final DocumentChunkRecord chunk;private final int score;Candidate(DocumentChunkRecord chunk,int score){this.chunk=chunk;this.score=score;}int getScore(){return score;}}
}
