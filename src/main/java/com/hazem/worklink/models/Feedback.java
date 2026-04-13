package com.hazem.worklink.models;

import com.hazem.worklink.models.enums.FeedbackStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    private String id;

    private String missionId;
    private String missionTitle;

    private String userId;
    private String userRole; // "COMPANY" or "FREELANCER"

    private Integer rating;  // 1–5 stars
    private String comment;  // optional

    private FeedbackStatus status = FeedbackStatus.PENDING;
    private LocalDateTime createdAt;
}
