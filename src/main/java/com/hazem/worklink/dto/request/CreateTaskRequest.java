package com.hazem.worklink.dto.request;

import lombok.Data;

@Data
public class CreateTaskRequest {
    private String title;
    private String description;
}
