package com.hazem.worklink.models;

import com.hazem.worklink.models.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Indexed
    private String recipientId;

    private NotificationType type;

    private String title;

    private String message;

    private String senderName;

    private String senderId;

        private boolean isRead;

    private LocalDateTime createdAt;

    private String actionUrl;
}
