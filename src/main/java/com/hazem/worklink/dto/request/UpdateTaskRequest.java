package com.hazem.worklink.dto.request;

import com.hazem.worklink.models.enums.TaskStatus;
import lombok.Data;

@Data
public class UpdateTaskRequest {
    private String title;
    private String description;
    private TaskStatus status;
}
