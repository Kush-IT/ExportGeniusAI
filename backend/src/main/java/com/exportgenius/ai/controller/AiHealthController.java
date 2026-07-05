package com.exportgenius.ai.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai-health")
@PreAuthorize("hasRole('ADMIN')")
public class AiHealthController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RestTemplate restTemplate;

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public AiHealthController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAiHealth() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("pricingService");
        String cbState = cb.getState().name(); // e.g. CLOSED, OPEN, HALF_OPEN

        Map<String, Object> healthReport = new HashMap<>();
        healthReport.put("circuitBreakerState", cbState);

        try {
            Map<?, ?> aiHealth = restTemplate.getForObject(aiServiceUrl + "/health", Map.class);
            healthReport.put("aiServiceStatus", aiHealth);
            healthReport.put("online", true);
        } catch (Exception e) {
            healthReport.put("aiServiceStatus", Map.of("error", e.getMessage()));
            healthReport.put("online", false);
        }

        return ResponseEntity.ok(healthReport);
    }
}
