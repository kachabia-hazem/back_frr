package com.hazem.worklink.models;

import com.hazem.worklink.models.enums.ReportStatus;
import com.hazem.worklink.models.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    private String id;

    private ReportStatus status = ReportStatus.EN_ATTENTE;
    private ReportType type;

    // Reporter (who submits the report)
    private String reportedById;
    private String reportedByRole;   // FREELANCER or COMPANY
    private String reportedByName;
    private String reportedByEmail;

    // Reported party
    private String reportedAgainstId;
    private String reportedAgainstRole;
    private String reportedAgainstName;
    private String reportedAgainstEmail;

    // Optional linked contract
    private String contractId;
    private String contractTitle;

    private String description;

    // Admin actions
    private String adminNote;
    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
