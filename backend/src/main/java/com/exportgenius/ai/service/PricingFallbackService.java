package com.exportgenius.ai.service;

import com.exportgenius.ai.dto.PricingSuggestionDTO;
import com.exportgenius.ai.repository.MarginLedgerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class PricingFallbackService {

    private final MarginLedgerRepository marginLedgerRepository;

    public PricingFallbackService(MarginLedgerRepository marginLedgerRepository) {
        this.marginLedgerRepository = marginLedgerRepository;
    }

    public PricingSuggestionDTO getFallbackPrice(BigDecimal supplyPrice, String categoryName) {
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        Double avgMarginVal = marginLedgerRepository.getAverageMarginPctByCategoryAndDate(categoryName, ninetyDaysAgo);
        
        BigDecimal marginPct;
        if (avgMarginVal == null || avgMarginVal <= 0.0 || avgMarginVal >= 1.0) {
            marginPct = BigDecimal.valueOf(0.25); // Default flat 25% margin
        } else {
            marginPct = BigDecimal.valueOf(avgMarginVal);
        }

        // suggested_sell_price = supply_price / (1 - margin_pct)
        BigDecimal divisor = BigDecimal.ONE.subtract(marginPct);
        BigDecimal suggestedSellPrice = supplyPrice.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal marginAmount = suggestedSellPrice.subtract(supplyPrice);

        return PricingSuggestionDTO.builder()
                .suggestedSellPrice(suggestedSellPrice)
                .predictedMarginPct(marginPct)
                .marginAmount(marginAmount)
                .isEstimated(true)
                .build();
    }
}
