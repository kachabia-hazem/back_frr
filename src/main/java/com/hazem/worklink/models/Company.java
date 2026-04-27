package com.hazem.worklink.models;


import com.hazem.worklink.models.enums.CompanyStatus;
import com.hazem.worklink.models.enums.LegalForm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // ── Admin Verification ───────────────────────────────────────────────────
    private CompanyStatus verificationStatus = CompanyStatus.PENDING;

    private String rejectionReason;

    private LocalDateTime verifiedAt;

    private Integer trustScore;

    // ── Points & Subscription ─────────────────────────────────────────────────
    private int pointsBalance = 0;

    private String subscriptionPlanId;

    private LocalDateTime subscriptionStartDate;

    private LocalDateTime subscriptionExpiresAt;
}
