package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.SendMessageRequest;
import com.hazem.worklink.models.enums.Role;
import com.hazem.worklink.services.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket STOMP controller.
 * Clients publish to /app/chat/{conversationId}
 * MessageService broadcasts result to /topic/conversation/{conversationId}
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{conversationId}")
    public void handleMessage(
            @DestinationVariable String conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {

        if (principal == null) return;
        Authentication auth = (Authentication) principal;
        String email = auth.getName();
        Role role = Role.valueOf(auth.getAuthorities().iterator().next().getAuthority());
        messageService.sendMessage(conversationId, email, role, request.getContent());
    }

    /** Typing indicator — client sends {typing: true/false} */
    @MessageMapping("/chat/{conversationId}/typing")
    public void handleTyping(
            @DestinationVariable String conversationId,
            @Payload Map<String, Object> payload,
            Principal principal) {

        if (principal == null) return;
        Authentication auth = (Authentication) principal;
        Role role = Role.valueOf(auth.getAuthorities().iterator().next().getAuthority());
        String userId = messageService.resolveUserId(auth.getName(), role);
        boolean typing = Boolean.TRUE.equals(payload.get("typing"));
        messagingTemplate.convertAndSend(
            "/topic/conversation/" + conversationId + "/typing",
            Map.of("userId", userId, "typing", typing)
        );
    }
}
