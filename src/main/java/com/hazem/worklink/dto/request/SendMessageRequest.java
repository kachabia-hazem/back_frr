package com.hazem.worklink.dto.request;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String content;
    private String conversationId;
}
