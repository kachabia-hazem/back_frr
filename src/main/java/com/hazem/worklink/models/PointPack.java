package com.hazem.worklink.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "point_packs")
public class PointPack {

    @Id
    private String id;

    private String name;
    private String category;      // DECOUVERTE | POPULAIRE | PRO
    private int points;
    private double price;         // TND
    private String badge;
    private Boolean isActive = true;
    private int displayOrder;

    // Promo
    private Boolean promoEnabled = false;
    private int promoDiscountPercent = 0;
    private String promoLabel = "";
    private LocalDateTime promoExpiresAt;
}
