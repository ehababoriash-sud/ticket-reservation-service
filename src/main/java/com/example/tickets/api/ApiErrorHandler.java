package com.example.tickets.api;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
@RestControllerAdvice
public class ApiErrorHandler {
 @ExceptionHandler(ApiException.class)
 public ResponseEntity<Map<String,Object>> business(ApiException e) {
  return ResponseEntity.status(e.status).body(Map.of("code",e.code,"message",e.getMessage(),"timestamp",Instant.now().toString()));
 }
 @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, ConstraintViolationException.class})
 public ResponseEntity<Map<String,String>> invalid(Exception e) {
  return ResponseEntity.badRequest().body(Map.of("code","INVALID_INPUT","message","Invalid request payload"));
 }
}
