package com.hazem.worklink.dto.response;

import com.hazem.worklink.models.Application;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private String id;
    private String freelancerId;
    private String missionId;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    private String country;
    private String postalCode;
    private String city;
    private String postalAddress;

    private String cvUrl;

    private String salaryExpectations;
    private String currentSalaryAndNotice;
    private String previouslyWorked;

    private ApplicationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    // Enriched fields
    private String missionTitle;
    private String companyName;

    public static ApplicationResponse from(Application application, Mission mission, Company company) {
        ApplicationResponseBuilder builder = ApplicationResponse.builder()
                .id(application.getId())
                .freelancerId(application.getFreelancerId())
                .missionId(application.getMissionId())
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .email(application.getEmail())
                .phoneNumber(application.getPhoneNumber())
                .country(application.getCountry())
                .postalCode(application.getPostalCode())
                .city(application.getCity())
                .postalAddress(application.getPostalAddress())
                .cvUrl(application.getCvUrl())
                .salaryExpectations(application.getSalaryExpectations())
                .currentSalaryAndNotice(application.getCurrentSalaryAndNotice())
                .previouslyWorked(application.getPreviouslyWorked())
                .status(application.getStatus())
                .submittedAt(application.getSubmittedAt())
                .updatedAt(application.getUpdatedAt());

        if (mission != null) {
            builder.missionTitle(mission.getJobTitle());
        }
        if (company != null) {
            builder.companyName(company.getCompanyName());
        }

        return builder.build();
    }
}
