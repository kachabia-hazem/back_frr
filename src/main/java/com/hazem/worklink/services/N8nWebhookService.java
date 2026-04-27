package com.hazem.worklink.services;

import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Notification;
import com.hazem.worklink.repositories.AdminRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class N8nWebhookService {

    @Value("${n8n.webhook.url}")
    private String webhookUrl;

    @Value("${n8n.webhook.admin-url}")
    private String adminWebhookUrl;

    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final AdminRepository adminRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendEmailNotification(Notification notification) {
        try {
            String recipientEmail = null;
            String recipientName = null;

            // Chercher dans Freelancer
            var freelancer = freelancerRepository.findById(notification.getRecipientId());
            if (freelancer.isPresent()) {
                recipientEmail = freelancer.get().getEmail();
                recipientName = freelancer.get().getFirstName() + " " + freelancer.get().getLastName();
            } else {
                // Chercher dans Company
                var company = companyRepository.findById(notification.getRecipientId());
                if (company.isPresent()) {
                    recipientEmail = company.get().getEmail();
                    recipientName = company.get().getCompanyName();
                }
            }

            if (recipientEmail == null) {
                log.warn("No email found for recipientId: {}", notification.getRecipientId());
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("recipientEmail", recipientEmail);
            payload.put("recipientName", recipientName);
            payload.put("title", notification.getTitle());
            payload.put("message", notification.getMessage());
            payload.put("actionUrl", "http://localhost:4200" + (notification.getActionUrl() != null ? notification.getActionUrl() : ""));
            payload.put("notificationType", notification.getType().name());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(webhookUrl, request, String.class);

            log.info("n8n webhook called for notification: {} to {}", notification.getType(), recipientEmail);

        } catch (Exception e) {
            log.error("Failed to call n8n webhook: {}", e.getMessage());
        }
    }

    /**
     * Notifie l'admin (email) qu'une nouvelle entreprise vient de s'inscrire.
     * Déclenché automatiquement à chaque inscription d'entreprise.
     */
    @Async
    public void notifyAdminNewCompanyRegistration(Company company) {
        try {
            // Récupérer tous les emails admin
            adminRepository.findAll().forEach(admin -> {
                Map<String, Object> payload = new HashMap<>();
                payload.put("event", "NEW_COMPANY_REGISTRATION");
                payload.put("adminEmail", admin.getEmail());
                payload.put("adminName", admin.getFirstName() + " " + admin.getLastName());
                payload.put("companyName", company.getCompanyName());
                payload.put("companyEmail", company.getEmail());
                payload.put("companyId", company.getId());
                payload.put("businessSector", company.getBusinessSector());
                payload.put("managerName", company.getManagerName());
                payload.put("trustScore", company.getTrustScore());
                payload.put("actionUrl", "http://localhost:4200/admin/verifications");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
                restTemplate.postForEntity(adminWebhookUrl, request, String.class);
                log.info("Admin notified via n8n for new company: {}", company.getCompanyName());
            });
        } catch (Exception e) {
            log.error("Failed to notify admin via n8n: {}", e.getMessage());
        }
    }

    /**
     * Notifie l'admin via n8n du résumé quotidien (appelé par le scheduler).
     */
    @Async
    public void sendDailyAdminDigest(Map<String, Object> stats) {
        try {
            adminRepository.findAll().forEach(admin -> {
                Map<String, Object> payload = new HashMap<>(stats);
                payload.put("event", "DAILY_ADMIN_DIGEST");
                payload.put("adminEmail", admin.getEmail());
                payload.put("adminName", admin.getFirstName() + " " + admin.getLastName());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
                restTemplate.postForEntity(adminWebhookUrl, request, String.class);
                log.info("Daily digest sent to admin: {}", admin.getEmail());
            });
        } catch (Exception e) {
            log.error("Failed to send daily digest via n8n: {}", e.getMessage());
        }
    }
}
