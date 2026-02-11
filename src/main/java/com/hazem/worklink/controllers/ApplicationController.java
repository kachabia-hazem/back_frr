package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.CreateApplicationRequest;
import com.hazem.worklink.dto.response.ApplicationResponse;
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
}
