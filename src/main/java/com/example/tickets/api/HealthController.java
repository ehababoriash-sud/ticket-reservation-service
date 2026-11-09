package com.example.tickets.api;
import org.springframework.boot.actuate.health.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class HealthController {
 private final HealthEndpoint health;
 public HealthController(HealthEndpoint health) { this.health=health; }
 @GetMapping("/health")
 public ResponseEntity<Map<String,String>> health() {
  String status=health.health().getStatus().getCode();
  return ResponseEntity.status("UP".equals(status)?HttpStatus.OK:HttpStatus.SERVICE_UNAVAILABLE)
   .body(Map.of("status",status));
 }
}
