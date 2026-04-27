package com.hazem.worklink.services;

import com.hazem.worklink.dto.response.ContractResponse;
import com.hazem.worklink.dto.response.MissionResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Admin;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.enums.CompanyStatus;
import com.hazem.worklink.models.enums.ContractStatus;
import com.hazem.worklink.repositories.AdminRepository;
import com.hazem.worklink.repositories.ApplicationRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.ContractRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final CompanyRepository companyRepository;
    private final FreelancerRepository freelancerRepository;
    private final AdminRepository adminRepository;
    private final MissionRepository missionRepository;
    private final ContractRepository contractRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;
    private final N8nWebhookService n8nWebhookService;
    private final AiSearchClient aiSearchClient;

    // ─── Stats globales (Overview) ────────────────────────────────────────────

    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalFreelancers = freelancerRepository.count();
        long totalCompanies = companyRepository.count();
        long pendingCompanies = companyRepository.countByVerificationStatus(CompanyStatus.PENDING);
        long approvedCompanies = companyRepository.countByVerificationStatus(CompanyStatus.APPROVED);
        long rejectedCompanies = companyRepository.countByVerificationStatus(CompanyStatus.REJECTED);
        long totalMissions = missionRepository.count();
        long totalContracts = contractRepository.count();
        long totalApplications = applicationRepository.count();
        long totalAdmins = adminRepository.count();

        stats.put("totalFreelancers", totalFreelancers);
        stats.put("totalCompanies", totalCompanies);
        stats.put("pendingCompanies", pendingCompanies);
        stats.put("approvedCompanies", approvedCompanies);
        stats.put("rejectedCompanies", rejectedCompanies);
        stats.put("totalMissions", totalMissions);
        stats.put("totalContracts", totalContracts);
        stats.put("totalApplications", totalApplications);
        stats.put("totalAdmins", totalAdmins);
        stats.put("generatedAt", LocalDateTime.now().toString());

        return stats;
    }

    // ─── Company Verification ─────────────────────────────────────────────────

    public List<Company> getPendingCompanies() {
        return companyRepository.findByVerificationStatus(CompanyStatus.PENDING);
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Company getCompanyById(String companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
    }

    public Company approveCompany(String companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        company.setVerificationStatus(CompanyStatus.APPROVED);
        company.setVerifiedAt(LocalDateTime.now());
        company.setRejectionReason(null);
        company.setUpdatedAt(LocalDateTime.now());
        companyRepository.save(company);

        // Notifier l'entreprise
        notificationService.sendCompanyApprovedNotification(company.getId());

        log.info("Company approved: {} ({})", company.getCompanyName(), company.getId());
        return company;
    }

    public Company rejectCompany(String companyId, String reason) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        company.setVerificationStatus(CompanyStatus.REJECTED);
        company.setRejectionReason(reason);
        company.setVerifiedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        companyRepository.save(company);

        // Notifier l'entreprise avec le motif
        notificationService.sendCompanyRejectedNotification(company.getId(), reason);

        log.info("Company rejected: {} ({}) - reason: {}", company.getCompanyName(), company.getId(), reason);
        return company;
    }

    // ─── Freelancer Management ────────────────────────────────────────────────

    public List<Freelancer> getAllFreelancers() {
        return freelancerRepository.findAll();
    }

    public Freelancer getFreelancerById(String freelancerId) {
        return freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found: " + freelancerId));
    }

    // ─── User Ban / Unban ─────────────────────────────────────────────────────

    public Map<String, Object> toggleFreelancerBan(String freelancerId, String banReason) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found: " + freelancerId));
        boolean willBeBanned = Boolean.TRUE.equals(freelancer.getIsActive());
        freelancer.setIsActive(!willBeBanned);
        freelancer.setBanReason(willBeBanned ? banReason : null);
        freelancer.setUpdatedAt(LocalDateTime.now());
        freelancerRepository.save(freelancer);
        Map<String, Object> result = new HashMap<>();
        result.put("id", freelancerId);
        result.put("isActive", freelancer.getIsActive());
        result.put("message", freelancer.getIsActive() ? "Compte réactivé" : "Compte suspendu");
        return result;
    }

    public Map<String, Object> toggleCompanyBan(String companyId, String banReason) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        boolean willBeBanned = Boolean.TRUE.equals(company.getIsActive());
        company.setIsActive(!willBeBanned);
        company.setBanReason(willBeBanned ? banReason : null);
        company.setUpdatedAt(LocalDateTime.now());
        companyRepository.save(company);
        Map<String, Object> result = new HashMap<>();
        result.put("id", companyId);
        result.put("isActive", company.getIsActive());
        result.put("message", company.getIsActive() ? "Compte réactivé" : "Compte suspendu");
        return result;
    }

    // ─── Delete users ─────────────────────────────────────────────────────────

    public void deleteFreelancer(String freelancerId) {
        if (!freelancerRepository.existsById(freelancerId)) {
            throw new ResourceNotFoundException("Freelancer not found: " + freelancerId);
        }
        freelancerRepository.deleteById(freelancerId);
        log.info("Freelancer deleted by admin: {}", freelancerId);
    }

    public void deleteCompany(String companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company not found: " + companyId);
        }
        companyRepository.deleteById(companyId);
        log.info("Company deleted by admin: {}", companyId);
    }

    // ─── Mission Management ───────────────────────────────────────────────────

    public List<MissionResponse> getAllMissionsWithCompany() {
        List<Mission> missions = missionRepository.findAll();
        List<String> companyIds = missions.stream()
                .map(Mission::getCompanyId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Company> companyMap = companyIds.isEmpty()
                ? Map.of()
                : companyRepository.findAllById(companyIds).stream()
                    .collect(Collectors.toMap(Company::getId, c -> c));
        return missions.stream()
                .map(m -> MissionResponse.from(m, m.getCompanyId() != null ? companyMap.get(m.getCompanyId()) : null))
                .collect(Collectors.toList());
    }

    public void adminDeleteMission(String missionId, String reason) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));
        String companyId = mission.getCompanyId();
        String jobTitle = mission.getJobTitle();
        missionRepository.deleteById(missionId);
        notificationService.sendMissionDeletedByAdminNotification(companyId, jobTitle, reason);
        log.info("Mission deleted by admin: {} ({}), reason: {}", jobTitle, missionId, reason);
    }

    // ─── Contract Management ──────────────────────────────────────────────────

    public List<ContractResponse> getAllContracts() {
        return contractRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(ContractResponse::from)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getContractStats() {
        Map<String, Long> stats = new HashMap<>();
        for (ContractStatus s : ContractStatus.values()) {
            stats.put(s.name(), contractRepository.findAll().stream()
                    .filter(c -> c.getStatus() == s).count());
        }
        stats.put("TOTAL", contractRepository.count());
        return stats;
    }

    public ContractResponse adminCancelContract(String contractId, String reason) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        if (contract.getStatus() == ContractStatus.CANCELLED) {
            throw new RuntimeException("Contract is already cancelled.");
        }
        if (contract.getStatus() == ContractStatus.FINISHED) {
            throw new RuntimeException("Cannot cancel a finished contract.");
        }
        contract.setStatus(ContractStatus.CANCELLED);
        contract.setCancelledAt(LocalDateTime.now());
        contract.setCancellationReason(reason);
        contractRepository.save(contract);
        notificationService.sendContractCancelledByAdminNotification(
                contract.getFreelancerId(), contract.getCompanyId(), contract.getMissionTitle(), reason);
        log.info("Contract {} cancelled by admin, reason: {}", contractId, reason);
        return ContractResponse.from(contract);
    }

    // ─── Evolution Charts ─────────────────────────────────────────────────────

    public List<Map<String, Object>> getUsersEvolution(int months) {
        List<Freelancer> freelancers = freelancerRepository.findAll();
        List<Company> companies = companyRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i)
                    .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1);
            long fCount = freelancers.stream()
                    .filter(f -> f.getCreatedAt() != null
                              && !f.getCreatedAt().isBefore(start)
                              && f.getCreatedAt().isBefore(end))
                    .count();
            long cCount = companies.stream()
                    .filter(c -> c.getCreatedAt() != null
                              && !c.getCreatedAt().isBefore(start)
                              && c.getCreatedAt().isBefore(end))
                    .count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + start.getYear());
            point.put("freelancers", fCount);
            point.put("companies", cCount);
            point.put("total", fCount + cCount);
            result.add(point);
        }
        return result;
    }

    public List<Map<String, Object>> getContractsEvolution(int months) {
        List<Contract> contracts = contractRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i)
                    .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1);
            List<Contract> monthContracts = contracts.stream()
                    .filter(c -> c.getCreatedAt() != null
                              && !c.getCreatedAt().isBefore(start)
                              && c.getCreatedAt().isBefore(end))
                    .collect(Collectors.toList());
            long total    = monthContracts.size();
            long signed   = monthContracts.stream()
                    .filter(c -> c.getStatus() == ContractStatus.SIGNED
                              || c.getStatus() == ContractStatus.FINISHED)
                    .count();
            long finished = monthContracts.stream()
                    .filter(c -> c.getStatus() == ContractStatus.FINISHED)
                    .count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + start.getYear());
            point.put("total", total);
            point.put("signed", signed);
            point.put("finished", finished);
            result.add(point);
        }
        return result;
    }

    // ─── Admin Management (Super Admin only) ──────────────────────────────────

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    // ─── Trust Score refresh (re-analyse AI) ─────────────────────────────────

    public Company refreshTrustScore(String companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        aiSearchClient.computeCompanyTrustScore(company);
        return companyRepository.findById(companyId).orElseThrow();
    }
}
