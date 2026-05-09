package com.hazem.worklink.services;

import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.enums.ContractStatus;
import com.hazem.worklink.models.enums.PaymentStatus;
import com.hazem.worklink.repositories.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractPaymentDeadlineScheduler {

    private final ContractRepository contractRepository;
    private final NotificationService notificationService;

    /**
     * Runs every hour. Finds SIGNED contracts whose startDate has arrived
     * but payment is still UNPAID/FAILED/null, and auto-cancels them.
     *
     * Payment window: from freelancer signature (signedAt) until startDate (exclusive).
     * If the company hasn't paid by the time startDate is reached, the contract is cancelled.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cancelUnpaidContractsPastStartDate() {
        LocalDate today = LocalDate.now();

        List<Contract> candidates = contractRepository
                .findByStatusAndStartDateLessThanEqual(ContractStatus.SIGNED, today);

        for (Contract contract : candidates) {
            PaymentStatus ps = contract.getPaymentStatus();
            // Skip contracts that have already been paid (escrow held or captured)
            if (ps == PaymentStatus.AUTHORIZED || ps == PaymentStatus.CAPTURED) {
                continue;
            }

            contract.setStatus(ContractStatus.CANCELLED);
            contract.setCancelledAt(LocalDateTime.now());
            contract.setCancellationReason(
                    "Payment was not completed before the mission start date. Contract automatically cancelled.");
            contractRepository.save(contract);

            notificationService.sendContractAutoCancelledNotification(
                    contract.getFreelancerId(),
                    contract.getCompanyId(),
                    contract.getMissionTitle()
            );

            log.info("Auto-cancelled contract {} (mission: '{}') — payment deadline exceeded on {}",
                    contract.getId(), contract.getMissionTitle(), today);
        }
    }

    /**
     * Runs every day at 09:00 AM.
     * Sends a payment-deadline warning to companies whose SIGNED+UNPAID contract
     * starts in exactly 3 days or exactly 1 day from now.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendPaymentDeadlineWarnings() {
        LocalDate today = LocalDate.now();

        for (long daysAhead : new long[]{3, 1}) {
            LocalDate targetDate = today.plusDays(daysAhead);

            List<Contract> approaching = contractRepository
                    .findByStatusAndStartDateBetween(ContractStatus.SIGNED, targetDate, targetDate);

            for (Contract contract : approaching) {
                PaymentStatus ps = contract.getPaymentStatus();
                if (ps == PaymentStatus.AUTHORIZED || ps == PaymentStatus.CAPTURED) continue;

                long daysLeft = ChronoUnit.DAYS.between(today, contract.getStartDate());

                notificationService.sendPaymentDeadlineWarningNotification(
                        contract.getCompanyId(),
                        contract.getMissionTitle(),
                        contract.getStartDate(),
                        daysLeft);

                notificationService.sendPaymentDeadlineWarningToFreelancer(
                        contract.getFreelancerId(),
                        contract.getMissionTitle(),
                        contract.getStartDate(),
                        daysLeft);

                log.info("Payment deadline warning sent for contract {} (mission: '{}') — {} day(s) left",
                        contract.getId(), contract.getMissionTitle(), daysLeft);
            }
        }
    }
}
