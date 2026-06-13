package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.CreateReportRequest;
import com.hazem.worklink.dto.request.RejectReportRequest;
import com.hazem.worklink.models.Report;
import com.hazem.worklink.models.enums.ReportStatus;
import com.hazem.worklink.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ─── User endpoints (FREELANCER / COMPANY) ────────────────────────────────

    @PostMapping("/api/reports")
    public ResponseEntity<Report> createReport(@RequestBody CreateReportRequest req,
                                               Authentication auth) {
        return ResponseEntity.ok(reportService.createReport(req, auth.getName()));
    }

    @PostMapping("/api/reports/public")
    public ResponseEntity<Report> createPublicReport(@RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().build();
        CreateReportRequest req = new CreateReportRequest();
        req.setType(com.hazem.worklink.models.enums.ReportType.valueOf(body.getOrDefault("type", "COMPTE_INJUSTE")));
        req.setDescription(body.getOrDefault("description", ""));
        String customType = body.get("customType");
        if (customType != null && !customType.isBlank()) req.setCustomType(customType);
        return ResponseEntity.ok(reportService.createReport(req, email));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    @GetMapping("/api/admin/reports")
    public ResponseEntity<List<Report>> getAllReports(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(reportService.getReportsByStatus(ReportStatus.valueOf(status)));
        }
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/api/admin/reports/{id}")
    public ResponseEntity<Report> getReport(@PathVariable String id) {
        return ResponseEntity.ok(reportService.getReport(id));
    }

    @PutMapping("/api/admin/reports/{id}/status")
    public ResponseEntity<Report> updateStatus(@PathVariable String id,
                                               @RequestBody Map<String, String> body) {
        ReportStatus status = ReportStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(reportService.updateStatus(id, status));
    }

    @PostMapping("/api/admin/reports/{id}/reject")
    public ResponseEntity<Report> rejectReport(@PathVariable String id,
                                               @RequestBody RejectReportRequest req) {
        return ResponseEntity.ok(reportService.rejectReport(id, req.getReason()));
    }

    @GetMapping("/api/admin/reports/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(reportService.getStats());
    }
}
