package com.hazem.worklink.dto.request;

import lombok.Data;

@Data
public class ValidateMissionRequest {
    private boolean approved;       // true = approve, false = request revision
    private String note;            // Feedback or validation note
    private Integer rating;         // 1–5 stars (required when approved)
}
