package com.smartdoc.ai.vision;

import com.fasterxml.jackson.databind.*;
import com.smartdoc.ai.DailyAiQuota;
import com.smartdoc.ai.provider.*;
import com.smartdoc.document.*;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.storage.FileStorage;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.List;

@Service
public class VisionActionService {
    private static final int MAX_IMAGE_BYTES=8*1024*1024,MAX_QUESTION_POINTS=500,MAX_OBSERVATION_POINTS=16_000,MAX_CONTEXT_POINTS=24_000,MAX_CONTEXT_CHUNKS=64;
    private static final long MAX_PIXELS=16_000_000L;
    private static final String PROMPT_VERSION="vision-v1";
    private static final String VISION_PROMPT="Analyze the supplied document image. Return only JSON with exactly these keys: description (string), ocrText (string), codeOrDiagram (string), uncertainties (array of strings). Do not wrap the JSON unless necessary.";

    private final DocumentMapper documents;private final DocumentChunkMapper chunks;private final FileStorage storage;private final VisionCacheMapper cache;private final ModelRouter router;
    private final DailyAiQuota quota;private final ObjectMapper json;private final PdfPageImageRenderer pdf;private final Clock clock;
    public VisionActionService(DocumentMapper documents,DocumentChunkMapper chunks,FileStorage storage,VisionCacheMapper cache,ModelRouter router,DailyAiQuota quota,ObjectMapper json,PdfPageImageRenderer pdf,Clock clock){this.documents=documents;this.chunks=chunks;this.storage=storage;this.cache=cache;this.router=router;this.quota=quota;this.json=json;this.pdf=pdf;this.clock=clock;}

    public VisionActionResponse execute(long userId,long documentId,VisionActionRequest request,MultipartFile screenshot)throws Exception{
        DocumentRecord document=documents.selectOwned(documentId,userId);if(document==null)throw new DocumentNotFoundException();
        if(!"READY".equals(document.getStatus()))throw new InvalidDocumentException("文档尚未解析完成");
        if(request==null||request.getAction()==null)throw new InvalidDocumentException("请选择视觉操作");
        String question=normalize(request.getQuestion());if(points(question)>MAX_QUESTION_POINTS)throw new InvalidDocumentException("问题长度不能超过 500 字");
        NormalizedImage image=image(document,request.getPageNumber(),screenshot);
        String documentContext=request.getAction()==VisionAction.DEEP_ANALYSIS?documentContext(userId,document,request.getPageNumber()):null;

        ProviderSession vision=router.require(userId,ProviderCapability.VISION);AiProviderConfig visionProvider=vision.getProvider();
        String hash=sha256(image.getBytes());LocalDateTime now=LocalDateTime.now(clock);
        VisionCacheRecord row=cache.selectOwned(userId,hash,visionProvider.getId(),visionProvider.getModel(),PROMPT_VERSION,now);
        boolean hit=row!=null;VisionObservation observation;
        if(hit){observation=parseObservation(row.getObservation());}
        else{
            AiRoutingConfig routing=requireRouting(userId);quota.consume(userId,routing.getDailyLimit());
            ProviderResponse response=vision.getAdapter().vision(visionProvider,vision.getApiKey(),new VisionCompletionRequest(VISION_PROMPT,image.getBytes(),image.getMediaType(),routing.getMaxOutputTokens()));
            observation=parseObservation(response.getContent());String stored=json.writeValueAsString(observation);
            cache.deleteExpiredOwnedKey(userId,hash,visionProvider.getId(),visionProvider.getModel(),PROMPT_VERSION,now);
            VisionCacheRecord candidate=new VisionCacheRecord();candidate.setUserId(userId);candidate.setDocumentId(documentId);candidate.setContentSha256(hash);candidate.setProviderId(visionProvider.getId());candidate.setModel(visionProvider.getModel());candidate.setPromptVersion(PROMPT_VERSION);candidate.setObservation(stored);candidate.setCreatedAt(now);candidate.setExpiresAt(now.plusDays(30));
            try{if(cache.insert(candidate)!=1)throw new IllegalStateException("Vision cache insert failed");row=candidate;}
            catch(DuplicateKeyException race){row=cache.selectOwned(userId,hash,visionProvider.getId(),visionProvider.getModel(),PROMPT_VERSION,LocalDateTime.now(clock));if(row==null)throw race;observation=parseObservation(row.getObservation());hit=true;}
        }
        if(request.getAction()==VisionAction.DIRECT)return new VisionActionResponse(request.getAction(),observation,null,visionProvider.getModel(),null,hit);

        ProviderSession text=router.require(userId,ProviderCapability.TEXT);AiRoutingConfig routing=requireRouting(userId);quota.consume(userId,routing.getDailyLimit());
        String observationJson=json.writeValueAsString(observation);String prompt="视觉观察：\n"+truncate(observationJson,MAX_OBSERVATION_POINTS)+"\n\n用户问题：\n"+question+"\n\nDocument text context:\n"+documentContext;
        String analysis=text.getAdapter().complete(text.getProvider(),text.getApiKey(),new TextCompletionRequest("基于视觉观察和当前文档文本深入分析；明确区分可见事实与推断。",prompt,routing.getMaxOutputTokens())).getContent();
        return new VisionActionResponse(request.getAction(),observation,analysis,visionProvider.getModel(),text.getProvider().getModel(),hit);
    }

    private NormalizedImage image(DocumentRecord document,Integer pageNumber,MultipartFile screenshot)throws Exception{
        if("PDF".equals(document.getDocumentType())){
            if(pageNumber==null||pageNumber<1||document.getPageCount()==null||pageNumber>document.getPageCount())throw new InvalidDocumentException("当前页码无效");
            try(InputStream in=storage.open(document.getStorageKey())){return pdf.render(in,pageNumber);}
        }
        if(screenshot==null||screenshot.isEmpty())throw new InvalidDocumentException("请粘贴或选择图片");
        if(screenshot.getSize()>MAX_IMAGE_BYTES)throw new InvalidDocumentException("图像数据超过 8 MiB 限制");
        byte[] bytes=screenshot.getBytes();if(!hasSupportedMagic(bytes))throw new InvalidDocumentException("仅支持 PNG、JPEG 或 WebP 图片");
        BufferedImage decoded;
        try(InputStream raw=new ByteArrayInputStream(bytes);ImageInputStream in=ImageIO.createImageInputStream(raw)){
            if(in==null)throw new InvalidDocumentException("图片无法解码");
            java.util.Iterator<ImageReader> readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw new InvalidDocumentException("图片无法解码");
            ImageReader reader=readers.next();try{reader.setInput(in,true,true);validateImageDimensions(reader);decoded=reader.read(0);}finally{reader.dispose();}
        }catch(IOException e){throw new InvalidDocumentException("图片无法解码");}
        if(decoded==null)throw new InvalidDocumentException("图片无法解码");
        try{long pixels=(long)decoded.getWidth()*decoded.getHeight();if(decoded.getWidth()<1||decoded.getHeight()<1||pixels>MAX_PIXELS)throw new InvalidDocumentException("图像像素数超过限制");return PdfPageImageRenderer.encodePng(decoded);}finally{decoded.flush();}
    }
    static void validateImageDimensions(ImageReader reader)throws IOException{int width=reader.getWidth(0),height=reader.getHeight(0);if(width<1||height<1||height>MAX_PIXELS||width>MAX_PIXELS/height)throw new InvalidDocumentException("图像像素数超过限制");}
    private static boolean hasSupportedMagic(byte[] b){boolean png=b.length>=8&&(b[0]&255)==0x89&&b[1]==0x50&&b[2]==0x4e&&b[3]==0x47&&b[4]==0x0d&&b[5]==0x0a&&b[6]==0x1a&&b[7]==0x0a;boolean jpeg=b.length>=3&&(b[0]&255)==0xff&&(b[1]&255)==0xd8&&(b[2]&255)==0xff;boolean webp=b.length>=12&&b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P';return png||jpeg||webp;}
    private VisionObservation parseObservation(String raw){try{String value=stripFence(raw);JsonNode node=json.readTree(value);if(node==null||!node.isObject()||!node.path("description").isTextual()||node.path("description").asText().trim().isEmpty()||!node.path("ocrText").isTextual()||!node.path("codeOrDiagram").isTextual()||!node.path("uncertainties").isArray())throw invalid();for(JsonNode item:node.path("uncertainties"))if(!item.isTextual())throw invalid();return json.treeToValue(node,VisionObservation.class);}catch(ProviderHttpException e){throw e;}catch(Exception e){throw invalid();}}
    private static String stripFence(String raw){if(raw==null||raw.trim().isEmpty())throw invalid();String value=raw.trim();if(!value.startsWith("```"))return value;int line=value.indexOf('\n');if(line<0||!value.endsWith("```"))throw invalid();String label=value.substring(3,line).trim();if(!label.isEmpty()&&!"json".equalsIgnoreCase(label))throw invalid();String inner=value.substring(line+1,value.length()-3).trim();if(inner.contains("```"))throw invalid();return inner;}
    private AiRoutingConfig requireRouting(long userId){AiRoutingConfig routing=router.routingFor(userId);if(routing==null||routing.getDailyLimit()==null||routing.getMaxOutputTokens()==null)throw new ProviderHttpException("MODEL_NOT_CONFIGURED",400);return routing;}
    private String documentContext(long userId,DocumentRecord document,Integer pageNumber){
        if(!"PDF".equals(document.getDocumentType()))return truncate(nullToEmpty(document.getContentText()),MAX_CONTEXT_POINTS);
        List<DocumentChunkRecord> rows=chunks.selectOwnedForVisionContext(userId,document.getId(),pageNumber==null?1:pageNumber,MAX_CONTEXT_CHUNKS);
        StringBuilder context=new StringBuilder();int remaining=MAX_CONTEXT_POINTS;
        if(rows!=null)for(DocumentChunkRecord row:rows){String content=normalize(row==null?null:row.getContent());if(content.isEmpty())continue;if(context.length()>0){if(remaining<2)break;context.append("\n\n");remaining-=2;}int available=Math.min(remaining,points(content));context.append(content,0,content.offsetByCodePoints(0,available));remaining-=available;if(remaining==0)break;}
        if(context.length()==0)throw new InvalidDocumentException("PDF 缺少可用于深度分析的文本上下文");return context.toString();
    }
    private static ProviderHttpException invalid(){return new ProviderHttpException("INVALID_VISION_RESPONSE",502);}
    private static String normalize(String value){return value==null?"":value.trim();}private static int points(String value){return value.codePointCount(0,value.length());}
    private static String truncate(String value,int maximum){if(points(value)<=maximum)return value;return value.substring(0,value.offsetByCodePoints(0,maximum));}
    private static String nullToEmpty(String value){return value==null?"":value;}
    private static String sha256(byte[] bytes){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder(64);for(byte b:digest)out.append(String.format("%02x",b&255));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
