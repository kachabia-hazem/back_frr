package com.hazem.worklink.dto.request;

import lombok.Data;

@Data
public class CreatePackRequest {
    private String name;
    private String category;  // DECOUVERTE | POPULAIRE | PRO
    private int points;
    private double price;
    private String badge;
    private boolean active = true;
    private int displayOrder;
}
