package com.exportgenius.ai.dto.exporter;

import com.exportgenius.ai.entity.DealStage;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExporterDealDTO {
    private UUID dealId;
    private String productTitle;
    private Integer quantity;
    private BigDecimal supplyPrice;
    private BigDecimal paymentFromUs; // supplyPrice * quantity
    private LocalDateTime deliveryDeadline;
    private DealStage stage;
}
