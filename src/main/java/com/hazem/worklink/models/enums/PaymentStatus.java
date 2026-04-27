package com.hazem.worklink.models.enums;

public enum PaymentStatus {
    UNPAID,       // contract signed, payment not yet initiated
    AUTHORIZED,   // stripe holds the funds (manual capture)
    CAPTURED,     // funds captured after mission validated
    FAILED,       // payment failed
    REFUNDED      // payment refunded
}
