package com.smartdoc.search;

import com.smartdoc.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class SearchController {
    private final SearchService service;
    public SearchController(SearchService service){this.service=service;}
    @GetMapping("/api/search")
    public List<UnifiedSearchHit> search(@RequestParam String q,@RequestParam(required=false)String types,
            @RequestParam(defaultValue="30")Integer limit,HttpServletRequest request){return service.search(CurrentUser.from(request).getUserId(),q,types,limit);}
}
