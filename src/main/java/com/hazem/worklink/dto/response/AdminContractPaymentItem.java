package com.hazem.worklink.dto.response;

import com.hazem.worklink.models.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractPaymentItem {
    private String id;
    private String freelancerName;
    private String freelancerEmail;
    private String companyName;
    private String companyEmail;
    private String missionTitle;
    private Double totalAmount;
    private Double platformFee;
    private Double freelancerAmount;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime capturedAt;
}
