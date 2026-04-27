package com.hazem.worklink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPointTransactionItem {
    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String type;
    private int points;
    private double amount;
    private String description;
    private LocalDateTime createdAt;
}
