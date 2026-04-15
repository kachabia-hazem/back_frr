package com.hazem.worklink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackPublicDto {
    private String id;
    private String userRole;   // "COMPANY" or "FREELANCER"
    private String userName;   // firstName + lastName or companyName
    private String userPhoto;  // profilePicture or companyLogo (relative path)
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
