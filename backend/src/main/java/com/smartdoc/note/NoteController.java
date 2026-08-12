package com.smartdoc.note;

import com.smartdoc.auth.CurrentUser;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class NoteController {
 private final NoteService service;public NoteController(NoteService service){this.service=service;}
 @PostMapping("/api/documents/{documentId}/notes") @ResponseStatus(HttpStatus.CREATED)
 public NoteView create(@PathVariable long documentId,@RequestBody NoteInput input,HttpServletRequest request){return service.create(user(request),documentId,input);}
 @GetMapping("/api/documents/{documentId}/notes") public List<NoteView> documentNotes(@PathVariable long documentId,HttpServletRequest request){return service.documentNotes(user(request),documentId);}
 @PatchMapping("/api/notes/{id}") public NoteView update(@PathVariable long id,@RequestBody NotePatch input,HttpServletRequest request){return service.update(user(request),id,input);}
 @DeleteMapping("/api/notes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable long id,HttpServletRequest request){service.delete(user(request),id);}
 @GetMapping("/api/notes") public List<NoteView> search(@RequestParam(required=false)String query,@RequestParam(required=false)Boolean favorite,@RequestParam(required=false)Long documentId,@RequestParam(required=false)Long tagId,HttpServletRequest request){return service.search(user(request),query,favorite,documentId,tagId);}
 @GetMapping("/api/notes/export") public ResponseEntity<byte[]> export(@RequestParam long documentId,HttpServletRequest request){return ResponseEntity.ok().contentType(new MediaType("text","markdown",java.nio.charset.StandardCharsets.UTF_8)).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=notes.md").header("X-Content-Type-Options","nosniff").body(service.exportMarkdown(user(request),documentId));}
 private long user(HttpServletRequest request){return CurrentUser.from(request).getUserId();}
}
