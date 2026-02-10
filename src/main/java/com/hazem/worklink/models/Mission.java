package com.hazem.worklink.models;

import com.hazem.worklink.models.enums.MissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "missions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mission {

    @Id
    private String id;

    private String companyId;

    // Basic Offer Information
    private String jobTitle;
    private String field;
    private String location;

    // Working Conditions
    private String missionType;
    private Integer yearsOfExperience;
    private LocalDate startDate;
    private LocalDate endDate;

    // Job Description
    private String description;

    // Required Skills & Technical Environment
    private String requiredSkills;
    private String technicalEnvironment;

    // Additional
    private LocalDate applicationDeadline;
    private String missionBusinessSector;

    // Budget
    private Double tjm;

    private MissionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
