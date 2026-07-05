package com.exportgenius.ai;

import com.exportgenius.ai.client.PricingClient;
import com.exportgenius.ai.dto.PricingSuggestionDTO;
import com.exportgenius.ai.repository.MarginLedgerRepository;
import com.exportgenius.ai.service.PricingFallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class ResilienceFallbackTest {

    private MarginLedgerRepository marginLedgerRepository;
    private PricingFallbackService pricingFallbackService;
    private RestTemplate restTemplate;
    private PricingClient pricingClient;

    @BeforeEach
    public void setUp() {
        marginLedgerRepository = Mockito.mock(MarginLedgerRepository.class);
        pricingFallbackService = new PricingFallbackService(marginLedgerRepository);
        restTemplate = Mockito.mock(RestTemplate.class);
        pricingClient = new PricingClient(restTemplate, pricingFallbackService);
    }

    @Test
    public void testClientFallbackWhenServiceIsUnreachable() {
        // Mock restTemplate throwing ResourceAccessException (connection refused)
        when(restTemplate.postForObject(anyString(), any(), any()))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // Mock historical average margin pct in DB is 30% (0.30)
        when(marginLedgerRepository.getAverageMarginPctByCategoryAndDate(eq("Textiles"), any(LocalDateTime.class)))
                .thenReturn(0.30);

        // Call the fallback method directly to test the resilience contract logic
        PricingSuggestionDTO suggestion = pricingClient.getFallbackPricing(
                BigDecimal.valueOf(100.00), "Textiles", "Germany", 50, new RuntimeException("Service down")
        );

        assertNotNull(suggestion);
        assertTrue(suggestion.isEstimated());
        assertEquals(BigDecimal.valueOf(0.30), suggestion.getPredictedMarginPct());
        // suggestedPrice = 100 / (1 - 0.30) = 142.86
        assertEquals(BigDecimal.valueOf(142.86), suggestion.getSuggestedSellPrice());
        assertEquals(BigDecimal.valueOf(42.86), suggestion.getMarginAmount());
    }

    @Test
    public void testColdStartDefaultMarginFallback() {
        // Mock historical average margin is null (cold start)
        when(marginLedgerRepository.getAverageMarginPctByCategoryAndDate(anyString(), any(LocalDateTime.class)))
                .thenReturn(null);

        PricingSuggestionDTO suggestion = pricingFallbackService.getFallbackPrice(
                BigDecimal.valueOf(10.00), "UnknownCategory"
        );

        assertNotNull(suggestion);
        assertTrue(suggestion.isEstimated());
        assertEquals(BigDecimal.valueOf(0.25), suggestion.getPredictedMarginPct()); // 25% default
        // suggestedPrice = 10 / (1 - 0.25) = 13.33
        assertEquals(BigDecimal.valueOf(13.33), suggestion.getSuggestedSellPrice());
        assertEquals(BigDecimal.valueOf(3.33), suggestion.getMarginAmount());
    }
}
