package com.exportgenius.ai.dto.admin;

import com.exportgenius.ai.entity.DealStage;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDealDTO {
    private UUID dealId;
    private UUID requirementId;
    private UUID catalogueId;
    private UUID exporterId;
    private String exporterName;
    private String exporterCountry;
    private UUID importerId;
    private String importerName;
    private String importerCountry;
    private String productTitle;
    private Integer quantity;
    private BigDecimal supplyPrice;
    private BigDecimal sellPrice;
    private BigDecimal marginAmount;
    private BigDecimal marginPct;
    private DealStage stage;
    private boolean importerAccepted;
    private LocalDateTime deliveryDeadline;
    private String shippingDocumentUrl;
    private BigDecimal reliabilityScore;
    private List<String> auditTrail;
}
