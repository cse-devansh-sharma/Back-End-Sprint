package com.cap.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStatusDTO {

    @NotBlank(message = "Status is required")
    private String status;
}