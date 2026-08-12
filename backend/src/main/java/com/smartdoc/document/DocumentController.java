package com.smartdoc.document;

import com.smartdoc.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service=service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public DocumentRecord upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        return service.upload(CurrentUser.from(request).getUserId(), file);
    }
    @GetMapping public List<DocumentRecord> list(HttpServletRequest request) { return service.list(CurrentUser.from(request).getUserId()); }
    @GetMapping("/{id}") public DocumentRecord get(@PathVariable long id,HttpServletRequest request) { return service.get(CurrentUser.from(request).getUserId(),id); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id,HttpServletRequest request) throws Exception { service.delete(CurrentUser.from(request).getUserId(),id); }
}
