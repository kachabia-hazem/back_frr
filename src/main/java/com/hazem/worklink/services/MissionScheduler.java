package com.hazem.worklink.services;

import com.hazem.worklink.models.ActiveMission;
import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.enums.ActiveMissionStatus;
import com.hazem.worklink.models.enums.MissionStatus;
import com.hazem.worklink.repositories.ActiveMissionRepository;
import com.hazem.worklink.repositories.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissionScheduler {

    private final MissionRepository missionRepository;
    private final ActiveMissionRepository activeMissionRepository;
    private final NotificationService notificationService;

    /**
     * Runs every minute.
     * OPEN missions whose applicationDeadline has passed → set status to CLOSED.
     * Missions are kept in the database for company history.
     */
    @Scheduled(fixedRate = 60000)
    public void processExpiredMissions() {
        LocalDate today = LocalDate.now();

        List<Mission> expiredOpen = missionRepository
                .findByStatusAndApplicationDeadlineBefore(MissionStatus.OPEN, today);
        for (Mission mission : expiredOpen) {
            mission.setStatus(MissionStatus.CLOSED);
            mission.setUpdatedAt(LocalDateTime.now());
            missionRepository.save(mission);
            log.info("Mission '{}' (id={}) closed — deadline passed", mission.getJobTitle(), mission.getId());
        }
    }

    /**
     * Runs every minute.
     * PENDING active missions whose startDate has been reached → set status to ACTIVE
     * and notify both freelancer and company.
     */
    @Scheduled(fixedRate = 60000)
    public void activatePendingMissions() {
        LocalDate today = LocalDate.now();

        List<ActiveMission> toActivate = activeMissionRepository
                .findByStatusAndStartDateLessThanEqual(ActiveMissionStatus.PENDING, today);

        for (ActiveMission mission : toActivate) {
            mission.setStatus(ActiveMissionStatus.ACTIVE);
            activeMissionRepository.save(mission);
            log.info("ActiveMission '{}' (id={}) activated — start date reached", mission.getTitle(), mission.getId());

            try {
                notificationService.sendMissionActivatedNotification(
                        mission.getFreelancerId(),
                        mission.getCompanyId(),
                        mission.getTitle(),
                        mission.getId());
            } catch (Exception e) {
                log.warn("Failed to send activation notification for mission {}: {}", mission.getId(), e.getMessage());
            }
        }
    }
}
