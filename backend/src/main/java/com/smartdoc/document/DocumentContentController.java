package com.smartdoc.document;

import com.smartdoc.auth.CurrentUser;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.Locale;

@RestController
@RequestMapping("/api/documents")
public class DocumentContentController {
    private final DocumentService service;

    public DocumentContentController(DocumentService service) { this.service=service; }

    @GetMapping("/{id}/content")
    public ResponseEntity<?> content(@PathVariable long id,HttpServletRequest request) throws Exception {
        long userId=CurrentUser.from(request).getUserId();
        DocumentRecord document=service.get(userId,id);
        if(!"READY".equals(document.getStatus()))throw new InvalidDocumentException("文档尚未解析完成");
        if(DocumentType.PDF.name().equals(document.getDocumentType())){
            InputStream input=service.openContent(userId,id);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\"document.pdf\"")
                    .header("X-Content-Type-Options","nosniff").body(new InputStreamResource(input));
        }
        return ResponseEntity.ok(new TextContent(document.getDocumentType(),language(document.getName()),document.getContentText()));
    }

    @PostMapping("/{id}/retry") public DocumentRecord retry(@PathVariable long id,HttpServletRequest request){return service.retry(CurrentUser.from(request).getUserId(),id);}
    @GetMapping("/{id}/delete-impact") public DocumentService.DeleteImpact deleteImpact(@PathVariable long id,HttpServletRequest request){return service.deleteImpact(CurrentUser.from(request).getUserId(),id);}

    static String language(String filename){
        String extension=DocumentType.extensionFromFilename(filename);
        switch(extension){
            case "java":return "java";case "xml":return "xml";case "yml":case "yaml":return "yaml";case "sql":return "sql";
            case "js":return "javascript";case "ts":return "typescript";case "json":return "json";case "properties":return "properties";
            case "sh":return "shell";case "ps1":return "powershell";case "md":case "markdown":return "markdown";case "txt":return "text";
            default:throw new InvalidDocumentException("暂不支持该文件类型");
        }
    }

    public static final class TextContent{
        private final String type,language,content;
        TextContent(String type,String language,String content){this.type=type.toLowerCase(Locale.ROOT);this.language=language;this.content=content;}
        public String getType(){return type;}public String getLanguage(){return language;}public String getContent(){return content;}
    }
}
