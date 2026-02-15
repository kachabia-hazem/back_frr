package com.hazem.worklink.models;


import com.hazem.worklink.models.embedded.Certification;
import com.hazem.worklink.models.embedded.Education;
import com.hazem.worklink.models.embedded.Project;
import com.hazem.worklink.models.embedded.WorkExperience;
import com.hazem.worklink.models.enums.Gender;
import com.hazem.worklink.models.enums.ProfileType;
import com.hazem.worklink.models.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "freelancers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Freelancer extends User {

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

    private String country;

    private String postalCode;

    private String city;

    private String postalAddress;

    // Informations supplémentaires optionnelles
    private String bio;

    private String profilePicture;

    private List<String> skills;

    private String portfolioUrl;

    private String cvUrl;

    private Double rating;

    private Integer completedProjects;

    // Card customization
    private String cardBackground;

    private List<String> portfolioImages = new ArrayList<>();

    // CV Data
    private List<Education> education = new ArrayList<>();

    private List<Project> projects = new ArrayList<>();

    private List<Certification> certifications = new ArrayList<>();

    private List<WorkExperience> workExperience = new ArrayList<>();
}