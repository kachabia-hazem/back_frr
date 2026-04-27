package com.hazem.worklink.dto.response;

import java.time.LocalDateTime;

public record CompanySubscriptionResponse(
        boolean active,
        int pointsBalance,
        SubscriptionPlanResponse plan,
        LocalDateTime subscribedAt,
        LocalDateTime expiresAt
) {}
