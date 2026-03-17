package com.hazem.worklink.services;

import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /** userId -> true (online) */
    private final ConcurrentHashMap<String, Boolean> onlineUsers = new ConcurrentHashMap<>();

    public void markOnline(String email, String role) {
        String userId = resolveUserId(email, role);
        if (userId == null) return;
        onlineUsers.put(userId, Boolean.TRUE);
        broadcast(userId, true, null);
    }

    public void markOffline(String email, String role) {
        String userId = resolveUserId(email, role);
        if (userId == null) return;
        onlineUsers.remove(userId);
        LocalDateTime lastSeen = LocalDateTime.now();
        saveLastSeen(email, role, lastSeen);
        broadcast(userId, false, lastSeen);
    }

    public boolean isOnline(String userId) {
        return onlineUsers.containsKey(userId);
    }

    public LocalDateTime getLastSeen(String userId) {
        // Try freelancer first, then company
        var fl = freelancerRepository.findById(userId);
        if (fl.isPresent()) return fl.get().getLastSeen();
        var co = companyRepository.findById(userId);
        return co.map(c -> c.getLastSeen()).orElse(null);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void broadcast(String userId, boolean online, LocalDateTime lastSeen) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("online", online);
        if (lastSeen != null) payload.put("lastSeen", lastSeen.toString());
        messagingTemplate.convertAndSend("/topic/presence", payload);
    }

    private String resolveUserId(String email, String role) {
        if ("COMPANY".equals(role)) {
            return companyRepository.findByEmail(email).map(c -> c.getId()).orElse(null);
        }
        return freelancerRepository.findByEmail(email).map(f -> f.getId()).orElse(null);
    }

    private void saveLastSeen(String email, String role, LocalDateTime lastSeen) {
        if ("COMPANY".equals(role)) {
            companyRepository.findByEmail(email).ifPresent(c -> {
                c.setLastSeen(lastSeen);
                companyRepository.save(c);
            });
        } else {
            freelancerRepository.findByEmail(email).ifPresent(f -> {
                f.setLastSeen(lastSeen);
                freelancerRepository.save(f);
            });
        }
    }
}
