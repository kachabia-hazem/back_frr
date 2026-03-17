package com.hazem.worklink.services;

import com.hazem.worklink.dto.response.ConversationResponse;
import com.hazem.worklink.dto.response.MessageResponse;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Conversation;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.Message;
import com.hazem.worklink.models.enums.ContractStatus;
import com.hazem.worklink.models.enums.Role;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.ContractRepository;
import com.hazem.worklink.repositories.ConversationRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ContractRepository contractRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ──────────────────────────────────────────────────────────────────────────
    // Conversation management
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called automatically when a contract is signed — ensures a conversation exists.
     * Uses entity IDs directly (no email lookup needed).
     */
    public void ensureConversationFromContract(String companyId, String freelancerId) {
        if (conversationRepository.findByCompanyIdAndFreelancerId(companyId, freelancerId).isPresent()) {
            return; // already exists
        }
        Company company = companyRepository.findById(companyId).orElse(null);
        Freelancer freelancer = freelancerRepository.findById(freelancerId).orElse(null);
        if (company == null || freelancer == null) return;

        Conversation conv = Conversation.builder()
                .companyId(companyId)
                .companyName(company.getCompanyName())
                .companyLogo(company.getCompanyLogo())
                .freelancerId(freelancerId)
                .freelancerName(freelancer.getFirstName() + " " + freelancer.getLastName())
                .freelancerPicture(freelancer.getProfilePicture())
                .lastMessageTime(LocalDateTime.now())
                .build();
        conversationRepository.save(conv);
    }

    /**
     * Company-only: create or retrieve an existing conversation with a freelancer.
     */
    public ConversationResponse getOrCreateConversation(String companyEmail, String freelancerId) {
        Company company = companyRepository.findByEmail(companyEmail)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        return conversationRepository.findByCompanyIdAndFreelancerId(company.getId(), freelancerId)
                .map(conv -> toResponse(conv, company.getId()))
                .orElseGet(() -> {
                    Freelancer freelancer = freelancerRepository.findById(freelancerId)
                            .orElseThrow(() -> new RuntimeException("Freelancer not found"));

                    Conversation conv = Conversation.builder()
                            .companyId(company.getId())
                            .companyName(company.getCompanyName())
                            .companyLogo(company.getCompanyLogo())
                            .freelancerId(freelancerId)
                            .freelancerName(freelancer.getFirstName() + " " + freelancer.getLastName())
                            .freelancerPicture(freelancer.getProfilePicture())
                            .build();

                    return toResponse(conversationRepository.save(conv), company.getId());
                });
    }

    /**
     * Get all conversations for the current user.
     * - Company → sees all its conversations
     * - Freelancer → sees only conversations where a SIGNED contract exists
     */
    public List<ConversationResponse> getConversations(String email, Role role) {
        if (role == Role.COMPANY) {
            Company company = companyRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Company not found"));
            return conversationRepository.findByCompanyId(company.getId())
                    .stream()
                    .sorted(Comparator.comparing(
                            conv -> conv.getLastMessageTime() != null ? conv.getLastMessageTime() : conv.getCreatedAt(),
                            Comparator.reverseOrder()))
                    .map(conv -> toResponse(conv, company.getId()))
                    .collect(Collectors.toList());
        } else {
            Freelancer freelancer = freelancerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));
            return conversationRepository.findByFreelancerId(freelancer.getId())
                    .stream()
                    .filter(conv -> hasSignedContract(conv.getCompanyId(), freelancer.getId()))
                    .sorted(Comparator.comparing(
                            conv -> conv.getLastMessageTime() != null ? conv.getLastMessageTime() : conv.getCreatedAt(),
                            Comparator.reverseOrder()))
                    .map(conv -> toResponse(conv, freelancer.getId()))
                    .collect(Collectors.toList());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Messages
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Load all messages for a conversation (marks them as read for the caller).
     */
    public List<MessageResponse> getMessages(String conversationId, String email, Role role) {
        String userId = resolveUserId(email, role);
        validateAccess(conversationId, userId, role);
        markAsRead(conversationId, userId);
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Send a message (called by REST controller or WebSocket handler).
     * Saves to DB and broadcasts via WebSocket to all topic subscribers.
     */
    public MessageResponse sendMessage(String conversationId, String senderEmail, Role senderRole, String content) {
        String senderId = resolveUserId(senderEmail, senderRole);
        Conversation conv = validateAccess(conversationId, senderId, senderRole);

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .senderRole(senderRole.name())
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        message = messageRepository.save(message);

        // Update conversation meta
        conv.setLastMessage(content);
        conv.setLastMessageTime(message.getTimestamp());
        String otherPartyId = senderRole == Role.COMPANY ? conv.getFreelancerId() : conv.getCompanyId();
        conv.getUnreadCount().merge(otherPartyId, 1, Integer::sum);
        conversationRepository.save(conv);

        MessageResponse response = toMessageResponse(message);
        // Push to all WebSocket subscribers of this conversation
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, response);
        return response;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Conversation validateAccess(String conversationId, String userId, Role role) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (role == Role.COMPANY) {
            if (!conv.getCompanyId().equals(userId)) {
                throw new RuntimeException("Access denied");
            }
        } else if (role == Role.FREELANCER) {
            if (!conv.getFreelancerId().equals(userId)) {
                throw new RuntimeException("Access denied");
            }
            if (!hasSignedContract(conv.getCompanyId(), userId)) {
                throw new RuntimeException("No active contract for this conversation");
            }
        }
        return conv;
    }

    public void markAsRead(String conversationId, String userId) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            conv.getUnreadCount().put(userId, 0);
            conversationRepository.save(conv);
        });
        // Notify the sender that their messages have been read
        messagingTemplate.convertAndSend(
            "/topic/conversation/" + conversationId + "/read",
            java.util.Map.of("readerId", userId, "conversationId", conversationId)
        );
    }

    private boolean hasSignedContract(String companyId, String freelancerId) {
        return contractRepository.existsByCompanyIdAndFreelancerIdAndStatus(
                companyId, freelancerId, ContractStatus.SIGNED);
    }

    public String resolveUserId(String email, Role role) {
        if (role == Role.COMPANY) {
            return companyRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Company not found"))
                    .getId();
        }
        return freelancerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"))
                .getId();
    }

    private ConversationResponse toResponse(Conversation conv, String currentUserId) {
        return ConversationResponse.builder()
                .id(conv.getId())
                .companyId(conv.getCompanyId())
                .companyName(conv.getCompanyName())
                .companyLogo(conv.getCompanyLogo())
                .freelancerId(conv.getFreelancerId())
                .freelancerName(conv.getFreelancerName())
                .freelancerPicture(conv.getFreelancerPicture())
                .lastMessage(conv.getLastMessage())
                .lastMessageTime(conv.getLastMessageTime())
                .unreadCount(conv.getUnreadCount())
                .hasContract(hasSignedContract(conv.getCompanyId(), conv.getFreelancerId()))
                .build();
    }

    private MessageResponse toMessageResponse(Message msg) {
        return MessageResponse.builder()
                .id(msg.getId())
                .conversationId(msg.getConversationId())
                .senderId(msg.getSenderId())
                .senderRole(msg.getSenderRole())
                .content(msg.getContent())
                .timestamp(msg.getTimestamp())
                .read(msg.isRead())
                .build();
    }
}
