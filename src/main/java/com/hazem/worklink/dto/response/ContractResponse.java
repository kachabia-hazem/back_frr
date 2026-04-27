package com.hazem.worklink.dto.response;

import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.enums.ContractStatus;
import com.hazem.worklink.models.enums.PaymentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ContractResponse {

    private String id;
    private String jobId;
    private String freelancerId;
    private String companyId;
    private String freelancerName;
    private String freelancerEmail;
    private String companyName;
    private String companyEmail;
    private String missionTitle;
    private Double salary;
    private LocalDate startDate;
    private LocalDate endDate;
    private String terms;
    private ContractStatus status;
    private String pdfUrl;
    private String signedPdfUrl;
    private LocalDateTime signedAt;
    private LocalDateTime companySignedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime finishedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime createdAt;

    // Payment fields
    private PaymentStatus paymentStatus;
    private Double totalAmount;
    private Double platformFee;
    private Double freelancerAmount;

    public static ContractResponse from(Contract c) {
        ContractResponse r = new ContractResponse();
        r.setId(c.getId());
        r.setJobId(c.getJobId());
        r.setFreelancerId(c.getFreelancerId());
        r.setCompanyId(c.getCompanyId());
        r.setFreelancerName(c.getFreelancerName());
        r.setFreelancerEmail(c.getFreelancerEmail());
        r.setCompanyName(c.getCompanyName());
        r.setCompanyEmail(c.getCompanyEmail());
        r.setMissionTitle(c.getMissionTitle());
        r.setSalary(c.getSalary());
        r.setStartDate(c.getStartDate());
        r.setEndDate(c.getEndDate());
        r.setTerms(c.getTerms());
        r.setStatus(c.getStatus());
        r.setPdfUrl(c.getPdfUrl());
        r.setSignedPdfUrl(c.getSignedPdfUrl());
        r.setSignedAt(c.getSignedAt());
        r.setCompanySignedAt(c.getCompanySignedAt());
        r.setRejectedAt(c.getRejectedAt());
        r.setRejectionReason(c.getRejectionReason());
        r.setFinishedAt(c.getFinishedAt());
        r.setCancelledAt(c.getCancelledAt());
        r.setCancellationReason(c.getCancellationReason());
        r.setCreatedAt(c.getCreatedAt());
        r.setPaymentStatus(c.getPaymentStatus());
        r.setTotalAmount(c.getTotalAmount());
        r.setPlatformFee(c.getPlatformFee());
        r.setFreelancerAmount(c.getFreelancerAmount());
        return r;
    }
}
