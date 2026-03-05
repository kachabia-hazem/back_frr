package com.hazem.worklink.models;

import com.hazem.worklink.models.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    private String id;

    private String missionId;
    private String title;
    private String description;
    private TaskStatus status = TaskStatus.TODO;
    private int orderIndex;
    private LocalDateTime createdAt;
}
