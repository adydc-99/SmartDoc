package com.smartdoc.reader;

import com.smartdoc.auth.CurrentUser;
import com.smartdoc.document.DocumentRecord;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import javax.validation.Valid;

@RestController
public class ReaderController {
    private final ReaderService service;
    public ReaderController(ReaderService service){this.service=service;}

    @PutMapping("/api/documents/{id}/progress")
    public ProgressView save(@PathVariable long id,@Valid @RequestBody ProgressInput body,HttpServletRequest request){return service.save(CurrentUser.from(request).getUserId(),id,body);}
    @GetMapping("/api/documents/{id}/progress")
    public ProgressView get(@PathVariable long id,HttpServletRequest request){return service.get(CurrentUser.from(request).getUserId(),id);}
    @GetMapping("/api/documents/{id}/search")
    public List<SearchHit> search(@PathVariable long id,@RequestParam String q,@RequestParam(defaultValue="50")Integer limit,HttpServletRequest request){return service.search(CurrentUser.from(request).getUserId(),id,q,limit);}
    @GetMapping("/api/reader/recent")
    public List<DocumentRecord> recent(@RequestParam(defaultValue="8")Integer limit,HttpServletRequest request){return service.recent(CurrentUser.from(request).getUserId(),limit);}
}
