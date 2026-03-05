package com.hazem.worklink.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "deliverables")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Deliverable {

    @Id
    private String id;

    private String missionId;
    private String freelancerId;
    private String fileUrl;
    private String fileName;
    private String description;
    private LocalDateTime uploadedAt;
}
