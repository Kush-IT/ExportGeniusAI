package com.exportgenius.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementRequest {
    @NotBlank(message = "Product type is required")
    private String productType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private String qualityStandard;

    @Positive(message = "Target price must be positive")
    private BigDecimal targetPrice;

    @NotBlank(message = "Destination country is required")
    private String destinationCountry;

    private String timeline;
}
