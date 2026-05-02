package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.AdminSendEmailRequest;
import com.hazem.worklink.dto.request.CreateLegitRequest;
import com.hazem.worklink.models.Legit;
import com.hazem.worklink.models.enums.LegitStatus;
import com.hazem.worklink.services.LegitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LegitController {

    private final LegitService legitService;

    // ─── User endpoints (FREELANCER / COMPANY) ────────────────────────────────

    @PostMapping("/api/legits")
    public ResponseEntity<Legit> createLegit(@RequestBody CreateLegitRequest req,
                                              Authentication auth) {
        return ResponseEntity.ok(legitService.createLegit(req, auth.getName()));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    @GetMapping("/api/admin/legits")
    public ResponseEntity<List<Legit>> getAllLegits(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(legitService.getLegitsByStatus(LegitStatus.valueOf(status)));
        }
        return ResponseEntity.ok(legitService.getAllLegits());
    }

    @GetMapping("/api/admin/legits/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(legitService.getStats());
    }

    @GetMapping("/api/admin/legits/{id}")
    public ResponseEntity<Legit> getLegit(@PathVariable String id) {
        return ResponseEntity.ok(legitService.getLegit(id));
    }

    @PutMapping("/api/admin/legits/{id}/status")
    public ResponseEntity<Legit> updateStatus(@PathVariable String id,
                                               @RequestBody Map<String, String> body) {
        LegitStatus status = LegitStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(legitService.updateStatus(id, status));
    }

    @PostMapping("/api/admin/legits/{id}/send-email")
    public ResponseEntity<Legit> sendEmail(@PathVariable String id,
                                            @RequestBody AdminSendEmailRequest req) {
        return ResponseEntity.ok(legitService.sendEmail(id, req));
    }
}
