package com.hazem.worklink.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("point_transactions")
@Data
public class PointTransaction {
    @Id private String id;
    private String userId;
    private String type; // PURCHASE_PACK | SUBSCRIBE_PLAN | APPLICATION | AI_MATCHING | BOOST | FEATURED
    private String referenceId; // packId or planId
    private int points; // positive = credit, negative = debit
    private double amount; // price paid in DT (0 for debit)
    private String description;
    private LocalDateTime createdAt;
}
