package com.hazem.worklink.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "subscription_plans")
public class SubscriptionPlan {

    @Id
    private String id;

    private String name;
    private double pricePerMonth;   // TND
    private int pointsPerMonth;
    private List<String> advantages;
    private Boolean isActive = true;
    private int displayOrder;

    // Promo
    private Boolean promoEnabled = false;
    private int promoDiscountPercent = 0;
    private String promoLabel = "";
    private LocalDateTime promoExpiresAt;
}
