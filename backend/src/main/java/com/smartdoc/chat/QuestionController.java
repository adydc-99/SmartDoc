package com.smartdoc.chat;
import com.smartdoc.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
@RestController @RequestMapping("/api/documents/{documentId}")
public class QuestionController {
    private final QuestionService service; public QuestionController(QuestionService service){this.service=service;}
    @PostMapping("/questions") public QuestionService.AnswerResponse ask(@PathVariable long documentId,@Valid @RequestBody AskRequest body,HttpServletRequest req) throws Exception{
        return service.ask(CurrentUser.from(req).getUserId(),documentId,body.question);
    }
    @GetMapping("/questions") public List<QuestionRecord> history(@PathVariable long documentId,HttpServletRequest req){return service.history(CurrentUser.from(req).getUserId(),documentId);}
    public static class AskRequest {@NotBlank public String question;}
}
