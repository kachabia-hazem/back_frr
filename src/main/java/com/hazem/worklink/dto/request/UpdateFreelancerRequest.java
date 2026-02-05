package com.hazem.worklink.dto.request;

import com.hazem.worklink.models.enums.Gender;
import com.hazem.worklink.models.enums.Language;
import com.hazem.worklink.models.enums.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFreelancerRequest {

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
    private String location;
    private String bio;
    private List<String> skills;
    private String portfolioUrl;
}
