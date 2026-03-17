package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.SendMessageRequest;
import com.hazem.worklink.dto.response.ConversationResponse;
import com.hazem.worklink.dto.response.MessageResponse;
import com.hazem.worklink.models.enums.Role;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.services.MessageService;
import com.hazem.worklink.services.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final PresenceService presenceService;
    private final FreelancerRepository freelancerRepository;

    /**
     * GET /api/messages/search-freelancers?q=name — Company searches freelancers by name.
     * Returns lightweight list: id, firstName, lastName, profilePicture, currentPosition.
     */
    @GetMapping("/search-freelancers")
    public ResponseEntity<List<Map<String, String>>> searchFreelancers(
            @RequestParam(defaultValue = "") String q,
            Authentication auth) {
        Role role = Role.valueOf(auth.getAuthorities().iterator().next().getAuthority());
        if (role != Role.COMPANY) return ResponseEntity.status(403).build();

        String query = q.toLowerCase().trim();
        List<Map<String, String>> results = freelancerRepository.findAll().stream()
                .filter(f -> {
                    String fullName = ((f.getFirstName() != null ? f.getFirstName() : "") + " "
                            + (f.getLastName() != null ? f.getLastName() : "")).toLowerCase();
                    return query.isEmpty() || fullName.contains(query);
                })
                .limit(10)
                .map(f -> Map.of(
                        "id",              f.getId(),
                        "firstName",       f.getFirstName() != null ? f.getFirstName() : "",
                        "lastName",        f.getLastName()  != null ? f.getLastName()  : "",
                        "profilePicture",  f.getProfilePicture() != null ? f.getProfilePicture() : "",
                        "currentPosition", f.getCurrentPosition() != null ? f.getCurrentPosition() : ""
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/messages/conversations/{id}/read — Mark conversation as read (called in real-time when a message is received).
     */
    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String conversationId,
            Authentication auth) {
        String email = auth.getName();
        Role role = Role.valueOf(auth.getAuthorities().iterator().next().getAuthority());
        String userId = messageService.resolveUserId(email, role);
        messageService.markAsRead(conversationId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/messages/presence/{userId} — Check if a user is online and their last seen time.
     */
    @GetMapping("/presence/{userId}")
    public ResponseEntity<Map<String, Object>> getPresence(@PathVariable String userId) {
        boolean online = presenceService.isOnline(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("online", online);
        if (!online) {
            LocalDateTime lastSeen = presenceService.getLastSeen(userId);
            result.put("lastSeen", lastSeen != null ? lastSeen.toString() : null);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/messages/conversations — Company initiates or retrieves a conversation.
     * Body: { "freelancerId": "..." }
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> getOrCreateConversation(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String email = auth.getName();
        Role role = Role.valueOf(auth.getAuthorities().iterator().next().getAuthority());
        if (role != Role.COMPANY) {
            return ResponseEntity.status(403).build();
        }
        String freelancerId = body.get("freelancerId");
        return ResponseEntity.ok(messageService.getOrCreateConversation(email, freelancerId));
    }

    /**
     * GET /api/messages/conversations — All conversations for the current user.
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(Authentication auth) {
        String email = auth.getName();
        Role role = Role.valueOf(auth.getAuthorities().iterator().next().getAuthority());
        return ResponseEntity.ok(messageService.getConversations(email, role));
    }

    /**
     * GET /api/messages/conversations/{id}/messages — Get messages for a conversation.
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String conversationId,
            Authentication auth) {
        String email = auth.getName();
        Role role = Role.valueOf(auth.getAuthorities().iterator().next().getAuthority());
        return ResponseEntity.ok(messageService.getMessages(conversationId, email, role));
    }

    /**
     * POST /api/messages/conversations/{id}/send — Send a message (REST fallback).
     * Primary: WebSocket via /app/chat/{conversationId}
     */
    @PostMapping("/conversations/{conversationId}/send")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessageRequest request,
            Authentication auth) {
        String email = auth.getName();
        Role role = Role.valueOf(auth.getAuthorities().iterator().next().getAuthority());
        return ResponseEntity.ok(messageService.sendMessage(conversationId, email, role, request.getContent()));
    }
}
