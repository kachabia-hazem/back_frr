package com.hazem.worklink.models;

import com.hazem.worklink.models.enums.LegitStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "legits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Legit {

    @Id
    private String id;

    private LegitStatus status = LegitStatus.EN_ATTENTE;

    // Mission context
    private String activeMissionId;
    private String contractId;
    private String missionTitle;

    // Reporter (who submitted the legit)
    private String reporterId;
    private String reporterRole;   // FREELANCER or COMPANY
    private String reporterName;
    private String reporterEmail;
    private String reporterPhone;

    // Other party
    private String otherPartyId;
    private String otherPartyRole;
    private String otherPartyName;
    private String otherPartyEmail;
    private String otherPartyPhone;

    // Legit content
    private String description;
    private Double totalAmount;
    private String resolution;
    private List<String> evidenceFiles = new ArrayList<>();

    // Admin
    private String adminNote;
    private String adminDecision;           // ANNULE | REMBOURSE | CONTINUE
    private Double freelancerRefundPercentage;
    private Double companyRefundPercentage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
