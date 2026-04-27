package com.hazem.worklink.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateSubscriptionRequest {
    private String name;
    private double pricePerMonth;
    private int pointsPerMonth;
    private List<String> advantages;
    private boolean active = true;
    private int displayOrder;
}
