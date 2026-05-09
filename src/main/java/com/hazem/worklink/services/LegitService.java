package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.AdminCancelMissionRequest;
import com.hazem.worklink.dto.request.AdminContinueMissionRequest;
import com.hazem.worklink.dto.request.AdminRefundRequest;
import com.hazem.worklink.dto.request.AdminSendEmailRequest;
import com.hazem.worklink.dto.request.CreateLegitRequest;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.ActiveMission;
import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.Legit;
import com.hazem.worklink.models.enums.ActiveMissionStatus;
import com.hazem.worklink.models.enums.ContractStatus;
import com.hazem.worklink.models.enums.LegitStatus;
import com.hazem.worklink.models.enums.PaymentStatus;
import com.hazem.worklink.repositories.ActiveMissionRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.ContractRepository;
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
    private final ContractRepository contractRepository;
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
        Legit saved = legitRepository.save(legit);

        // Notify both parties of status progression (skip EN_ATTENTE = initial state)
        if (newStatus != LegitStatus.EN_ATTENTE) {
            String title = legit.getMissionTitle() != null ? legit.getMissionTitle() : "Mission";
            if (legit.getReporterId() != null)
                notificationService.sendLegitStatusUpdatedNotification(legit.getReporterId(), newStatus.name(), title);
            if (legit.getOtherPartyId() != null)
                notificationService.sendLegitStatusUpdatedNotification(legit.getOtherPartyId(), newStatus.name(), title);
        }
        return saved;
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

    // ─── Admin: resolution actions ────────────────────────────────────────────

    public Legit adminCancelMission(String legitId, AdminCancelMissionRequest req) {
        Legit legit = getLegit(legitId);
        String reason = req.getReason() != null ? req.getReason() : "";

        contractRepository.findById(legit.getContractId() != null ? legit.getContractId() : "").ifPresent(contract -> {
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setCancelledAt(LocalDateTime.now());
            contract.setCancellationReason("Annulé par l'admin suite au litige : " + reason);
            contractRepository.save(contract);
        });

        if (legit.getActiveMissionId() != null) {
            activeMissionRepository.findById(legit.getActiveMissionId()).ifPresent(mission -> {
                mission.setStatus(ActiveMissionStatus.COMPLETED);
                activeMissionRepository.save(mission);
            });
        }

        legit.setStatus(LegitStatus.RESOLU);
        legit.setAdminDecision("ANNULE");
        legit.setAdminNote(reason);
        legit.setResolvedAt(LocalDateTime.now());
        legit.setUpdatedAt(LocalDateTime.now());
        Legit saved = legitRepository.save(legit);

        String title = legit.getMissionTitle() != null ? legit.getMissionTitle() : "Mission";
        if (legit.getReporterId() != null)
            notificationService.sendLegitMissionCancelledNotification(legit.getReporterId(), title, reason);
        if (legit.getOtherPartyId() != null)
            notificationService.sendLegitMissionCancelledNotification(legit.getOtherPartyId(), title, reason);

        log.info("Admin cancelled mission for legit {}, reason: {}", legitId, reason);
        return saved;
    }

    public Legit adminRefundMission(String legitId, AdminRefundRequest req) {
        double flPct = req.getFreelancerPercentage() != null ? req.getFreelancerPercentage() : 0;
        double coPct = req.getCompanyPercentage() != null ? req.getCompanyPercentage() : 0;
        if (Math.abs(flPct + coPct - 100.0) > 0.1) {
            throw new IllegalArgumentException("Les pourcentages doivent totaliser 100%");
        }
        String reason = req.getReason() != null ? req.getReason() : "";

        Legit legit = getLegit(legitId);

        final double[] calculatedAmounts = {0.0, 0.0}; // [freelancer, company]
        contractRepository.findById(legit.getContractId() != null ? legit.getContractId() : "").ifPresent(contract -> {
            double total = contract.getTotalAmount() != null ? contract.getTotalAmount() : 0.0;
            double flAmount = Math.round(total * flPct) / 100.0;
            double coAmount = Math.round(total * coPct) / 100.0;
            calculatedAmounts[0] = flAmount;
            calculatedAmounts[1] = coAmount;
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setCancelledAt(LocalDateTime.now());
            contract.setCancellationReason("Remboursement décidé par l'admin suite au litige : " + reason);
            contract.setPaymentStatus(PaymentStatus.REFUNDED);
            contract.setFreelancerRefundAmount(flAmount);
            contract.setCompanyRefundAmount(coAmount);
            contractRepository.save(contract);
        });

        if (legit.getActiveMissionId() != null) {
            activeMissionRepository.findById(legit.getActiveMissionId()).ifPresent(mission -> {
                mission.setStatus(ActiveMissionStatus.COMPLETED);
                activeMissionRepository.save(mission);
            });
        }

        legit.setStatus(LegitStatus.RESOLU);
        legit.setAdminDecision("REMBOURSE");
        legit.setFreelancerRefundPercentage(flPct);
        legit.setCompanyRefundPercentage(coPct);
        legit.setAdminNote(reason);
        legit.setResolvedAt(LocalDateTime.now());
        legit.setUpdatedAt(LocalDateTime.now());
        Legit saved = legitRepository.save(legit);

        String title = legit.getMissionTitle() != null ? legit.getMissionTitle() : "Mission";
        String freelancerId = "FREELANCER".equals(legit.getReporterRole()) ? legit.getReporterId() : legit.getOtherPartyId();
        String companyId    = "COMPANY".equals(legit.getReporterRole())    ? legit.getReporterId() : legit.getOtherPartyId();

        if (freelancerId != null)
            notificationService.sendLegitRefundNotification(freelancerId, title, flPct, "FREELANCER");
        if (companyId != null)
            notificationService.sendLegitRefundNotification(companyId, title, coPct, "COMPANY");

        log.info("Admin refund for legit {}: freelancer {}%, company {}%", legitId, flPct, coPct);
        return saved;
    }

    public Legit adminContinueMission(String legitId, AdminContinueMissionRequest req) {
        Legit legit = getLegit(legitId);
        String note = req.getAdminNote() != null ? req.getAdminNote() : "";

        if (legit.getActiveMissionId() != null) {
            activeMissionRepository.findById(legit.getActiveMissionId()).ifPresent(mission -> {
                mission.setStatus(ActiveMissionStatus.ACTIVE);
                activeMissionRepository.save(mission);
            });
        }

        legit.setStatus(LegitStatus.RESOLU);
        legit.setAdminDecision("CONTINUE");
        legit.setAdminNote(note);
        legit.setResolvedAt(LocalDateTime.now());
        legit.setUpdatedAt(LocalDateTime.now());
        Legit saved = legitRepository.save(legit);

        String title = legit.getMissionTitle() != null ? legit.getMissionTitle() : "Mission";
        if (legit.getReporterId() != null)
            notificationService.sendLegitMissionContinuedNotification(legit.getReporterId(), title);
        if (legit.getOtherPartyId() != null)
            notificationService.sendLegitMissionContinuedNotification(legit.getOtherPartyId(), title);

        log.info("Admin continued mission for legit {}", legitId);
        return saved;
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
