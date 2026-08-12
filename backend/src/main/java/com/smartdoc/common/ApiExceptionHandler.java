package com.smartdoc.common;

import com.smartdoc.document.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DocumentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) public Map<String,String> notFound(DocumentNotFoundException e){return Map.of("message",e.getMessage());}
    @ExceptionHandler({InvalidDocumentException.class,MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> badRequest(Exception e){return Map.of("message",e instanceof InvalidDocumentException?e.getMessage():"请求参数不正确");}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> responseStatus(ResponseStatusException e){
        return ResponseEntity.status(e.getStatus()).body(Map.of("message",e.getReason()==null?"请求失败":e.getReason()));
    }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) public Map<String,String> other(Exception e){return Map.of("message","服务暂时不可用");}
}
