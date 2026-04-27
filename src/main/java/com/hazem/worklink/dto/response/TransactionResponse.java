package com.hazem.worklink.dto.response;

import java.time.LocalDateTime;

public record TransactionResponse(
        String id,
        String type,
        String description,
        int points,
        double amount,
        LocalDateTime createdAt
) {}
