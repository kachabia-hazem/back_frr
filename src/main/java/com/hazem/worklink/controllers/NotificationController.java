package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.response.NotificationResponse;
import com.hazem.worklink.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** GET /api/notifications/my — Get all notifications for the logged-in freelancer */
    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(notificationService.getMyNotifications(email));
    }

    /** GET /api/notifications/unread-count — Number of unread notifications */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        String email = authentication.getName();
        long count = notificationService.getUnreadCount(email);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /** PATCH /api/notifications/{id}/read — Mark a single notification as read */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id, Authentication authentication) {
        String email = authentication.getName();
        notificationService.markAsRead(id, email);
        return ResponseEntity.ok().build();
    }

    /** PATCH /api/notifications/read-all — Mark all notifications as read */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String email = authentication.getName();
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok().build();
    }
}
