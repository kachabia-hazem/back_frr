package com.hazem.worklink.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateSubscriptionRequest {
    private String name;
    private double pricePerMonth;
    private int pointsPerMonth;
    private List<String> advantages;
    private boolean active;
    private int displayOrder;
}
