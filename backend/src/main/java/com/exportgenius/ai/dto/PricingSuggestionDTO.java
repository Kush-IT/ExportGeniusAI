package com.exportgenius.ai.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingSuggestionDTO {
    private BigDecimal suggestedSellPrice;
    private BigDecimal predictedMarginPct;
    private BigDecimal marginAmount;
    private boolean isEstimated;
}
