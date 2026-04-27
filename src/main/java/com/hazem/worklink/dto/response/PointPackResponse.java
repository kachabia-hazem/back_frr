package com.hazem.worklink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PointPackResponse {
    private String id;
    private String name;
    private String category;
    private int points;
    private double price;
    private double pricePerPoint;
    private int savingsPercent;
    private String badge;
    private boolean active;
    private int displayOrder;

    // Promo
    private boolean promoEnabled;
    private int promoDiscountPercent;
    private String promoLabel;
    private LocalDateTime promoExpiresAt;
    private Double promoPrice;
}
