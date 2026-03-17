package com.hazem.worklink.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "company_freelancer_unique", def = "{'companyId': 1, 'freelancerId': 1}", unique = true)
public class Conversation {

    @Id
    private String id;

    private String companyId;
    private String companyName;
    private String companyLogo;

    private String freelancerId;
    private String freelancerName;
    private String freelancerPicture;

    private String lastMessage;
    private LocalDateTime lastMessageTime;

    @Builder.Default
    private Map<String, Integer> unreadCount = new HashMap<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
