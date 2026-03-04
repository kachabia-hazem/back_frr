package com.hazem.worklink.services;

import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.enums.ContractStatus;
import com.hazem.worklink.repositories.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily scheduler that reminds freelancers to sign contracts still pending after 3 days.
 * Runs every day at 09:00 AM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractSignatureReminderScheduler {

    private final ContractRepository contractRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendContractSignatureReminders() {
        log.info("[CONTRACT-REMINDER] Running contract signature reminder check...");

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        List<Contract> pendingContracts = contractRepository
                .findByStatusAndCreatedAtBefore(ContractStatus.PENDING_SIGNATURE, threeDaysAgo);

        if (pendingContracts.isEmpty()) {
            log.info("[CONTRACT-REMINDER] No pending contracts older than 3 days found.");
            return;
        }

        for (Contract contract : pendingContracts) {
            log.info("[CONTRACT-REMINDER] Sending reminder to freelancer '{}' for contract '{}' (created: {})",
                    contract.getFreelancerEmail(), contract.getId(), contract.getCreatedAt());

            notificationService.sendContractSignatureReminderNotification(
                    contract.getFreelancerId(),
                    contract.getMissionTitle(),
                    contract.getCompanyName()
            );
        }

        log.info("[CONTRACT-REMINDER] Sent {} reminder(s).", pendingContracts.size());
    }
}
