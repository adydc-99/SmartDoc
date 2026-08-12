package com.smartdoc.ai;
import com.smartdoc.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
@RestController @RequestMapping("/api/documents/{documentId}/ai/actions")
public class AiActionController {
 private final AiActionService service;public AiActionController(AiActionService service){this.service=service;}
 @PostMapping public AiActionResponse execute(@PathVariable long documentId,@RequestBody AiActionRequest body,HttpServletRequest request){return service.execute(CurrentUser.from(request).getUserId(),documentId,body);}
}
