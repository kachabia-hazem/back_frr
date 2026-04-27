package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.response.ContractResponse;
import com.hazem.worklink.dto.response.MissionResponse;
import com.hazem.worklink.models.Admin;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ─── Stats Overview ───────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        return ResponseEntity.ok(adminService.getGlobalStats());
    }

    @GetMapping("/stats/users-evolution")
    public ResponseEntity<List<Map<String, Object>>> getUsersEvolution(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(adminService.getUsersEvolution(months));
    }

    @GetMapping("/stats/contracts-evolution")
    public ResponseEntity<List<Map<String, Object>>> getContractsEvolution(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(adminService.getContractsEvolution(months));
    }

    // ─── Company Verification ─────────────────────────────────────────────────

    @GetMapping("/companies/pending")
    public ResponseEntity<List<Company>> getPendingCompanies() {
        return ResponseEntity.ok(adminService.getPendingCompanies());
    }

    @GetMapping("/companies")
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(adminService.getAllCompanies());
    }

    @GetMapping("/companies/{id}")
    public ResponseEntity<Company> getCompanyById(@PathVariable String id) {
        return ResponseEntity.ok(adminService.getCompanyById(id));
    }

    @PostMapping("/companies/{id}/approve")
    public ResponseEntity<Company> approveCompany(@PathVariable String id) {
        return ResponseEntity.ok(adminService.approveCompany(id));
    }

    @PostMapping("/companies/{id}/reject")
    public ResponseEntity<Company> rejectCompany(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        return ResponseEntity.ok(adminService.rejectCompany(id, reason));
    }

    @PostMapping("/companies/{id}/toggle-ban")
    public ResponseEntity<Map<String, Object>> toggleCompanyBan(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String banReason = body != null ? body.getOrDefault("banReason", "") : "";
        return ResponseEntity.ok(adminService.toggleCompanyBan(id, banReason));
    }

    @DeleteMapping("/companies/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable String id) {
        adminService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/companies/{id}/refresh-trust-score")
    public ResponseEntity<Company> refreshTrustScore(@PathVariable String id) {
        return ResponseEntity.ok(adminService.refreshTrustScore(id));
    }

    // ─── Freelancer Management ────────────────────────────────────────────────

    @GetMapping("/freelancers")
    public ResponseEntity<List<Freelancer>> getAllFreelancers() {
        return ResponseEntity.ok(adminService.getAllFreelancers());
    }

    @GetMapping("/freelancers/{id}")
    public ResponseEntity<Freelancer> getFreelancerById(@PathVariable String id) {
        return ResponseEntity.ok(adminService.getFreelancerById(id));
    }

    @PostMapping("/freelancers/{id}/toggle-ban")
    public ResponseEntity<Map<String, Object>> toggleFreelancerBan(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String banReason = body != null ? body.getOrDefault("banReason", "") : "";
        return ResponseEntity.ok(adminService.toggleFreelancerBan(id, banReason));
    }

    @DeleteMapping("/freelancers/{id}")
    public ResponseEntity<Void> deleteFreelancer(@PathVariable String id) {
        adminService.deleteFreelancer(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Mission Management ───────────────────────────────────────────────────

    @GetMapping("/missions")
    public ResponseEntity<List<MissionResponse>> getAllMissions() {
        return ResponseEntity.ok(adminService.getAllMissionsWithCompany());
    }

    @DeleteMapping("/missions/{id}")
    public ResponseEntity<Void> deleteMission(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        adminService.adminDeleteMission(id, reason);
        return ResponseEntity.noContent().build();
    }

    // ─── Contract Management ──────────────────────────────────────────────────

    @GetMapping("/contracts")
    public ResponseEntity<List<ContractResponse>> getAllContracts() {
        return ResponseEntity.ok(adminService.getAllContracts());
    }

    @GetMapping("/contracts/stats")
    public ResponseEntity<Map<String, Long>> getContractStats() {
        return ResponseEntity.ok(adminService.getContractStats());
    }

    @PostMapping("/contracts/{id}/cancel")
    public ResponseEntity<ContractResponse> cancelContract(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        return ResponseEntity.ok(adminService.adminCancelContract(id, reason));
    }

    // ─── Admin Management (Super Admin) ───────────────────────────────────────

    @GetMapping("/admins")
    public ResponseEntity<List<Admin>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }
}
