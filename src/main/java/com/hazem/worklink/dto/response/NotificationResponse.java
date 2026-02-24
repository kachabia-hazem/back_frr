package com.hazem.worklink.dto.response;

import com.hazem.worklink.models.Notification;
import com.hazem.worklink.models.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private NotificationType type;
    private String title;
    private String message;
    private String senderName;
    private String senderId;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String actionUrl;

    public static NotificationResponse from(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setSenderName(notification.getSenderName());
        response.setSenderId(notification.getSenderId());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());
        response.setActionUrl(notification.getActionUrl());
        return response;
    }
}
