package com.hazem.worklink.dto.request;

import lombok.Data;

@Data
public class AdminSendEmailRequest {
    private String subject;
    private String body;
}
