package com.hazem.worklink.services;

import com.hazem.worklink.dto.response.NotificationResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Admin;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.Notification;
import com.hazem.worklink.models.enums.NotificationType;
import com.hazem.worklink.repositories.AdminRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final AdminRepository adminRepository;
    private final EmailService emailService;

    private static final Set<NotificationType> EMAIL_NOTIFICATION_TYPES = Set.of(
            // Freelancer
            NotificationType.WELCOME,
            NotificationType.APPLICATION_ACCEPTED,
            NotificationType.APPLICATION_REJECTED,
            NotificationType.NEW_MISSION_MATCH,
            NotificationType.MISSION_DEADLINE_SOON,
            NotificationType.CONTRACT_GENERATED,
            NotificationType.CONTRACT_SIGNED,
            NotificationType.CONTRACT_SIGNATURE_REMINDER,
            NotificationType.MISSION_VALIDATED,
            // Company
            NotificationType.COMPANY_WELCOME,
            NotificationType.APPLICATION_RECEIVED,
            NotificationType.PENDING_APPLICATIONS_REMINDER,
            NotificationType.MISSION_SUBMITTED,
            NotificationType.CONTRACT_REJECTED,
            // Admin / Verification
            NotificationType.COMPANY_PENDING_VERIFICATION,
            NotificationType.COMPANY_APPROVED,
            NotificationType.COMPANY_REJECTED,
            NotificationType.MISSION_DELETED_BY_ADMIN,
            NotificationType.CONTRACT_CANCELLED_BY_ADMIN,
            NotificationType.FEEDBACK_VALIDATED,
            NotificationType.FEEDBACK_REJECTED
    );

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
        Notification saved = notificationRepository.save(n);

        if (EMAIL_NOTIFICATION_TYPES.contains(type)) {
            // Résoudre l'email et le nom du destinataire
            String recipientEmail = null;
            String recipientName = null;

            var freelancer = freelancerRepository.findById(recipientId);
            if (freelancer.isPresent()) {
                recipientEmail = freelancer.get().getEmail();
                recipientName = freelancer.get().getFirstName() + " " + freelancer.get().getLastName();
            } else {
                var company = companyRepository.findById(recipientId);
                if (company.isPresent()) {
                    recipientEmail = company.get().getEmail();
                    recipientName = company.get().getCompanyName();
                }
            }

            if (recipientEmail == null) {
                var admin = adminRepository.findById(recipientId);
                if (admin.isPresent()) {
                    recipientEmail = admin.get().getEmail();
                    recipientName = admin.get().getFirstName() + " " + admin.get().getLastName();
                }
            }

            if (recipientEmail != null) {
                emailService.sendNotificationEmail(recipientEmail, recipientName, title, message, actionUrl);
            } else {
                log.warn("No email found for recipientId: {} — notification email skipped", recipientId);
            }
        }

        return saved;
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

    // ─── Contract notification triggers ─────────────────────────────────────────

    /** Contract 1 – Sent to freelancer when a contract is generated (app accepted) */
    public void sendContractGeneratedNotification(String freelancerId, String missionTitle, String companyName, String contractId) {
        String message = String.format(
                "A contract has been generated for the mission \"%s\" by %s.\n" +
                "Please review the contract terms and sign it electronically in your Contracts section.\n" +
                "Your mission begins once the contract is signed.",
                missionTitle, companyName);

        build(freelancerId, NotificationType.CONTRACT_GENERATED,
                "Contract Ready for Signature \uD83D\uDCDD",
                message,
                companyName, null, "/freelancer-contracts");
    }

    /** Contract 2 – Sent to company when contract is generated */
    public void sendContractCreatedToCompanyNotification(String companyId, String missionTitle, String freelancerName) {
        String message = String.format(
                "A contract has been sent to %s for the mission \"%s\".\n" +
                "You will be notified once the freelancer signs the contract.",
                freelancerName, missionTitle);

        build(companyId, NotificationType.CONTRACT_GENERATED,
                "Contract Sent to Freelancer",
                message,
                "WorkLink", null, "/company-contracts");
    }

    /** Contract 3 – Sent to company when freelancer signs the contract */
    public void sendContractSignedNotification(String companyId, String missionTitle, String freelancerName, String contractId, String signedPdfUrl) {
        String message = String.format(
                "%s has signed the contract for the mission \"%s\".\n" +
                "The mission is now officially started. You can view and download the signed contract in your Contracts section.",
                freelancerName, missionTitle);

        build(companyId, NotificationType.CONTRACT_SIGNED,
                "Contract Signed \u2705",
                message,
                freelancerName, null, "/company-contracts");
    }

    /** Contract 4 – Sent to freelancer confirming their signature and sending the signed PDF */
    public void sendContractSignedToFreelancerNotification(String freelancerId, String missionTitle, String signedPdfUrl) {
        String message = String.format(
                "You have successfully signed the contract for the mission \"%s\".\n" +
                "Your mission has officially started. The signed contract is available for download in your Contracts section.",
                missionTitle);

        build(freelancerId, NotificationType.CONTRACT_SIGNED,
                "Contract Signed Successfully \u2705",
                message,
                "WorkLink", null, "/freelancer-contracts");
    }

    /** Contract 5b – Sent to company when freelancer rejects the contract */
    public void sendContractRejectedNotification(String companyId, String missionTitle, String freelancerName, String reason) {
        String message = String.format(
                "%s has rejected the contract for the mission \"%s\".\n%s\n" +
                "You can review the application and generate a new contract if needed.",
                freelancerName, missionTitle,
                (reason != null && !reason.isBlank()) ? "Reason: " + reason : "No reason provided.");

        build(companyId, NotificationType.CONTRACT_REJECTED,
                "Contract Rejected",
                message,
                freelancerName, null, "/company-contracts");
    }

    /** Contract 5 – Reminder sent to freelancer 3 days after contract creation if still unsigned */
    public void sendContractSignatureReminderNotification(String freelancerId, String missionTitle, String companyName) {
        String message = String.format(
                "Reminder: You have a pending contract for the mission \"%s\" by %s that is still awaiting your signature.\n" +
                "Please sign it as soon as possible to officially start the mission.\n" +
                "You can review and sign the contract in your Contracts section.",
                missionTitle, companyName);

        build(freelancerId, NotificationType.CONTRACT_SIGNATURE_REMINDER,
                "Contract Signature Reminder \u23F0",
                message,
                "WorkLink", null, "/freelancer-contracts");
    }

    /** Mission Validation 1 – Sent to company when freelancer submits work for validation */
    public void sendMissionSubmittedNotification(String companyId, String missionTitle, String freelancerName, String missionId) {
        String message = String.format(
                "%s has completed and submitted the mission \"%s\" for your review.\n" +
                "Please check the deliverables and Kanban board, then approve or request revisions in Mission Tracking.",
                freelancerName, missionTitle);

        build(companyId, NotificationType.MISSION_SUBMITTED,
                "Mission Submitted for Validation",
                message,
                freelancerName, null, "/company-mission-view/" + missionId);
    }

    /** Mission Validation 2 – Sent to freelancer when company validates (approves or requests revision) */
    public void sendMissionValidatedNotification(String freelancerId, String missionTitle, String companyName,
                                                  boolean approved, String note, String missionId) {
        String title = approved ? "Mission Approved \u2705" : "Revision Requested \uD83D\uDD04";
        String message;
        if (approved) {
            message = String.format(
                    "Congratulations! %s has reviewed and approved your work on the mission \"%s\".\n" +
                    (note != null && !note.isBlank() ? "Company feedback: \"%s\"" : "Well done — the mission is now marked as completed."),
                    companyName, missionTitle, note);
        } else {
            message = String.format(
                    "%s has reviewed your submission for the mission \"%s\" and requested revisions.\n" +
                    (note != null && !note.isBlank() ? "Feedback: \"%s\"\n\n" : "") +
                    "The mission is now active again. Please make the necessary changes and resubmit when ready.",
                    companyName, missionTitle, note);
        }

        build(freelancerId, NotificationType.MISSION_VALIDATED,
                title, message,
                companyName, null, "/active-mission/" + missionId);
    }

    // ─── Admin / Company Verification notifications ──────────────────────────────

    /** Verification 1 – Envoyée à l'entreprise dès son inscription (attente de validation) */
    public void sendCompanyPendingVerificationNotification(String companyId) {
        String message =
                "Bienvenue sur WorkLink!\n\n" +
                "Votre demande d'inscription a bien été reçue.\n" +
                "Notre équipe va vérifier vos informations dans les plus brefs délais.\n\n" +
                "Vous recevrez une notification par email dès que votre compte sera validé.\n" +
                "Une fois approuvé, vous pourrez publier des missions et accéder à toutes les fonctionnalités de la plateforme.\n\n" +
                "Merci de votre confiance.";

        build(companyId, NotificationType.COMPANY_PENDING_VERIFICATION,
                "Compte en attente de validation",
                message,
                "WorkLink Team", null, "/company-dashboard");
    }

    /** Verification 2 – Envoyée à l'entreprise quand l'admin approuve son compte */
    public void sendCompanyApprovedNotification(String companyId) {
        String message =
                "Félicitations! Votre compte entreprise a été validé par notre équipe.\n\n" +
                "Vous pouvez désormais accéder à toutes les fonctionnalités de WorkLink:\n" +
                "- Publier des missions et recevoir des candidatures de freelancers qualifiés\n" +
                "- Parcourir notre annuaire de freelancers et trouver les meilleurs profils\n" +
                "- Gérer vos contrats et suivre vos projets en temps réel\n\n" +
                "Bienvenue dans la communauté WorkLink!";

        build(companyId, NotificationType.COMPANY_APPROVED,
                "Compte validé - Bienvenue sur WorkLink!",
                message,
                "WorkLink Team", null, "/company-dashboard");
    }

    /** Verification 3 – Envoyée à l'entreprise quand l'admin rejette son compte */
    public void sendCompanyRejectedNotification(String companyId, String reason) {
        String message = String.format(
                "Nous avons examiné votre demande d'inscription sur WorkLink.\n\n" +
                "Malheureusement, nous ne pouvons pas approuver votre compte pour le moment.\n\n" +
                "Motif: %s\n\n" +
                "Si vous pensez qu'il s'agit d'une erreur ou si vous souhaitez fournir des informations complémentaires, " +
                "veuillez nous contacter à support@worklink.com.",
                reason != null && !reason.isBlank() ? reason : "Informations insuffisantes ou non conformes.");

        build(companyId, NotificationType.COMPANY_REJECTED,
                "Demande d'inscription refusée",
                message,
                "WorkLink Team", null, "/company-dashboard");
    }

    /** Admin – Sent to both parties when admin force-cancels a contract */
    public void sendContractCancelledByAdminNotification(
            String freelancerId, String companyId, String missionTitle, String reason) {
        String msg = String.format(
                "Le contrat pour la mission \"%s\" a été annulé par l'équipe d'administration WorkLink.\n\n" +
                "Motif : %s\n\n" +
                "Pour toute question, veuillez contacter support@worklink.com.",
                missionTitle,
                reason != null && !reason.isBlank() ? reason : "Non précisé.");

        build(freelancerId, NotificationType.CONTRACT_CANCELLED_BY_ADMIN,
                "Contrat annulé par l'administrateur", msg, "WorkLink Admin", null, "/freelancer-dashboard/contracts");
        build(companyId, NotificationType.CONTRACT_CANCELLED_BY_ADMIN,
                "Contrat annulé par l'administrateur", msg, "WorkLink Admin", null, "/company-dashboard/contracts");
    }

    /** Admin – Sent to company when admin deletes one of their missions */
    public void sendMissionDeletedByAdminNotification(String companyId, String missionTitle, String reason) {
        String message = String.format(
                "Votre mission \"%s\" a été supprimée par l'équipe d'administration WorkLink.\n\n" +
                "Motif : %s\n\n" +
                "Si vous pensez qu'il s'agit d'une erreur, veuillez contacter notre support à support@worklink.com.",
                missionTitle,
                reason != null && !reason.isBlank() ? reason : "Non précisé.");

        build(companyId, NotificationType.MISSION_DELETED_BY_ADMIN,
                "Mission supprimée par l'administrateur",
                message,
                "WorkLink Admin", null, "/company-dashboard/missions");
    }

    /** Admin – Sent to user when their feedback is approved and published */
    public void sendFeedbackValidatedNotification(String userId) {
        build(userId, NotificationType.FEEDBACK_VALIDATED,
                "Votre avis est publié sur WorkLink !",
                "Merci pour votre retour ! Votre avis a été examiné par notre équipe et est désormais visible " +
                "sur la page d'accueil de WorkLink.\n\nVotre témoignage aide la communauté WorkLink à grandir.",
                "WorkLink Team", null, "/");
    }

    /** Admin – Sent to user when their feedback is rejected */
    public void sendFeedbackRejectedNotification(String userId, String reason) {
        String message = String.format(
                "Votre avis sur WorkLink n'a pas pu être publié après examen par notre équipe de modération.\n\n" +
                "Motif : %s\n\n" +
                "Si vous pensez qu'il s'agit d'une erreur, contactez-nous à support@worklink.com.",
                reason != null && !reason.isBlank() ? reason : "Non conforme à la charte de la communauté.");
        build(userId, NotificationType.FEEDBACK_REJECTED,
                "Votre avis n'a pas été retenu",
                message,
                "WorkLink Team", null, null);
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

    /** Mission Activated – Sent to both freelancer and company when a PENDING mission start date arrives */
    public void sendMissionActivatedNotification(String freelancerId, String companyId, String missionTitle, String missionId) {
        // Notify freelancer
        String freelancerMessage = String.format(
                "Your mission \"%s\" has officially started today! Head to Mission Tracking to begin your work.",
                missionTitle);
        build(freelancerId, NotificationType.MISSION_ACTIVATED,
                "Mission Started \uD83D\uDE80",
                freelancerMessage,
                "WorkLink", null, "/active-mission/" + missionId);

        // Notify company
        String companyMessage = String.format(
                "The mission \"%s\" has officially started today. You can follow the freelancer's progress in Mission Tracking.",
                missionTitle);
        build(companyId, NotificationType.MISSION_ACTIVATED,
                "Mission Started \uD83D\uDE80",
                companyMessage,
                "WorkLink", null, "/company-mission-view/" + missionId);
    }

    // ─── Read / query (supports both freelancers and companies) ─────────────────

    /** Resolves the MongoDB document ID for any registered user (freelancer, company, or admin). */
    private String resolveRecipientId(String email) {
        Optional<Freelancer> freelancer = freelancerRepository.findByEmail(email);
        if (freelancer.isPresent()) return freelancer.get().getId();

        Optional<Company> company = companyRepository.findByEmail(email);
        if (company.isPresent()) return company.get().getId();

        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) return admin.get().getId();

        throw new ResourceNotFoundException("User not found with email: " + email);
    }

    /** Sends a notification to every admin in the system. */
    private void notifyAllAdmins(NotificationType type, String title, String message,
                                  String senderName, String senderId, String actionUrl) {
        List<Admin> admins = adminRepository.findAll();
        for (Admin admin : admins) {
            build(admin.getId(), type, title, message, senderName, senderId, actionUrl);
        }
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

    // ─── Admin-targeted notification triggers ────────────────────────────────────

    /** Notifie tous les admins qu'une entreprise vient de s'inscrire et attend vérification. */
    public void sendAdminCompanyVerificationRequestNotification(String companyName, String companyId) {
        String message = String.format(
                "La société \"%s\" vient de s'inscrire sur WorkLink et attend votre validation.\n" +
                "Veuillez examiner les informations et documents fournis, puis approuver ou rejeter le compte.",
                companyName);

        notifyAllAdmins(
                NotificationType.ADMIN_COMPANY_VERIFICATION_REQUEST,
                "Nouvelle demande de vérification : " + companyName,
                message,
                companyName, companyId,
                "/admin/verifications/" + companyId);
    }

    /** Notifie tous les admins qu'un nouveau freelancer vient de s'inscrire. */
    public void sendAdminNewFreelancerRegisteredNotification(String freelancerName, String freelancerId) {
        String message = String.format(
                "Le freelancer \"%s\" vient de rejoindre WorkLink.\n" +
                "Son profil est maintenant actif sur la plateforme.",
                freelancerName);

        notifyAllAdmins(
                NotificationType.ADMIN_NEW_FREELANCER_REGISTERED,
                "Nouveau freelancer inscrit : " + freelancerName,
                message,
                freelancerName, freelancerId,
                "/admin/users");
    }

    /** Notifie tous les admins qu'un contrat vient d'être signé. */
    public void sendAdminContractSignedNotification(String missionTitle, String freelancerName, String companyName) {
        String message = String.format(
                "Le freelancer \"%s\" a signé le contrat pour la mission \"%s\" avec la société \"%s\".\n" +
                "Le mission est maintenant officiellement démarrée.",
                freelancerName, missionTitle, companyName);

        notifyAllAdmins(
                NotificationType.ADMIN_NEW_CONTRACT_SIGNED,
                "Contrat signé : " + missionTitle,
                message,
                "WorkLink System", null,
                "/admin/contracts");
    }

    /** Notifie tous les admins qu'une nouvelle mission a été publiée. */
    public void sendAdminNewMissionPublishedNotification(String missionTitle, String companyName, String missionId) {
        String message = String.format(
                "La société \"%s\" vient de publier une nouvelle mission : \"%s\".\n" +
                "Elle est maintenant visible aux freelancers sur la plateforme.",
                companyName, missionTitle);

        notifyAllAdmins(
                NotificationType.ADMIN_NEW_MISSION_PUBLISHED,
                "Nouvelle mission publiée : " + missionTitle,
                message,
                companyName, null,
                "/admin/missions");
    }

    // ── Payment notifications ─────────────────────────────────────────────────

    public void sendContractPaymentNotification(String companyId, String freelancerId,
                                                String missionTitle, Double amount) {
        String fmt = amount != null ? String.format("%.2f DT", amount) : "—";
        build(companyId, NotificationType.CONTRACT_PAYMENT_AUTHORIZED,
                "Paiement sécurisé ✓",
                "Votre paiement de " + fmt + " pour la mission \"" + missionTitle + "\" a été sécurisé.\n" +
                "Les fonds sont bloqués en escrow et seront libérés au freelancer après validation.",
                "WorkLink", null, "/company-contracts");
        build(freelancerId, NotificationType.CONTRACT_PAYMENT_AUTHORIZED,
                "Paiement de mission sécurisé 🔒",
                "L'entreprise a effectué le paiement pour la mission \"" + missionTitle + "\".\n" +
                "Un montant de " + fmt + " est en escrow. Vous le recevrez après validation de votre travail.",
                "WorkLink", null, "/freelancer-contracts");
    }

    public void sendPaymentReleasedNotification(String freelancerId, String missionTitle, Double amount) {
        String fmt = amount != null ? String.format("%.2f DT", amount) : "—";
        build(freelancerId, NotificationType.CONTRACT_PAYMENT_RELEASED,
                "Paiement libéré 🎉",
                "Le paiement de " + fmt + " pour la mission \"" + missionTitle +
                "\" a été libéré sur votre compte (commission 7% déduite).",
                "WorkLink", null, "/freelancer-contracts");
    }

    public void sendPackPurchaseNotification(String userId, String packName, int points) {
        build(userId, NotificationType.PACK_PURCHASED,
                "Points crédités ✓",
                points + " points ont été ajoutés à votre solde suite à l'achat du pack \"" + packName + "\".",
                "WorkLink", null, "/company-balance");
    }
}
