package com.cap.identity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ForgotPasswordDTO {

    @NotBlank
    @Email
    private String email;
}