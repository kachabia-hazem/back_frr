package com.hazem.worklink.config;

import com.hazem.worklink.services.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = sha.getSessionAttributes();
        if (attrs == null) return;
        String email = (String) attrs.get("wsEmail");
        String role  = (String) attrs.get("wsRole");
        if (email != null && role != null) {
            presenceService.markOnline(email, role);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = sha.getSessionAttributes();
        if (attrs == null) return;
        String email = (String) attrs.get("wsEmail");
        String role  = (String) attrs.get("wsRole");
        if (email != null && role != null) {
            presenceService.markOffline(email, role);
        }
    }
}
