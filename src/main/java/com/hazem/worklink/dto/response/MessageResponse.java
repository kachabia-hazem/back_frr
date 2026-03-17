package com.hazem.worklink.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderRole;
    private String content;
    private LocalDateTime timestamp;
    private boolean read;
}
