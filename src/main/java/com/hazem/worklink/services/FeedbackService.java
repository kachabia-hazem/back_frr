package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.SubmitFeedbackRequest;
import com.hazem.worklink.dto.response.FeedbackPublicDto;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Feedback;
import com.hazem.worklink.models.enums.FeedbackStatus;
import com.hazem.worklink.repositories.ActiveMissionRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FeedbackRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final ActiveMissionRepository activeMissionRepository;
    private final NotificationService notificationService;

    // ─── Submit (Company or Freelancer) ───────────────────────────────────────

    public Feedback submitFeedback(SubmitFeedbackRequest req, String email) {
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Resolve user identity
        String userId;
        String userRole;

        String userName;
        String userPhoto = null;

        var freelancer = freelancerRepository.findByEmail(email);
        if (freelancer.isPresent()) {
            userId    = freelancer.get().getId();
            userRole  = "FREELANCER";
            userName  = freelancer.get().getFirstName() + " " + freelancer.get().getLastName();
            userPhoto = freelancer.get().getProfilePicture();
        } else {
            var company = companyRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
            userId    = company.getId();
            userRole  = "COMPANY";
            userName  = company.getCompanyName();
            userPhoto = company.getCompanyLogo();
        }

        // Prevent duplicate feedback per mission per user
        if (feedbackRepository.existsByMissionIdAndUserId(req.getMissionId(), userId)) {
            throw new IllegalStateException("Feedback already submitted for this mission");
        }

        String missionTitle = activeMissionRepository.findById(req.getMissionId())
                .map(m -> m.getTitle())
                .orElse("Unknown mission");

        Feedback feedback = new Feedback();
        feedback.setMissionId(req.getMissionId());
        feedback.setMissionTitle(missionTitle);
        feedback.setUserId(userId);
        feedback.setUserRole(userRole);
        feedback.setUserName(userName);
        feedback.setUserPhoto(userPhoto);
        feedback.setRating(req.getRating());
        feedback.setComment(req.getComment());
        feedback.setStatus(FeedbackStatus.PENDING);
        feedback.setCreatedAt(LocalDateTime.now());

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Feedback submitted by {} ({}) for mission {}", email, userRole, req.getMissionId());
        return saved;
    }

    // ─── Public (home page) ───────────────────────────────────────────────────

    public List<FeedbackPublicDto> getPublicFeedbacks() {
        return feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.VALIDATED).stream()
                .filter(f -> f.getComment() != null && !f.getComment().isBlank())
                .map(f -> new FeedbackPublicDto(
                        f.getId(), f.getUserRole(),
                        f.getUserName() != null ? f.getUserName() : "Utilisateur",
                        f.getUserPhoto(),
                        f.getRating(), f.getComment(), f.getCreatedAt()))
                .toList();
    }

    // ─── Admin operations ─────────────────────────────────────────────────────

    public List<Feedback> getAllFeedbacks() {
        List<Feedback> list = feedbackRepository.findAllByOrderByCreatedAtDesc();
        list.forEach(this::enrichIfMissing);
        return list;
    }

    public List<Feedback> getPendingFeedbacks() {
        List<Feedback> list = feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.PENDING);
        list.forEach(this::enrichIfMissing);
        return list;
    }

    /** Backfills userName/userPhoto for feedbacks submitted before caching was added. */
    private void enrichIfMissing(Feedback f) {
        if (f.getUserName() != null) return;
        if ("FREELANCER".equals(f.getUserRole())) {
            freelancerRepository.findById(f.getUserId()).ifPresent(fr -> {
                f.setUserName(fr.getFirstName() + " " + fr.getLastName());
                f.setUserPhoto(fr.getProfilePicture());
            });
        } else {
            companyRepository.findById(f.getUserId()).ifPresent(c -> {
                f.setUserName(c.getCompanyName());
                f.setUserPhoto(c.getCompanyLogo());
            });
        }
        // Persist so the next call doesn't need to do the lookup again
        if (f.getUserName() != null) feedbackRepository.save(f);
    }

    public Feedback validateFeedback(String id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + id));
        if (feedback.getStatus() == FeedbackStatus.VALIDATED) return feedback;
        feedback.setStatus(FeedbackStatus.VALIDATED);
        feedback.setValidatedAt(LocalDateTime.now());
        Feedback saved = feedbackRepository.save(feedback);
        notificationService.sendFeedbackValidatedNotification(saved.getUserId());
        log.info("Feedback {} validated by admin", id);
        return saved;
    }

    public Feedback rejectFeedback(String id, String reason) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + id));
        if (feedback.getStatus() == FeedbackStatus.REJECTED) return feedback;
        feedback.setStatus(FeedbackStatus.REJECTED);
        feedback.setRejectionReason(reason);
        feedback.setRejectedAt(LocalDateTime.now());
        Feedback saved = feedbackRepository.save(feedback);
        notificationService.sendFeedbackRejectedNotification(saved.getUserId(), reason);
        log.info("Feedback {} rejected by admin, reason: {}", id, reason);
        return saved;
    }

    public Map<String, Long> getStats() {
        long total    = feedbackRepository.count();
        long pending  = feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.PENDING).size();
        long validated= feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.VALIDATED).size();
        long rejected = feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.REJECTED).size();
        return Map.of("TOTAL", total, "PENDING", pending, "VALIDATED", validated, "REJECTED", rejected);
    }

    public void deleteFeedback(String id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + id));
        feedbackRepository.delete(feedback);
        log.info("Feedback {} deleted by admin", id);
    }
}
