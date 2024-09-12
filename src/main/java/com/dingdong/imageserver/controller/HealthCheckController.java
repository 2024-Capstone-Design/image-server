package com.dingdong.imageserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/actuator/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Health check OK");
    }
}
