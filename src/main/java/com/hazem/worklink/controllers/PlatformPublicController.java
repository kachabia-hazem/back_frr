package com.hazem.worklink.controllers;

import com.hazem.worklink.models.Feedback;
import com.hazem.worklink.models.PlatformSettings;
import com.hazem.worklink.models.enums.CompanyStatus;
import com.hazem.worklink.models.enums.FeedbackStatus;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FeedbackRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.MissionRepository;
import com.hazem.worklink.services.AdminSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PlatformPublicController {

    private final AdminSettingsService settingsService;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository    companyRepository;
    private final MissionRepository    missionRepository;
    private final FeedbackRepository   feedbackRepository;

    @GetMapping("/platform-costs")
    public ResponseEntity<Map<String, Integer>> getPlatformCosts() {
        PlatformSettings s = settingsService.getSettings();
        return ResponseEntity.ok(Map.of(
                "applicationCost", s.getApplicationCost(),
                "aiMatchingCost",  s.getAiMatchingCost(),
                "aiRankingCost",   s.getAiRankingCost()
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getPlatformStats() {
        long freelancers = freelancerRepository.count();
        long companies   = companyRepository.countByVerificationStatus(CompanyStatus.APPROVED);
        long missions    = missionRepository.count();

        List<Feedback> validated = feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.VALIDATED);
        long satisfactionRate = 0;
        if (!validated.isEmpty()) {
            long positive = validated.stream()
                    .filter(f -> f.getRating() != null && f.getRating() >= 4)
                    .count();
            satisfactionRate = Math.round((positive * 100.0) / validated.size());
        }

        return ResponseEntity.ok(Map.of(
                "freelancers",       freelancers,
                "companies",         companies,
                "missions",          missions,
                "satisfactionRate",  satisfactionRate
        ));
    }
}
