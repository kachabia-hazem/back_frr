package com.hazem.worklink.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private String id;
    private String companyId;
    private String companyName;
    private String companyLogo;
    private String freelancerId;
    private String freelancerName;
    private String freelancerPicture;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Map<String, Integer> unreadCount;
    private boolean hasContract;
}
