package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.CreateApplicationRequest;
import com.hazem.worklink.dto.response.ApplicationResponse;
import com.hazem.worklink.dto.response.RankedApplicationResponse;
import com.hazem.worklink.models.Application;
import com.hazem.worklink.services.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<Application> submitApplication(
            Authentication authentication,
            @Valid @RequestBody CreateApplicationRequest request) {
        String email = authentication.getName();
        Application application = applicationService.submitApplication(email, request);
        return ResponseEntity.ok(application);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(applicationService.getMyApplications(email));
    }

    @GetMapping("/check/{missionId}")
    public ResponseEntity<Boolean> hasApplied(
            Authentication authentication,
            @PathVariable String missionId) {
        String email = authentication.getName();
        return ResponseEntity.ok(applicationService.hasApplied(email, missionId));
    }

    @GetMapping("/company")
    public ResponseEntity<List<ApplicationResponse>> getCompanyApplications(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(applicationService.getCompanyApplications(email));
    }

    @DeleteMapping("/withdraw/{missionId}")
    public ResponseEntity<Void> withdrawApplication(
            Authentication authentication,
            @PathVariable String missionId) {
        String email = authentication.getName();
        applicationService.withdrawApplication(email, missionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mission/{missionId}")
    public ResponseEntity<List<ApplicationResponse>> getMissionApplications(
            Authentication authentication,
            @PathVariable String missionId) {
        String email = authentication.getName();
        return ResponseEntity.ok(applicationService.getMissionApplications(email, missionId));
    }

    @GetMapping("/mission/{missionId}/ranked")
    public ResponseEntity<List<RankedApplicationResponse>> getRankedApplications(
            Authentication authentication,
            @PathVariable String missionId) {
        String email = authentication.getName();
        return ResponseEntity.ok(applicationService.getRankedApplications(email, missionId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam String status) {
        String email = authentication.getName();
        return ResponseEntity.ok(applicationService.updateApplicationStatus(email, id, status));
    }
}
