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
public class CatalogueRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Category ID is required")
    private Integer categoryId;

    @NotBlank(message = "HS Code is required")
    private String hsCode;

    @NotNull(message = "Supply price is required")
    @Positive(message = "Supply price must be positive")
    private BigDecimal supplyPrice;

    @Builder.Default
    private String currency = "USD";

    @NotNull(message = "Minimum Order Quantity (MOQ) is required")
    @Positive(message = "MOQ must be positive")
    private Integer moq;

    @NotNull(message = "Lead time in days is required")
    @Positive(message = "Lead time must be positive")
    private Integer leadTimeDays;

    private String productionCapacity;
}
