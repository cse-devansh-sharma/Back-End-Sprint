package com.cap.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPasswordResponseDTO {
    private String message;
    private String token; // only populated in dev mode, null in prod
}
