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

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final ActiveMissionRepository activeMissionRepository;

    // ─── Submit (Company or Freelancer) ───────────────────────────────────────

    public Feedback submitFeedback(SubmitFeedbackRequest req, String email) {
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Resolve user identity
        String userId;
        String userRole;

        var freelancer = freelancerRepository.findByEmail(email);
        if (freelancer.isPresent()) {
            userId = freelancer.get().getId();
            userRole = "FREELANCER";
        } else {
            var company = companyRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
            userId = company.getId();
            userRole = "COMPANY";
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
        return feedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(f -> f.getComment() != null && !f.getComment().isBlank())
                .map(f -> {
                    String userName;
                    String userPhoto = null;
                    if ("FREELANCER".equals(f.getUserRole())) {
                        var opt = freelancerRepository.findById(f.getUserId());
                        userName = opt.map(fr -> fr.getFirstName() + " " + fr.getLastName()).orElse("Freelancer");
                        userPhoto = opt.map(fr -> fr.getProfilePicture()).orElse(null);
                    } else {
                        var opt = companyRepository.findById(f.getUserId());
                        userName = opt.map(c -> c.getCompanyName()).orElse("Company");
                        userPhoto = opt.map(c -> c.getCompanyLogo()).orElse(null);
                    }
                    return new FeedbackPublicDto(
                            f.getId(), f.getUserRole(), userName, userPhoto,
                            f.getRating(), f.getComment(), f.getCreatedAt()
                    );
                })
                .toList();
    }

    // ─── Admin operations ─────────────────────────────────────────────────────

    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Feedback> getPendingFeedbacks() {
        return feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.PENDING);
    }

    public Feedback validateFeedback(String id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + id));
        feedback.setStatus(FeedbackStatus.VALIDATED);
        return feedbackRepository.save(feedback);
    }

    public void deleteFeedback(String id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + id));
        feedbackRepository.delete(feedback);
    }
}
