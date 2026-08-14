package com.smartdoc.ai.vision;

import com.smartdoc.auth.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/documents/{documentId}/vision-actions")
public class VisionActionController {
    private final VisionActionService service;public VisionActionController(VisionActionService service){this.service=service;}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisionActionResponse execute(@PathVariable long documentId,@ModelAttribute VisionActionRequest body,@RequestPart(value="screenshot",required=false) MultipartFile screenshot,HttpServletRequest request)throws Exception{return service.execute(CurrentUser.from(request).getUserId(),documentId,body,screenshot);}
}
