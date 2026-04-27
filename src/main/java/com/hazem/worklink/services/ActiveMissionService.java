package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.CreateTaskRequest;
import com.hazem.worklink.dto.request.SubmitMissionRequest;
import com.hazem.worklink.dto.request.UpdateTaskRequest;
import com.hazem.worklink.dto.request.ValidateMissionRequest;
import com.hazem.worklink.dto.response.GitActivityResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.*;
import com.hazem.worklink.models.enums.ActiveMissionStatus;
import com.hazem.worklink.models.enums.ContractStatus;
import com.hazem.worklink.models.enums.TaskStatus;
import com.hazem.worklink.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveMissionService {

    private final ActiveMissionRepository activeMissionRepository;
    private final ContractRepository contractRepository;
    private final TaskRepository taskRepository;
    private final DeliverableRepository deliverableRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final FileStorageService fileStorageService;
    private final GitHubService gitHubService;
    private final NotificationService notificationService;
    private final ReviewRepository reviewRepository;
    private final StripeService stripeService;

    // ─── Create from contract (triggered after company signs) ────────────────

    public ActiveMission createFromContract(Contract contract) {
        // If mission already exists (e.g. after a deadline extension + re-sign), update it
        if (activeMissionRepository.findByContractId(contract.getId()).isPresent()) {
            ActiveMission existing = activeMissionRepository.findByContractId(contract.getId()).get();
            if (existing.getStatus() == ActiveMissionStatus.ACTIVE || existing.getStatus() == ActiveMissionStatus.PENDING) {
                // Update end date and salary in case they changed
                existing.setEndDate(contract.getEndDate());
                existing.setStartDate(contract.getStartDate());
                if (contract.getSalary() != null) existing.setSalary(contract.getSalary());
                ActiveMission saved = activeMissionRepository.save(existing);
                log.info("ActiveMission {} updated from contract {}", saved.getId(), contract.getId());
                return saved;
            }
            log.info("ActiveMission already exists for contract {} with status {}", contract.getId(), existing.getStatus());
            return existing;
        }

        // Determine initial status: PENDING if start date is in the future, ACTIVE otherwise
        boolean startDateInFuture = contract.getStartDate() != null
                && contract.getStartDate().isAfter(LocalDate.now());
        ActiveMissionStatus initialStatus = startDateInFuture
                ? ActiveMissionStatus.PENDING
                : ActiveMissionStatus.ACTIVE;

        ActiveMission mission = new ActiveMission();
        mission.setContractId(contract.getId());
        mission.setFreelancerId(contract.getFreelancerId());
        mission.setCompanyId(contract.getCompanyId());
        mission.setTitle(contract.getMissionTitle());
        mission.setDescription("Mission created from contract #" + contract.getId());
        mission.setStatus(initialStatus);
        mission.setProgress(0);
        mission.setStartDate(contract.getStartDate());
        mission.setEndDate(contract.getEndDate());
        mission.setSalary(contract.getSalary());
        mission.setCreatedAt(LocalDateTime.now());

        ActiveMission saved = activeMissionRepository.save(mission);
        log.info("ActiveMission created: {} for contract {} with status {}", saved.getId(), contract.getId(), initialStatus);
        return saved;
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    public List<ActiveMission> getFreelancerMissions(String email) {
        Freelancer freelancer = freelancerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        return activeMissionRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancer.getId());
    }

    public List<ActiveMission> getCompanyMissions(String email) {
        Company company = companyRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return activeMissionRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId());
    }

    public ActiveMission getByIdForUser(String missionId, String userEmail) {
        ActiveMission mission = activeMissionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Active mission not found: " + missionId));

        boolean isFreelancer = freelancerRepository.findByEmail(userEmail)
                .map(f -> f.getId().equals(mission.getFreelancerId())).orElse(false);
        boolean isCompany = companyRepository.findByEmail(userEmail)
                .map(c -> c.getId().equals(mission.getCompanyId())).orElse(false);

        if (!isFreelancer && !isCompany) {
            throw new RuntimeException("Unauthorized access to active mission");
        }
        return mission;
    }

    // ─── Kanban Tasks ─────────────────────────────────────────────────────────

    public List<Task> getTasks(String missionId, String userEmail) {
        getByIdForUser(missionId, userEmail); // authorization check
        return taskRepository.findByMissionIdOrderByOrderIndexAsc(missionId);
    }

    public Task createTask(String missionId, CreateTaskRequest req, String email) {
        ActiveMission mission = getByIdForUser(missionId, email);
        assertFreelancer(mission, email);

        List<Task> existing = taskRepository.findByMissionIdOrderByOrderIndexAsc(missionId);
        int nextIndex = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getOrderIndex() + 1;

        Task task = new Task();
        task.setMissionId(missionId);
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setStatus(TaskStatus.TODO);
        task.setOrderIndex(nextIndex);
        task.setCreatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);
        recalculateProgress(missionId);
        return saved;
    }

    public Task updateTask(String missionId, String taskId, UpdateTaskRequest req, String email) {
        ActiveMission mission = getByIdForUser(missionId, email);
        assertFreelancer(mission, email);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        if (!task.getMissionId().equals(missionId)) {
            throw new RuntimeException("Task does not belong to this mission");
        }

        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getStatus() != null) task.setStatus(req.getStatus());

        Task saved = taskRepository.save(task);
        recalculateProgress(missionId);
        return saved;
    }

    public void deleteTask(String missionId, String taskId, String email) {
        ActiveMission mission = getByIdForUser(missionId, email);
        assertFreelancer(mission, email);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        if (!task.getMissionId().equals(missionId)) {
            throw new RuntimeException("Task does not belong to this mission");
        }

        taskRepository.delete(task);
        recalculateProgress(missionId);
    }

    private void recalculateProgress(String missionId) {
        List<Task> tasks = taskRepository.findByMissionIdOrderByOrderIndexAsc(missionId);
        if (tasks.isEmpty()) {
            updateProgressValue(missionId, 0);
            return;
        }
        long done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        int progress = (int) ((done * 100) / tasks.size());
        updateProgressValue(missionId, progress);
    }

    private void updateProgressValue(String missionId, int progress) {
        activeMissionRepository.findById(missionId).ifPresent(m -> {
            m.setProgress(progress);
            activeMissionRepository.save(m);
        });
    }

    // ─── Git Activity ─────────────────────────────────────────────────────────

    public Map<String, Object> validateGitUrl(String missionId, String url, String email) {
        getByIdForUser(missionId, email); // authorization check
        return gitHubService.validateRepo(url);
    }

    public ActiveMission setGitRepoUrl(String missionId, String repoUrl, String email) {
        ActiveMission mission = getByIdForUser(missionId, email);
        assertFreelancer(mission, email);
        mission.setGitRepositoryUrl(repoUrl);
        return activeMissionRepository.save(mission);
    }

    public GitActivityResponse refreshGitActivity(String missionId, String userEmail) {
        ActiveMission mission = getByIdForUser(missionId, userEmail);
        assertFreelancer(mission, userEmail);

        if (mission.getGitRepositoryUrl() == null || mission.getGitRepositoryUrl().isBlank()) {
            throw new IllegalArgumentException("No GitHub repository URL set for this mission");
        }

        GitActivityResponse response = gitHubService.fetchGitActivity(mission.getGitRepositoryUrl());

        // Persist the fetched data
        mission.setGitCurrentBranch(response.getBranch());
        mission.setGitCommitCount(response.getCommitCount());
        mission.setGitLastPushDate(response.getLastPushDate());
        mission.setGitLastCommitMessage(response.getLastCommitMessage());
        activeMissionRepository.save(mission);

        return response;
    }

    // ─── Deliverables ─────────────────────────────────────────────────────────

    public List<Deliverable> getDeliverables(String missionId, String userEmail) {
        getByIdForUser(missionId, userEmail); // authorization check
        return deliverableRepository.findByMissionIdOrderByUploadedAtDesc(missionId);
    }

    public Deliverable uploadDeliverable(String missionId, MultipartFile file, String description, String email) {
        ActiveMission mission = getByIdForUser(missionId, email);
        assertFreelancer(mission, email);

        String fileUrl = fileStorageService.storeDeliverable(file);

        Deliverable deliverable = new Deliverable();
        deliverable.setMissionId(missionId);
        deliverable.setFreelancerId(mission.getFreelancerId());
        deliverable.setFileUrl(fileUrl);
        deliverable.setFileName(file.getOriginalFilename());
        deliverable.setDescription(description);
        deliverable.setUploadedAt(LocalDateTime.now());

        return deliverableRepository.save(deliverable);
    }

    // ─── Status ───────────────────────────────────────────────────────────────

    public ActiveMission updateStatus(String missionId, ActiveMissionStatus status, String email) {
        ActiveMission mission = getByIdForUser(missionId, email);
        mission.setStatus(status);
        return activeMissionRepository.save(mission);
    }

    // ─── Mission Validation ───────────────────────────────────────────────────

    /**
     * Freelancer marks the mission as done and submits it for company validation.
     * Can only be called when status is ACTIVE or PAUSED.
     */
    public ActiveMission submitMission(String missionId, SubmitMissionRequest req, String email) {
        ActiveMission mission = getByIdForUser(missionId, email);
        assertFreelancer(mission, email);

        if (mission.getStatus() != ActiveMissionStatus.ACTIVE && mission.getStatus() != ActiveMissionStatus.PAUSED) {
            throw new IllegalStateException("Mission must be ACTIVE or PAUSED to submit for validation");
        }

        mission.setStatus(ActiveMissionStatus.SUBMITTED);
        mission.setSubmittedAt(LocalDateTime.now());
        mission.setSubmittedNote(req.getNote());
        ActiveMission saved = activeMissionRepository.save(mission);

        // Notify the company
        companyRepository.findById(mission.getCompanyId()).ifPresent(company -> {
            String freelancerName = getFreelancerName(mission.getFreelancerId());
            notificationService.sendMissionSubmittedNotification(
                    company.getId(), mission.getTitle(), freelancerName, missionId);
        });

        return saved;
    }

    /**
     * Company validates (approves or requests revision) a submitted mission.
     * Can also be called after the end date even if freelancer hasn't submitted yet.
     */
    public ActiveMission validateMission(String missionId, ValidateMissionRequest req, String email) {
        ActiveMission mission = getByIdForUser(missionId, email);
        assertCompany(mission, email);

        boolean isSubmitted = mission.getStatus() == ActiveMissionStatus.SUBMITTED;
        boolean deadlinePassed = mission.getEndDate() != null && !LocalDate.now().isBefore(mission.getEndDate());

        if (!isSubmitted && !deadlinePassed) {
            throw new IllegalStateException(
                    "Validation is only available after the mission end date or after the freelancer submits");
        }

        if (req.isApproved()) {
            mission.setStatus(ActiveMissionStatus.COMPLETED);
            mission.setValidatedAt(LocalDateTime.now());
            mission.setValidationRating(req.getRating());
        } else {
            // Revision requested: reopen the mission
            mission.setStatus(ActiveMissionStatus.ACTIVE);
            mission.setSubmittedAt(null);
            mission.setSubmittedNote(null);
        }
        mission.setValidationNote(req.getNote());
        ActiveMission saved = activeMissionRepository.save(mission);

        // Mark the linked contract as FINISHED and release escrow payment when mission is approved
        if (req.isApproved()) {
            contractRepository.findById(mission.getContractId()).ifPresent(contract -> {
                contract.setStatus(ContractStatus.FINISHED);
                contract.setFinishedAt(LocalDateTime.now());
                contractRepository.save(contract);
                log.info("Contract {} marked as FINISHED after mission {} validation", contract.getId(), missionId);

                // Release escrow: capture the held payment and credit freelancer
                try {
                    stripeService.captureContractPayment(contract.getId());
                } catch (Exception e) {
                    log.error("Failed to capture Stripe payment for contract {}: {}", contract.getId(), e.getMessage());
                }
            });
        }

        // If approved, create a review and update freelancer stats
        if (req.isApproved() && req.getRating() != null && !reviewRepository.existsByMissionId(missionId)) {
            companyRepository.findById(mission.getCompanyId()).ifPresent(company -> {
                Review review = new Review();
                review.setMissionId(missionId);
                review.setFreelancerId(mission.getFreelancerId());
                review.setCompanyId(mission.getCompanyId());
                review.setCompanyName(company.getCompanyName());
                review.setCompanyLogo(company.getCompanyLogo());
                review.setRating(req.getRating());
                review.setComment(req.getNote());
                review.setCreatedAt(LocalDateTime.now());
                reviewRepository.save(review);
            });

            freelancerRepository.findById(mission.getFreelancerId()).ifPresent(freelancer -> {
                List<Review> allReviews = reviewRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancer.getId());
                double avg = allReviews.stream()
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0);
                // Round to 1 decimal
                freelancer.setRating(Math.round(avg * 10.0) / 10.0);
                freelancer.setReviewCount(allReviews.size());
                freelancer.setCompletedProjects(
                        (freelancer.getCompletedProjects() == null ? 0 : freelancer.getCompletedProjects()) + 1);
                freelancerRepository.save(freelancer);
            });
        }

        // Notify the freelancer
        String companyName = getCompanyName(mission.getCompanyId());
        freelancerRepository.findById(mission.getFreelancerId()).ifPresent(freelancer ->
                notificationService.sendMissionValidatedNotification(
                        freelancer.getId(), mission.getTitle(), companyName, req.isApproved(), req.getNote(), missionId));

        return saved;
    }

    /**
     * Company extends the deadline for an overdue ACTIVE mission.
     * Returns the updated mission.
     */
    public ActiveMission extendMissionDeadline(String missionId, LocalDate newEndDate, String companyEmail) {
        ActiveMission mission = getByIdForUser(missionId, companyEmail);
        assertCompany(mission, companyEmail);

        if (mission.getStatus() != ActiveMissionStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE missions can have their deadline extended");
        }
        if (newEndDate == null) {
            throw new IllegalArgumentException("New end date is required");
        }

        mission.setEndDate(newEndDate);
        ActiveMission saved = activeMissionRepository.save(mission);
        log.info("Mission {} deadline extended to {} by company", missionId, newEndDate);
        return saved;
    }

    /**
     * Freelancer removes a COMPLETED mission from their history.
     * Only allowed when status is COMPLETED.
     */
    public void deleteFromHistory(String missionId, String email) {
        ActiveMission mission = activeMissionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Active mission not found: " + missionId));
        assertFreelancer(mission, email);

        if (mission.getStatus() != ActiveMissionStatus.COMPLETED) {
            throw new IllegalStateException("Only completed missions can be removed from history");
        }

        taskRepository.deleteByMissionId(missionId);
        deliverableRepository.deleteByMissionId(missionId);
        activeMissionRepository.delete(mission);
        log.info("Freelancer removed completed mission {} from history", missionId);
    }

    /**
     * Returns all missions submitted by freelancers that are awaiting this company's validation.
     */
    public List<ActiveMission> getPendingValidations(String email) {
        Company company = companyRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return activeMissionRepository.findByCompanyIdAndStatusOrderBySubmittedAtDesc(
                company.getId(), ActiveMissionStatus.SUBMITTED);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void assertFreelancer(ActiveMission mission, String email) {
        boolean isFreelancer = freelancerRepository.findByEmail(email)
                .map(f -> f.getId().equals(mission.getFreelancerId())).orElse(false);
        if (!isFreelancer) {
            throw new RuntimeException("Only the freelancer assigned to this mission can perform this action");
        }
    }

    private void assertCompany(ActiveMission mission, String email) {
        boolean isCompany = companyRepository.findByEmail(email)
                .map(c -> c.getId().equals(mission.getCompanyId())).orElse(false);
        if (!isCompany) {
            throw new RuntimeException("Only the company that owns this mission can perform this action");
        }
    }

    private String getFreelancerName(String freelancerId) {
        return freelancerRepository.findById(freelancerId)
                .map(f -> f.getFirstName() + " " + f.getLastName())
                .orElse("Freelancer");
    }

    private String getCompanyName(String companyId) {
        return companyRepository.findById(companyId)
                .map(Company::getCompanyName)
                .orElse("Company");
    }
}
