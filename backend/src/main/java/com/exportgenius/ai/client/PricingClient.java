package com.exportgenius.ai.client;

import com.exportgenius.ai.dto.PricingSuggestionDTO;
import com.exportgenius.ai.service.PricingFallbackService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class PricingClient {

    private final RestTemplate restTemplate;
    private final PricingFallbackService pricingFallbackService;

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public PricingClient(PricingFallbackService pricingFallbackService) {
        this.restTemplate = new RestTemplate();
        this.pricingFallbackService = pricingFallbackService;
    }

    // Constructor for testing
    public PricingClient(RestTemplate restTemplate, PricingFallbackService pricingFallbackService) {
        this.restTemplate = restTemplate;
        this.pricingFallbackService = pricingFallbackService;
    }

    @CircuitBreaker(name = "pricingService", fallbackMethod = "getFallbackPricing")
    public PricingSuggestionDTO getPricingSuggestion(BigDecimal supplyPrice, String category, String destinationCountry, Integer quantity) {
        String endpoint = aiServiceUrl + "/pricing/suggest";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("supply_price", supplyPrice);
        requestBody.put("category", category);
        requestBody.put("destination_country", destinationCountry);
        requestBody.put("quantity", quantity);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<?, ?> response = restTemplate.postForObject(endpoint, entity, Map.class);
            if (response != null) {
                return PricingSuggestionDTO.builder()
                        .suggestedSellPrice(new BigDecimal(response.get("suggested_sell_price").toString()))
                        .predictedMarginPct(new BigDecimal(response.get("predicted_margin_pct").toString()))
                        .marginAmount(new BigDecimal(response.get("margin_amount").toString()))
                        .isEstimated(false)
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException("AI pricing service call failed: " + e.getMessage(), e);
        }

        throw new RuntimeException("AI pricing service returned empty response");
    }

    // Fallback method called when the circuit breaker is open or the call fails
    public PricingSuggestionDTO getFallbackPricing(BigDecimal supplyPrice, String category, String destinationCountry, Integer quantity, Throwable t) {
        System.out.println("AI Service fallback triggered. Reason: " + t.getMessage());
        return pricingFallbackService.getFallbackPrice(supplyPrice, category);
    }
}
