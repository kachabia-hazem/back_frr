package com.hazem.worklink.dto.request;

import lombok.Data;

@Data
public class SubmitFeedbackRequest {
    private String missionId;
    private Integer rating;  // 1–5, required
    private String comment;  // optional
}
