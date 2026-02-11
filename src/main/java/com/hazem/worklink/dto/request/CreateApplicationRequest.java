package com.hazem.worklink.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationRequest {

    @NotBlank(message = "Mission ID is required")
    private String missionId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Country is required")
    private String country;

    private String postalCode;
    private String city;
    private String postalAddress;

    private String cvUrl;

    @NotBlank(message = "Salary expectations is required")
    private String salaryExpectations;

    @NotBlank(message = "Current salary and notice period is required")
    private String currentSalaryAndNotice;

    @NotBlank(message = "Previously worked answer is required")
    private String previouslyWorked;

    private String previousWorkDate;
    private String previousWorkExperience;
}
