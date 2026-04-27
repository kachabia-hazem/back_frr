package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.CreateReportRequest;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Report;
import com.hazem.worklink.models.enums.ReportStatus;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.ContractRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final ContractRepository contractRepository;
    private final EmailService emailService;

    // ─── Create (Freelancer or Company) ──────────────────────────────────────

    public Report createReport(CreateReportRequest req, String reporterEmail) {
        Report report = new Report();
        report.setType(req.getType());
        report.setStatus(ReportStatus.EN_ATTENTE);
        report.setDescription(req.getDescription());
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        // Resolve reporter identity
        var freelancerOpt = freelancerRepository.findByEmail(reporterEmail);
        if (freelancerOpt.isPresent()) {
            var fl = freelancerOpt.get();
            report.setReportedById(fl.getId());
            report.setReportedByRole("FREELANCER");
            report.setReportedByName(fl.getFirstName() + " " + fl.getLastName());
            report.setReportedByEmail(fl.getEmail());
        } else {
            var company = companyRepository.findByEmail(reporterEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + reporterEmail));
            report.setReportedById(company.getId());
            report.setReportedByRole("COMPANY");
            report.setReportedByName(company.getCompanyName());
            report.setReportedByEmail(company.getEmail());
        }

        // Resolve reported-against identity
        if (req.getReportedAgainstId() != null) {
            freelancerRepository.findById(req.getReportedAgainstId()).ifPresentOrElse(fl -> {
                report.setReportedAgainstId(fl.getId());
                report.setReportedAgainstRole("FREELANCER");
                report.setReportedAgainstName(fl.getFirstName() + " " + fl.getLastName());
                report.setReportedAgainstEmail(fl.getEmail());
            }, () -> companyRepository.findById(req.getReportedAgainstId()).ifPresent(c -> {
                report.setReportedAgainstId(c.getId());
                report.setReportedAgainstRole("COMPANY");
                report.setReportedAgainstName(c.getCompanyName());
                report.setReportedAgainstEmail(c.getEmail());
            }));
        }

        // Resolve optional contract
        if (req.getContractId() != null && !req.getContractId().isBlank()) {
            contractRepository.findById(req.getContractId()).ifPresent(c -> {
                report.setContractId(c.getId());
                report.setContractTitle(c.getMissionTitle());
            });
        }

        Report saved = reportRepository.save(report);
        log.info("Report created by {} ({}) against {}", reporterEmail, report.getReportedByRole(), req.getReportedAgainstId());
        return saved;
    }

    // ─── Admin: list & detail ─────────────────────────────────────────────────

    public List<Report> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Report> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Report getReport(String id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    // ─── Admin: update status ─────────────────────────────────────────────────

    public Report updateStatus(String id, ReportStatus newStatus) {
        Report report = getReport(id);
        report.setStatus(newStatus);
        report.setUpdatedAt(LocalDateTime.now());
        if (newStatus == ReportStatus.TRAITE || newStatus == ReportStatus.REJETE) {
            report.setResolvedAt(LocalDateTime.now());
        }
        return reportRepository.save(report);
    }

    // ─── Admin: simple warning ────────────────────────────────────────────────

    public Report warnReporter(String id, String note) {
        Report report = getReport(id);
        report.setAdminNote(note);
        report.setStatus(ReportStatus.TRAITE);
        report.setUpdatedAt(LocalDateTime.now());
        report.setResolvedAt(LocalDateTime.now());

        Report saved = reportRepository.save(report);

        // Notify reported-against party that a report about them was reviewed
        if (saved.getReportedAgainstEmail() != null) {
            emailService.sendNotificationEmail(
                    saved.getReportedAgainstEmail(),
                    saved.getReportedAgainstName() != null ? saved.getReportedAgainstName() : "User",
                    "Avertissement — WorkLink",
                    "Suite à un signalement à votre encontre, l'équipe WorkLink vous adresse un avertissement formel.\n\n" +
                    "Note de l'administrateur : " + note + "\n\n" +
                    "Veuillez respecter les règles de la plateforme pour éviter toute suspension de compte.",
                    "/dashboard"
            );
        }

        log.info("Warning sent for report {} — note: {}", id, note);
        return saved;
    }

    // ─── Admin: reject with email to reporter ────────────────────────────────

    public Report rejectReport(String id, String reason) {
        Report report = getReport(id);
        report.setStatus(ReportStatus.REJETE);
        report.setRejectionReason(reason);
        report.setUpdatedAt(LocalDateTime.now());
        report.setResolvedAt(LocalDateTime.now());

        Report saved = reportRepository.save(report);

        // Send rejection email to the reporter
        if (saved.getReportedByEmail() != null) {
            emailService.sendReportRejectionEmail(
                    saved.getReportedByEmail(),
                    saved.getReportedByName() != null ? saved.getReportedByName() : "User",
                    reason
            );
        }

        log.info("Report {} rejected, reason: {}", id, reason);
        return saved;
    }

    public Map<String, Long> getStats() {
        long total      = reportRepository.count();
        long enAttente  = reportRepository.countByStatus(ReportStatus.EN_ATTENTE);
        long enCours    = reportRepository.countByStatus(ReportStatus.EN_COURS);
        long traite     = reportRepository.countByStatus(ReportStatus.TRAITE);
        long rejete     = reportRepository.countByStatus(ReportStatus.REJETE);
        return Map.of(
                "TOTAL", total,
                "EN_ATTENTE", enAttente,
                "EN_COURS", enCours,
                "TRAITE", traite,
                "REJETE", rejete
        );
    }
}
