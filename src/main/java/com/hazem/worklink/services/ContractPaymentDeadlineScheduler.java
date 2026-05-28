package com.hazem.worklink.services;

import com.hazem.worklink.models.ActiveMission;
import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.enums.ActiveMissionStatus;
import com.hazem.worklink.models.enums.ContractStatus;
import com.hazem.worklink.models.enums.PaymentStatus;
import com.hazem.worklink.repositories.ActiveMissionRepository;
import com.hazem.worklink.repositories.CompanyRepository;
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
    private final ActiveMissionRepository activeMissionRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    /**
     * Runs every hour. Finds SIGNED contracts whose startDate has arrived
     * but payment is still UNPAID/FAILED/null, and auto-cancels them.
     * Also cancels the linked active mission and notifies both parties.
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

            // Cancel the contract
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setCancelledAt(LocalDateTime.now());
            contract.setCancellationReason(
                    "Payment was not completed before the mission start date. Contract automatically cancelled.");
            contractRepository.save(contract);

            // Cancel the linked active mission if it exists and is still PENDING
            activeMissionRepository.findByContractId(contract.getId()).ifPresent(mission -> {
                if (mission.getStatus() == ActiveMissionStatus.PENDING) {
                    mission.setStatus(ActiveMissionStatus.CANCELLED);
                    activeMissionRepository.save(mission);
                    log.info("ActiveMission {} (mission: '{}') cancelled — linked contract not paid",
                            mission.getId(), mission.getTitle());
                }
            });

            // Resolve company name for the notification message
            String companyName = companyRepository.findById(contract.getCompanyId())
                    .map(c -> c.getCompanyName() != null ? c.getCompanyName() : "the company")
                    .orElse("the company");

            // Notify both parties: contract cancelled
            notificationService.sendContractAutoCancelledNotification(
                    contract.getFreelancerId(),
                    contract.getCompanyId(),
                    contract.getMissionTitle()
            );

            // Notify both parties: active mission cancelled due to non-payment (clear scenario)
            notificationService.sendActiveMissionCancelledDueToNonPaymentNotification(
                    contract.getFreelancerId(),
                    contract.getCompanyId(),
                    contract.getMissionTitle(),
                    companyName
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
