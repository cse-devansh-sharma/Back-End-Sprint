package com.cap.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LoginResponseDTO {
    private String accessToken;
    private String role;
    private Long userId;
    private String fullName;
    private String email;
    private long expiresIn;
}