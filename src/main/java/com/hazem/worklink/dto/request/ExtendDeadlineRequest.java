package com.hazem.worklink.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExtendDeadlineRequest {
    private LocalDate newEndDate;          // required
    private Double adjustedPayment;        // optional — defaults to current salary
    private String reason;                 // optional but recommended
}
