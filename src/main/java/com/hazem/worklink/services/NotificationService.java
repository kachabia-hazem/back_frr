package com.hazem.worklink.services;

import com.hazem.worklink.dto.response.NotificationResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.Notification;
import com.hazem.worklink.models.enums.NotificationType;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;

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
                "Your application for the mission \"%s\" at %s has been successfully submitted.\n" +
                "We will notify you as soon as the company reviews your candidature.\n" +
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
                "Congratulations!\n" +
                "Your application for the mission \"%s\" has been accepted by %s.\n" +
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
                "Thank you for your interest.\n" +
                "Unfortunately, your application for the mission \"%s\" was not selected by %s at this time.\n" +
                "Don't be discouraged — there are many other opportunities waiting for you on Worklink.\n" +
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
                "Your application for the mission \"%s\" has been successfully withdrawn.\n" +
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
                "This opportunity might interest you, as your skills and experience align well with the requirements.\n" +
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
                "The application deadline for the mission \"%s\" is approaching on %s.\n" +
                "Make sure to submit your application before it's too late!",
                missionTitle, deadline);

        build(freelancerId, NotificationType.MISSION_DEADLINE_SOON,
                "Mission Deadline Approaching",
                message,
                "WorkLink", null, "/missions");
    }

    // ─── Company notification triggers ──────────────────────────────────────────

    /** Company Case 1 – Sent right after company registration */
    public void sendCompanyWelcomeNotification(String companyId) {
        String message =
                "Welcome to Worklink!\n\n" +
                "Our platform offers the ability to publish missions and review applications received from freelancers.\n" +
                "You will also find a directory of freelancers, enabling you to identify and connect with qualified professionals.\n\n" +
                "Using the sidebar, you can view and edit your company profile at any time.\n" +
                "From your dashboard, you can track received applications, published missions, balance, and access many other tools designed to help you manage your projects efficiently.\n\n" +
                "We look forward to supporting your success on Worklink.";

        build(companyId, NotificationType.COMPANY_WELCOME,
                "Welcome to Worklink!",
                message,
                "WorkLink Team", null, "/company-dashboard");
    }

    /** Company Case 2 – Sent when a company successfully publishes a mission */
    public void sendMissionPublishedNotification(String companyId, String missionTitle, String missionId) {
        String message = String.format(
                "Your mission \"%s\" has been successfully published on WorkLink.\n" +
                "Freelancers matching your requirements will be notified automatically.\n" +
                "You can track received applications from your Applications section.",
                missionTitle);

        build(companyId, NotificationType.MISSION_PUBLISHED,
                "Mission Published Successfully",
                message,
                "WorkLink", null, "/company-missions");
    }

    /** Company Case 3 – Sent when a freelancer applies to one of the company's missions */
    public void sendApplicationReceivedNotification(String companyId, String missionTitle, String freelancerName) {
        String message = String.format(
                "%s has submitted an application for your mission \"%s\".\n" +
                "Review the candidate's profile and application details in your Applications section.\n" +
                "You can accept or reject the application at any time.",
                freelancerName, missionTitle);

        build(companyId, NotificationType.APPLICATION_RECEIVED,
                "New Application Received",
                message,
                freelancerName, null, "/company-applications");
    }

    /** Company Case 4 – Reminder sent when pending applications have not been reviewed */
    public void sendPendingApplicationsReminderNotification(String companyId, String missionTitle, int pendingCount, String missionId) {
        String message = String.format(
                "You have %d pending application%s for your mission \"%s\" that have not been reviewed yet.\n" +
                "Timely responses help freelancers plan their work and improve your company's reputation on WorkLink.\n" +
                "Please review the applications in your Applications section.",
                pendingCount, pendingCount > 1 ? "s" : "", missionTitle);

        build(companyId, NotificationType.PENDING_APPLICATIONS_REMINDER,
                "Pending Applications Awaiting Review",
                message,
                "WorkLink", null, "/company-applications");
    }

    /** Company Case 5 – Sent when a mission is closed */
    public void sendMissionClosedNotification(String companyId, String missionTitle) {
        String message = String.format(
                "Your mission \"%s\" has been closed. No further applications will be accepted.\n" +
                "You can review all received applications and make your final decisions in the Applications section.\n" +
                "Thank you for using WorkLink to manage your recruitment.",
                missionTitle);

        build(companyId, NotificationType.MISSION_CLOSED,
                "Mission Closed",
                message,
                "WorkLink", null, "/company-missions");
    }

    // ─── Read / query (supports both freelancers and companies) ─────────────────

    /** Resolves the MongoDB document ID for any registered user (freelancer or company). */
    private String resolveRecipientId(String email) {
        Optional<Freelancer> freelancer = freelancerRepository.findByEmail(email);
        if (freelancer.isPresent()) return freelancer.get().getId();

        Optional<Company> company = companyRepository.findByEmail(email);
        if (company.isPresent()) return company.get().getId();

        throw new ResourceNotFoundException("User not found with email: " + email);
    }

    public List<NotificationResponse> getMyNotifications(String email) {
        String recipientId = resolveRecipientId(email);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String email) {
        String recipientId = resolveRecipientId(email);
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    public void markAsRead(String notificationId, String email) {
        String recipientId = resolveRecipientId(email);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipientId().equals(recipientId)) {
            throw new RuntimeException("Unauthorized: notification does not belong to this user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(String email) {
        String recipientId = resolveRecipientId(email);
        List<Notification> unread = notificationRepository.findByRecipientIdAndIsReadFalse(recipientId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
