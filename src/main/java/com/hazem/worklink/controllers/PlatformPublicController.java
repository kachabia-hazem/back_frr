package com.hazem.worklink.controllers;

import com.hazem.worklink.models.PlatformSettings;
import com.hazem.worklink.services.AdminSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PlatformPublicController {

    private final AdminSettingsService settingsService;

    @GetMapping("/platform-costs")
    public ResponseEntity<Map<String, Integer>> getPlatformCosts() {
        PlatformSettings s = settingsService.getSettings();
        return ResponseEntity.ok(Map.of(
                "applicationCost", s.getApplicationCost(),
                "aiMatchingCost",  s.getAiMatchingCost(),
                "aiRankingCost",   s.getAiRankingCost()
        ));
    }
}
