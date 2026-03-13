package com.hazem.worklink.dto.response;

import com.hazem.worklink.models.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedApplicationResponse {

    // Application fields
    private String applicationId;
    private String freelancerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String city;
    private String country;
    private String cvUrl;
    private String salaryExpectations;
    private ApplicationStatus status;
    private LocalDateTime submittedAt;

    // Freelancer enriched fields
    private String freelancerCurrentPosition;
    private String freelancerProfilePicture;
    private List<String> freelancerSkills;
    private Integer freelancerYearsOfExperience;
    private Double freelancerRating;
    private String freelancerBio;

    // AI scores
    private int rank;
    private double totalScore;
    private double skillScore;
    private double experienceScore;
    private double semanticScore;
    private double completenessScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
}
