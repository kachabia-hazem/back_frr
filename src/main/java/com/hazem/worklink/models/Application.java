package com.hazem.worklink.models;

import com.hazem.worklink.models.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    private String id;

    private String freelancerId;
    private String missionId;

    // Contact Information
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    // Location Details
    private String country;
    private String postalCode;
    private String city;
    private String postalAddress;

    // CV
    private String cvUrl;

    // Security Questions
    private String salaryExpectations;
    private String currentSalaryAndNotice;
    private String previouslyWorked;
    private String previousWorkDate;
    private String previousWorkExperience;

    private ApplicationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
