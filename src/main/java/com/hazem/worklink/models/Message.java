package com.hazem.worklink.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    private String senderId;
    private String senderRole; // "COMPANY" or "FREELANCER"
    private String content;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private boolean read = false;
}
