package com.cap.identity.dto;

import lombok.*;

@Data
@Builder
public class UserProfileDTO {

    private Long id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String role;
    private String status;
}