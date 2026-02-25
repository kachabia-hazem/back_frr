package com.hazem.worklink.services;

import com.hazem.worklink.models.Application;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.enums.ApplicationStatus;
import com.hazem.worklink.repositories.ApplicationRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Daily scheduler that reminds companies about pending (unreviewed) applications.
 * Runs every day at 09:00 AM.
 * Only notifies when at least one application has been pending for more than 3 days.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingApplicationsReminderScheduler {

    private final MissionRepository missionRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendPendingApplicationsReminders() {
        log.info("[REMINDER] Running pending applications reminder check...");

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        // Group missions by company
        List<Mission> allMissions = missionRepository.findAll();
        Map<String, List<Mission>> missionsByCompany = allMissions.stream()
                .filter(m -> m.getCompanyId() != null)
                .collect(Collectors.groupingBy(Mission::getCompanyId));

        missionsByCompany.forEach((companyId, missions) -> {
            Company company = companyRepository.findById(companyId).orElse(null);
            if (company == null) return;

            missions.forEach(mission -> {
                List<Application> pendingOld = applicationRepository.findByMissionId(mission.getId())
                        .stream()
                        .filter(a -> a.getStatus() == ApplicationStatus.PENDING
                                && a.getSubmittedAt() != null
                                && a.getSubmittedAt().isBefore(threeDaysAgo))
                        .collect(Collectors.toList());

                if (!pendingOld.isEmpty()) {
                    log.info("[REMINDER] Company '{}' has {} pending application(s) on mission '{}'",
                            company.getCompanyName(), pendingOld.size(), mission.getJobTitle());
                    notificationService.sendPendingApplicationsReminderNotification(
                            companyId, mission.getJobTitle(), pendingOld.size(), mission.getId());
                }
            });
        });

        log.info("[REMINDER] Pending applications reminder check completed.");
    }
}
