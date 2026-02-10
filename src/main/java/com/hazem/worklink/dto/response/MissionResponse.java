package com.hazem.worklink.dto.response;

import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.enums.MissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponse {

    // Mission fields
    private String id;
    private String companyId;
    private String jobTitle;
    private String field;
    private String location;
    private String missionType;
    private Integer yearsOfExperience;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String requiredSkills;
    private String technicalEnvironment;
    private Double tjm;
    private LocalDate applicationDeadline;
    private String missionBusinessSector;
    private MissionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Company fields
    private String companyName;
    private String companyLogo;
    private String businessSector;
    private String companyDescription;
    private Integer numberOfEmployees;

    public static MissionResponse from(Mission mission, Company company) {
        MissionResponseBuilder builder = MissionResponse.builder()
                .id(mission.getId())
                .companyId(mission.getCompanyId())
                .jobTitle(mission.getJobTitle())
                .field(mission.getField())
                .location(mission.getLocation())
                .missionType(mission.getMissionType())
                .yearsOfExperience(mission.getYearsOfExperience())
                .startDate(mission.getStartDate())
                .endDate(mission.getEndDate())
                .description(mission.getDescription())
                .requiredSkills(mission.getRequiredSkills())
                .technicalEnvironment(mission.getTechnicalEnvironment())
                .tjm(mission.getTjm())
                .applicationDeadline(mission.getApplicationDeadline())
                .missionBusinessSector(mission.getMissionBusinessSector())
                .status(mission.getStatus())
                .createdAt(mission.getCreatedAt())
                .updatedAt(mission.getUpdatedAt());

        if (company != null) {
            builder.companyName(company.getCompanyName())
                    .companyLogo(company.getCompanyLogo())
                    .businessSector(company.getBusinessSector())
                    .companyDescription(company.getDescription())
                    .numberOfEmployees(company.getNumberOfEmployees());
        }

        return builder.build();
    }
}
