package com.exportgenius.ai.dto.importer;

import com.exportgenius.ai.entity.DealStage;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImporterDealDTO {
    private UUID dealId;
    private String productTitle;
    private Integer quantity;
    private BigDecimal sellPrice;
    private DealStage stage;
    private String deliveryTimeline;
    private List<String> documentsAvailable;
}
