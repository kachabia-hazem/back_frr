package com.hazem.worklink.dto.response;

import java.util.List;

public record BalanceResponse(
        int pointsBalance,
        List<TransactionResponse> transactions
) {}
