package com.cap.timesheet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDTO {

    private Long    id;
    private String  projectCode;
    private String  name;
    private String  costCenter;
    private Boolean isBillable;
    private Boolean isActive;
}