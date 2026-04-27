package com.hazem.worklink.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdatePromoRequest {
    private boolean promoEnabled;
    private int promoDiscountPercent;
    private String promoLabel;
    private LocalDateTime promoExpiresAt;
}
