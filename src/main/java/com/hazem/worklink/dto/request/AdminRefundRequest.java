package com.hazem.worklink.dto.request;

import lombok.Data;

@Data
public class AdminRefundRequest {
    private Double freelancerPercentage;
    private Double companyPercentage;
    private String reason;
}
