package com.smartdoc.ai;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/settings/ai")
public class AiSettingsController {
    private final AiSettingsService settings; private final AiClient ai;
    public AiSettingsController(AiSettingsService settings,AiClient ai){this.settings=settings;this.ai=ai;}
    @GetMapping public AiSettingsView get(){return settings.view();}
    @PutMapping public AiSettingsView update(@RequestBody AiSettingsUpdate request){try{return settings.update(request);}catch(IllegalArgumentException|SecretPersistenceException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}}
    @PostMapping("/test") public AiTestResult test(){if(!(ai instanceof RoutingAiClient))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI routing is unavailable");return ((RoutingAiClient)ai).test();}
    @DeleteMapping("/key") @ResponseStatus(HttpStatus.NO_CONTENT) public void clear(){settings.clearKey();}
}
