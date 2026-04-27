package com.hazem.worklink.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDailyDigestScheduler {

    private final AdminService adminService;
    private final N8nWebhookService n8nWebhookService;

    /**
     * Envoie un résumé quotidien aux admins chaque matin à 8h00.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyDigest() {
        log.info("Sending daily admin digest...");
        try {
            var stats = adminService.getGlobalStats();
            n8nWebhookService.sendDailyAdminDigest(stats);
            log.info("Daily admin digest sent successfully.");
        } catch (Exception e) {
            log.error("Failed to send daily admin digest: {}", e.getMessage());
        }
    }
}
