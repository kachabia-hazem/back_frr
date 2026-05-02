package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.AdminSendEmailRequest;
import com.hazem.worklink.dto.request.CreateLegitRequest;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.ActiveMission;
import com.hazem.worklink.models.Legit;
import com.hazem.worklink.models.enums.ActiveMissionStatus;
import com.hazem.worklink.models.enums.LegitStatus;
import com.hazem.worklink.repositories.ActiveMissionRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.LegitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegitService {

    private final LegitRepository legitRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final ActiveMissionRepository activeMissionRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // ─── Create ──────────────────────────────────────────────────────────────

    public Legit createLegit(CreateLegitRequest req, String reporterEmail) {
        Legit legit = new Legit();
        legit.setStatus(LegitStatus.EN_ATTENTE);
        legit.setActiveMissionId(req.getActiveMissionId());
        legit.setDescription(req.getDescription());
        legit.setTotalAmount(req.getTotalAmount());
        legit.setResolution(req.getResolution());
        legit.setEvidenceFiles(req.getEvidenceFiles() != null ? req.getEvidenceFiles() : new ArrayList<>());
        legit.setCreatedAt(LocalDateTime.now());
        legit.setUpdatedAt(LocalDateTime.now());

        // Resolve mission context
        if (req.getActiveMissionId() != null) {
            activeMissionRepository.findById(req.getActiveMissionId()).ifPresent(m -> {
                legit.setContractId(m.getContractId());
                legit.setMissionTitle(m.getTitle());
            });
        }

        // Resolve reporter identity and other party
        var freelancerOpt = freelancerRepository.findByEmail(reporterEmail);
        if (freelancerOpt.isPresent()) {
            var fl = freelancerOpt.get();
            legit.setReporterId(fl.getId());
            legit.setReporterRole("FREELANCER");
            legit.setReporterName(fl.getFirstName() + " " + fl.getLastName());
            legit.setReporterEmail(fl.getEmail());
            legit.setReporterPhone(fl.getPhoneNumber());

            if (req.getActiveMissionId() != null) {
                activeMissionRepository.findById(req.getActiveMissionId()).ifPresent(m ->
                    companyRepository.findById(m.getCompanyId()).ifPresent(c -> {
                        legit.setOtherPartyId(c.getId());
                        legit.setOtherPartyRole("COMPANY");
                        legit.setOtherPartyName(c.getCompanyName());
                        legit.setOtherPartyEmail(c.getEmail());
                        legit.setOtherPartyPhone(c.getManagerPhoneNumber());
                    })
                );
            }
        } else {
            var company = companyRepository.findByEmail(reporterEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + reporterEmail));
            legit.setReporterId(company.getId());
            legit.setReporterRole("COMPANY");
            legit.setReporterName(company.getCompanyName());
            legit.setReporterEmail(company.getEmail());
            legit.setReporterPhone(company.getManagerPhoneNumber());

            if (req.getActiveMissionId() != null) {
                activeMissionRepository.findById(req.getActiveMissionId()).ifPresent(m ->
                    freelancerRepository.findById(m.getFreelancerId()).ifPresent(fl -> {
                        legit.setOtherPartyId(fl.getId());
                        legit.setOtherPartyRole("FREELANCER");
                        legit.setOtherPartyName(fl.getFirstName() + " " + fl.getLastName());
                        legit.setOtherPartyEmail(fl.getEmail());
                        legit.setOtherPartyPhone(fl.getPhoneNumber());
                    })
                );
            }
        }

        Legit saved = legitRepository.save(legit);
        log.info("Legit created by {} ({}), missionId={}", reporterEmail, legit.getReporterRole(), req.getActiveMissionId());

        // Set active mission to DISPUTE and notify both parties
        if (req.getActiveMissionId() != null) {
            activeMissionRepository.findById(req.getActiveMissionId()).ifPresent(mission -> {
                mission.setStatus(ActiveMissionStatus.DISPUTE);
                activeMissionRepository.save(mission);

                String initiatorName = saved.getReporterName() != null ? saved.getReporterName() : "Un utilisateur";
                String missionTitle  = mission.getTitle() != null ? mission.getTitle() : "Mission";

                // Notify reporter (confirmer que son litige a été ouvert)
                if (saved.getReporterId() != null) {
                    notificationService.sendLegitOpenedNotification(
                            saved.getReporterId(), initiatorName, missionTitle, mission.getId());
                }
                // Notify the other party
                if (saved.getOtherPartyId() != null) {
                    notificationService.sendLegitOpenedNotification(
                            saved.getOtherPartyId(), initiatorName, missionTitle, mission.getId());
                }
            });
        }

        return saved;
    }

    // ─── Admin: list & detail ─────────────────────────────────────────────────

    public List<Legit> getAllLegits() {
        return legitRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Legit> getLegitsByStatus(LegitStatus status) {
        return legitRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Legit getLegit(String id) {
        return legitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legit not found: " + id));
    }

    // ─── Admin: update status ─────────────────────────────────────────────────

    public Legit updateStatus(String id, LegitStatus newStatus) {
        Legit legit = getLegit(id);
        legit.setStatus(newStatus);
        legit.setUpdatedAt(LocalDateTime.now());
        if (newStatus == LegitStatus.RESOLU || newStatus == LegitStatus.REJETE) {
            legit.setResolvedAt(LocalDateTime.now());
        }
        return legitRepository.save(legit);
    }

    // ─── Admin: send email + in-app notification to reporter ─────────────────

    public Legit sendEmail(String id, AdminSendEmailRequest req) {
        Legit legit = getLegit(id);

        if (legit.getReporterEmail() != null) {
            emailService.sendNotificationEmail(
                    legit.getReporterEmail(),
                    legit.getReporterName() != null ? legit.getReporterName() : "Utilisateur",
                    req.getSubject(),
                    req.getBody(),
                    "/dashboard"
            );
        }

        if (legit.getReporterId() != null) {
            notificationService.sendLegitResponseNotification(
                    legit.getReporterId(), req.getSubject(), req.getBody());
        }

        if (legit.getStatus() == LegitStatus.EN_ATTENTE) {
            legit.setStatus(LegitStatus.EN_COURS);
        }
        legit.setUpdatedAt(LocalDateTime.now());
        return legitRepository.save(legit);
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    public Map<String, Long> getStats() {
        return Map.of(
                "TOTAL",     legitRepository.count(),
                "EN_ATTENTE", legitRepository.countByStatus(LegitStatus.EN_ATTENTE),
                "EN_COURS",  legitRepository.countByStatus(LegitStatus.EN_COURS),
                "RESOLU",    legitRepository.countByStatus(LegitStatus.RESOLU),
                "REJETE",    legitRepository.countByStatus(LegitStatus.REJETE)
        );
    }
}
