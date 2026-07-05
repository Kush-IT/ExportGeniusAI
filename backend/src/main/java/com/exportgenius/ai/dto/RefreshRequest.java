package com.exportgenius.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
