package com.hazem.worklink.dto.request;

import com.hazem.worklink.models.enums.LegalForm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanyRequest {

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
