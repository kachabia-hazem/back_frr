package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.SubmitFeedbackRequest;
import com.hazem.worklink.dto.response.FeedbackPublicDto;
import com.hazem.worklink.models.Feedback;
import com.hazem.worklink.services.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    // ─── Public endpoint (home page — no auth required) ──────────────────────

    @GetMapping("/api/public/feedbacks")
    public ResponseEntity<List<FeedbackPublicDto>> getPublicFeedbacks() {
        return ResponseEntity.ok(feedbackService.getPublicFeedbacks());
    }

    // ─── User endpoints (FREELANCER / COMPANY) ────────────────────────────────

    /** Submit platform feedback after a mission is validated */
    @PostMapping("/api/feedbacks")
    public ResponseEntity<Feedback> submitFeedback(@RequestBody SubmitFeedbackRequest req,
                                                    Authentication auth) {
        return ResponseEntity.ok(feedbackService.submitFeedback(req, auth.getName()));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    /** List all feedbacks */
    @GetMapping("/api/admin/feedbacks")
    public ResponseEntity<List<Feedback>> getAllFeedbacks() {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks());
    }

    /** List only pending feedbacks */
    @GetMapping("/api/admin/feedbacks/pending")
    public ResponseEntity<List<Feedback>> getPendingFeedbacks() {
        return ResponseEntity.ok(feedbackService.getPendingFeedbacks());
    }

    /** Validate (approve) a feedback */
    @PutMapping("/api/admin/feedbacks/{id}/validate")
    public ResponseEntity<Feedback> validateFeedback(@PathVariable String id) {
        return ResponseEntity.ok(feedbackService.validateFeedback(id));
    }

    /** Delete a feedback */
    @DeleteMapping("/api/admin/feedbacks/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable String id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }
}
