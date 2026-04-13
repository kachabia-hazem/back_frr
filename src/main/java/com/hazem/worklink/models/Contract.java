package com.hazem.worklink.models;

import com.hazem.worklink.models.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    private String id;

    private String jobId;          // missionId
    private String freelancerId;
    private String companyId;

    // Denormalized display info
    private String freelancerName;
    private String freelancerEmail;
    private String freelancerPhoto;
    private String companyName;
    private String companyEmail;
    private String missionTitle;

    // Contract details
    private Double salary;         // TJM (daily rate)
    private LocalDate startDate;
    private LocalDate endDate;
    private String terms;

    // State
    private ContractStatus status; // PENDING_SIGNATURE | SIGNED | CANCELLED

    // Files
    private String pdfUrl;         // path to unsigned PDF
    private String signedPdfUrl;   // path to signed PDF

    // Freelancer signature
    private String signatureImageBase64;
    private LocalDateTime signedAt;

    // Company signature
    private String companySignatureImageBase64;
    private LocalDateTime companySignedAt;

    // Rejection (by freelancer)
    private LocalDateTime rejectedAt;
    private String rejectionReason;

    private LocalDateTime createdAt;
}
