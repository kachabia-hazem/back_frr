package com.hazem.worklink.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    private String id;

    private String missionId;
    private String freelancerId;
    private String companyId;
    private String companyName;
    private String companyLogo;

    private Integer rating;   // 1–5 étoiles
    private String comment;

    private LocalDateTime createdAt;
}
