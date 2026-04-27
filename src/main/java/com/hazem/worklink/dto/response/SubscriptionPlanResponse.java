package com.hazem.worklink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class SubscriptionPlanResponse {
    private String id;
    private String name;
    private double pricePerMonth;
    private int pointsPerMonth;
    private List<String> advantages;
    private boolean active;
    private int displayOrder;

    // Promo
    private boolean promoEnabled;
    private int promoDiscountPercent;
    private String promoLabel;
    private LocalDateTime promoExpiresAt;
    private Double promoPrice;
}
