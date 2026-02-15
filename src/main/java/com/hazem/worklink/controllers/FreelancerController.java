package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.UpdateCvDataRequest;
import com.hazem.worklink.dto.request.UpdateFreelancerRequest;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.services.FreelancerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/freelancer")
@RequiredArgsConstructor
public class FreelancerController {

    private final FreelancerService freelancerService;

    @GetMapping("/public/{id}")
    public ResponseEntity<Freelancer> getFreelancerById(@PathVariable String id) {
        Freelancer freelancer = freelancerService.getFreelancerById(id);
        return ResponseEntity.ok(freelancer);
    }

    @GetMapping("/public/all")
    public ResponseEntity<java.util.List<Freelancer>> getAllFreelancers() {
        return ResponseEntity.ok(freelancerService.getAllFreelancers());
    }

    @GetMapping("/me")
    public ResponseEntity<Freelancer> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        Freelancer freelancer = freelancerService.getFreelancerByEmail(email);
        return ResponseEntity.ok(freelancer);
    }

    @PutMapping("/me")
    public ResponseEntity<Freelancer> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateFreelancerRequest request) {
        String email = authentication.getName();
        Freelancer freelancer = freelancerService.updateFreelancer(email, request);
        return ResponseEntity.ok(freelancer);
    }

    @PutMapping("/me/cv")
    public ResponseEntity<Freelancer> updateCvData(
            Authentication authentication,
            @RequestBody UpdateCvDataRequest request) {
        String email = authentication.getName();
        Freelancer freelancer = freelancerService.updateCvData(email, request);
        return ResponseEntity.ok(freelancer);
    }

    @PutMapping("/me/profile-picture")
    public ResponseEntity<Freelancer> updateProfilePicture(
            Authentication authentication,
            @RequestBody java.util.Map<String, String> request) {
        String email = authentication.getName();
        String pictureUrl = request.get("profilePicture");
        Freelancer freelancer = freelancerService.updateProfilePicture(email, pictureUrl);
        return ResponseEntity.ok(freelancer);
    }

    @PutMapping("/me/cv-url")
    public ResponseEntity<Freelancer> updateCvUrl(
            Authentication authentication,
            @RequestBody java.util.Map<String, String> request) {
        String email = authentication.getName();
        String cvUrl = request.get("cvUrl");
        Freelancer freelancer = freelancerService.updateCvUrl(email, cvUrl);
        return ResponseEntity.ok(freelancer);
    }

    @PutMapping("/me/card-customization")
    public ResponseEntity<Freelancer> updateCardCustomization(
            Authentication authentication,
            @RequestBody java.util.Map<String, Object> request) {
        String email = authentication.getName();
        String cardBackground = (String) request.get("cardBackground");
        @SuppressWarnings("unchecked")
        java.util.List<String> portfolioImages = (java.util.List<String>) request.get("portfolioImages");
        Freelancer freelancer = freelancerService.updateCardCustomization(email, cardBackground, portfolioImages);
        return ResponseEntity.ok(freelancer);
    }
}
