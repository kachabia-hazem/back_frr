package com.hazem.worklink.controllers;

import com.hazem.worklink.models.PlatformSettings;
import com.hazem.worklink.services.AdminSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminSettingsController {

    private final AdminSettingsService settingsService;

    @GetMapping
    public ResponseEntity<PlatformSettings> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping("/platform-fee")
    public ResponseEntity<PlatformSettings> updatePlatformFee(@RequestBody Map<String, Integer> body) {
        Integer percent = body.get("percent");
        if (percent == null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(settingsService.updatePlatformFee(percent));
    }

    @PutMapping("/point-costs")
    public ResponseEntity<PlatformSettings> updatePointCosts(@RequestBody Map<String, Integer> body) {
        Integer appCost     = body.get("applicationCost");
        Integer aiMatch     = body.get("aiMatchingCost");
        Integer aiRank      = body.get("aiRankingCost");
        Integer welcome     = body.get("welcomeBonus");
        if (appCost == null || aiMatch == null || aiRank == null || welcome == null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(settingsService.updatePointCosts(appCost, aiMatch, aiRank, welcome));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String email = authentication.getName();
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null || newPassword.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));

        settingsService.changeAdminPassword(email, currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
