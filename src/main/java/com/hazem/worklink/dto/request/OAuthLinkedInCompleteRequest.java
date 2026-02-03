package com.hazem.worklink.dto.request;

import com.hazem.worklink.models.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLinkedInCompleteRequest {

    @NotNull(message = "Le rôle est requis")
    private Role role;

    // LinkedIn profile data
    @NotBlank(message = "L'email est requis")
    private String email;

    private String linkedInId;

    // ── Freelancer fields ──
    private String firstName;
    private String lastName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private Integer yearsOfExperience;
    private List<ProfileType> profileTypes;
    private Double tjm;
    private List<Language> languages;
    private String currentPosition;
    private String bio;
    private List<String> skills;
    private String portfolioUrl;
    private String profilePicture;

    // ── Company fields ──
    private String companyName;
    private String address;
    private String websiteUrl;
    private LegalForm legalForm;
    private String tradeRegister;
    private LocalDate foundationDate;
    private String businessSector;
    private String managerName;
    private String managerEmail;
    private String managerPosition;
    private String managerPhoneNumber;
    private String description;
    private Integer numberOfEmployees;
}
