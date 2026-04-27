package com.hazem.worklink.dto.request;

import lombok.Data;

@Data
public class UpdatePackRequest {
    private String name;
    private int points;
    private double price;
    private String badge;
    private boolean active;
    private int displayOrder;
}
