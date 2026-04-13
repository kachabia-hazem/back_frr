package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.UpdateCvDataRequest;
import com.hazem.worklink.dto.request.UpdateFreelancerRequest;
import com.hazem.worklink.dto.response.MissionResponse;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.Review;
import com.hazem.worklink.services.AiSearchClient;
import com.hazem.worklink.services.FreelancerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/freelancer")
@RequiredArgsConstructor
public class FreelancerController {

    private final FreelancerService freelancerService;
    private final AiSearchClient aiSearchClient;

    @GetMapping("/public/{id}")
    public ResponseEntity<Freelancer> getFreelancerById(@PathVariable String id) {
        Freelancer freelancer = freelancerService.getFreelancerById(id);
        return ResponseEntity.ok(freelancer);
    }

    @GetMapping("/public/{id}/reviews")
    public ResponseEntity<java.util.List<Review>> getFreelancerReviews(@PathVariable String id) {
        return ResponseEntity.ok(freelancerService.getFreelancerReviews(id));
    }

    @PostMapping("/public/{id}/view")
    public ResponseEntity<Void> recordProfileView(@PathVariable String id) {
        freelancerService.incrementProfileViews(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/public/batch-appearances")
    public ResponseEntity<Void> recordSearchAppearances(@RequestBody java.util.List<String> ids) {
        freelancerService.incrementSearchAppearances(ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public/all")
    public ResponseEntity<java.util.List<Freelancer>> getAllFreelancers() {
        return ResponseEntity.ok(freelancerService.getAllFreelancers());
    }

    @GetMapping("/public/ai-search")
    public ResponseEntity<java.util.List<FreelancerService.AiFreelancerResult>> aiSearch(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "20") int topK) {
        return ResponseEntity.ok(freelancerService.aiSearchFreelancers(prompt, topK));
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

    @PostMapping("/me/extract-cv")
    public ResponseEntity<java.util.Map<String, Object>> extractCvData(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        java.util.Map<String, Object> extracted = aiSearchClient.extractCvData(file);
        if (extracted.isEmpty()) {
            return ResponseEntity.status(503).body(java.util.Map.of("error", "AI service unavailable or extraction failed"));
        }
        return ResponseEntity.ok(extracted);
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

    // ─── Saved Missions ───────────────────────────────────────────────────────

    /** Returns the list of saved mission IDs (most-recent first) for the current freelancer. */
    @GetMapping("/saved-missions")
    public ResponseEntity<List<String>> getSavedMissionIds(Authentication authentication) {
        return ResponseEntity.ok(freelancerService.getSavedMissionIds(authentication.getName()));
    }

    /** Returns full mission details for all saved missions. */
    @GetMapping("/saved-missions/details")
    public ResponseEntity<List<MissionResponse>> getSavedMissions(Authentication authentication) {
        return ResponseEntity.ok(freelancerService.getSavedMissions(authentication.getName()));
    }

    /** Toggle save/unsave a mission. Returns the updated list of saved IDs (most-recent first). */
    @PostMapping("/saved-missions/{missionId}")
    public ResponseEntity<List<String>> toggleSavedMission(@PathVariable String missionId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(freelancerService.toggleSavedMission(authentication.getName(), missionId));
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
