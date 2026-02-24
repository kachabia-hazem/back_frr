package com.hazem.worklink.services;

import com.hazem.worklink.dto.response.NotificationResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.Notification;
import com.hazem.worklink.models.enums.NotificationType;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FreelancerRepository freelancerRepository;

    // ─── Create helpers ────────────────────────────────────────────────────────

    private Notification build(String recipientId, NotificationType type, String title,
                                String message, String senderName, String senderId, String actionUrl) {
        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setSenderName(senderName);
        n.setSenderId(senderId);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        n.setActionUrl(actionUrl);
        return notificationRepository.save(n);
    }

    // ─── Automatic notification triggers ───────────────────────────────────────

    /** Case 1 – Sent right after freelancer registration */
    public void sendWelcomeNotification(String freelancerId) {
        String message =
                "Welcome to Worklink!\n\n" +
                "Our platform offers a wide range of missions published by companies, allowing you to explore " +
                "and apply to opportunities that match your skills.\n\n" +
                "You can also access a directory of fellow freelancers to connect, collaborate, and grow within " +
                "the community.\n\n" +
                "Through the sidebar, you can easily view and edit your profile at any time.\n\n" +
                "Your dashboard allows you to track your applications, active missions, balance, and access many " +
                "other tools designed to help you manage your freelance activity efficiently.\n\n" +
                "We wish you every success on Worklink.";

        build(freelancerId, NotificationType.WELCOME,
                "Welcome to Worklink!",
                message,
                "WorkLink Team", null, "/freelancer-dashboard");
    }

    /** Case 2 – Sent when a freelancer submits an application */
    public void sendApplicationSubmittedNotification(String freelancerId, String missionTitle, String companyName) {
        String message = String.format(
                "Your application for the mission \"%s\" at %s has been successfully submitted. " +
                "We will notify you as soon as the company reviews your candidature. " +
                "You can track the status of all your applications in the Applications section of your dashboard.",
                missionTitle, companyName);

        build(freelancerId, NotificationType.APPLICATION_SUBMITTED,
                "Application Submitted Successfully",
                message,
                "WorkLink", null, "/freelancer-applications");
    }

    /** Case 3a – Sent when a company accepts the freelancer's application */
    public void sendApplicationAcceptedNotification(String freelancerId, String missionTitle, String companyName, String companyId) {
        String message = String.format(
                "Congratulations! Your application for the mission \"%s\" has been accepted by %s. " +
                "Please visit your Applications section to view the full details and next steps.",
                missionTitle, companyName);

        build(freelancerId, NotificationType.APPLICATION_ACCEPTED,
                "Application Accepted \uD83C\uDF89",
                message,
                companyName, companyId, "/freelancer-applications");
    }

    /** Case 3b – Sent when a company rejects the freelancer's application */
    public void sendApplicationRejectedNotification(String freelancerId, String missionTitle, String companyName, String companyId) {
        String message = String.format(
                "Thank you for your interest. Unfortunately, your application for the mission \"%s\" was not selected by %s at this time. " +
                "Don't be discouraged — there are many other opportunities waiting for you on Worklink. " +
                "Keep exploring and applying to missions that match your skills!",
                missionTitle, companyName);

        build(freelancerId, NotificationType.APPLICATION_REJECTED,
                "Application Status Updated",
                message,
                companyName, companyId, "/freelancer-applications");
    }

    /** Case 4 – Sent when a freelancer withdraws their application */
    public void sendApplicationWithdrawnNotification(String freelancerId, String missionTitle) {
        String message = String.format(
                "Your application for the mission \"%s\" has been successfully withdrawn. " +
                "You can re-apply to this mission at any time as long as it is still open.",
                missionTitle);

        build(freelancerId, NotificationType.APPLICATION_WITHDRAWN,
                "Application Withdrawn",
                message,
                "WorkLink", null, "/freelancer-applications");
    }

    /** Case 5 – Sent when a new mission matches the freelancer's skills */
    public void sendNewMissionMatchNotification(String freelancerId, String missionTitle, String companyName, String missionId) {
        String message = String.format(
                "Company \"%s\" has posted a mission that matches your profile.\n" +
                "This opportunity might interest you, as your skills and experience align well with the requirements. " +
                "Stay proactive to connect with exciting projects and grow your freelance portfolio.",
                companyName);

        build(freelancerId, NotificationType.NEW_MISSION_MATCH,
                "New Mission Match: " + missionTitle,
                message,
                companyName, null, "/missions/" + missionId);
    }

    /** Case 6 – Sent when a mission deadline is approaching (within 3 days) */
    public void sendMissionDeadlineSoonNotification(String freelancerId, String missionTitle, String deadline) {
        String message = String.format(
                "The application deadline for the mission \"%s\" is approaching on %s. " +
                "Make sure to submit your application before it's too late!",
                missionTitle, deadline);

        build(freelancerId, NotificationType.MISSION_DEADLINE_SOON,
                "Mission Deadline Approaching",
                message,
                "WorkLink", null, "/missions");
    }

    // ─── Read / query ───────────────────────────────────────────────────────────

    public List<NotificationResponse> getMyNotifications(String freelancerEmail) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + freelancerEmail));

        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(freelancer.getId())
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String freelancerEmail) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + freelancerEmail));
        return notificationRepository.countByRecipientIdAndIsReadFalse(freelancer.getId());
    }

    public void markAsRead(String notificationId, String freelancerEmail) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + freelancerEmail));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipientId().equals(freelancer.getId())) {
            throw new RuntimeException("Unauthorized: notification does not belong to this user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(String freelancerEmail) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + freelancerEmail));

        List<Notification> unread = notificationRepository.findByRecipientIdAndIsReadFalse(freelancer.getId());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
