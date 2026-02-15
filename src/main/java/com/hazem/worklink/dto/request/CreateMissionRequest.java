package com.hazem.worklink.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMissionRequest {

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotBlank(message = "Field is required")
    private String field;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Mission type is required")
    private String missionType;

    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Years of experience must be 0 or more")
    private Integer yearsOfExperience;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Required skills is required")
    private String requiredSkills;

    private String technicalEnvironment;

    private LocalDate applicationDeadline;

    private String missionBusinessSector;

    private String speciality;

    @NotNull(message = "TJM is required")
    @Min(value = 0, message = "TJM must be 0 or more")
    private Double tjm;
}
