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
 * Daily scheduler that manages unsigned contracts:
 *  - Day 3 after creation: sends a final reminder warning the freelancer they have 1 day left.
 *  - Day 4 after creation: auto-cancels the contract and notifies both parties.
 *
 * Runs every day at 09:00 AM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractSignatureReminderScheduler {

    private final ContractRepository contractRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    public void processUnsignedContracts() {
        log.info("[CONTRACT-UNSIGNED] Running unsigned contract check...");

        LocalDateTime now        = LocalDateTime.now();
        LocalDateTime fourDaysAgo  = now.minusDays(4);
        LocalDateTime threeDaysAgo = now.minusDays(3);

        // Phase 1 — Auto-cancel contracts older than 4 days (1 day after the reminder)
        List<Contract> toCancel = contractRepository
                .findByStatusAndCreatedAtBefore(ContractStatus.PENDING_SIGNATURE, fourDaysAgo);

        for (Contract contract : toCancel) {
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setCancelledAt(LocalDateTime.now());
            contract.setCancellationReason(
                    "Freelancer did not sign the contract within the allowed period. Contract automatically cancelled.");
            contractRepository.save(contract);

            notificationService.sendContractExpiredUnsignedNotification(
                    contract.getFreelancerId(),
                    contract.getCompanyId(),
                    contract.getMissionTitle(),
                    contract.getFreelancerName()
            );

            log.info("[CONTRACT-UNSIGNED] Auto-cancelled contract {} (mission: '{}') — freelancer never signed after 4 days",
                    contract.getId(), contract.getMissionTitle());
        }

        // Phase 2 — Send final reminder to contracts that are 3–4 days old (haven't been cancelled yet)
        List<Contract> toRemind = contractRepository
                .findByStatusAndCreatedAtBetween(ContractStatus.PENDING_SIGNATURE, fourDaysAgo, threeDaysAgo);

        for (Contract contract : toRemind) {
            notificationService.sendContractSignatureReminderNotification(
                    contract.getFreelancerId(),
                    contract.getMissionTitle(),
                    contract.getCompanyName()
            );

            log.info("[CONTRACT-UNSIGNED] Final reminder sent to freelancer '{}' for contract '{}' — 1 day left to sign",
                    contract.getFreelancerEmail(), contract.getId());
        }

        log.info("[CONTRACT-UNSIGNED] Done — {} auto-cancellation(s), {} final reminder(s).",
                toCancel.size(), toRemind.size());
    }
}
