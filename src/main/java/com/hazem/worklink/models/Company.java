package com.hazem.worklink.models;


import com.hazem.worklink.models.enums.LegalForm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Company extends User {

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

    // Informations supplémentaires optionnelles
    private String companyLogo;

    private String description;

    private Integer numberOfEmployees;



    private Integer postedProjects;
}
