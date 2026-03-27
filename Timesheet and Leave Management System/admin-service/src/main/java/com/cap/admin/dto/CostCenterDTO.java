package com.cap.admin.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CostCenterDTO {
    private Long    id;
    private String  code;
    private String  name;
    private Boolean isActive;
}
