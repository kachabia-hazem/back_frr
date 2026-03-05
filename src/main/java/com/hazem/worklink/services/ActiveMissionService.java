package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.CreateTaskRequest;
import com.hazem.worklink.dto.request.UpdateTaskRequest;
import com.hazem.worklink.dto.response.GitActivityResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.*;
import com.hazem.worklink.models.enums.ActiveMissionStatus;
import com.hazem.worklink.models.enums.TaskStatus;
import com.hazem.worklink.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveMissionService {

    private final ActiveMissionRepository activeMissionRepository;
    private final TaskRepository taskRepository;
    private final DeliverableRepository deliverableRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final FileStorageService fileStorageService;
    private final GitHubService gitHubService;

    // ─── Create from contract (triggered after company signs) ────────────────

    public ActiveMission createFromContract(Contract contract) {
        // Avoid duplicates
        if (activeMissionRepository.findByContractId(contract.getId()).isPresent()) {
            log.info("ActiveMission already exists for contract {}", contract.getId());
            return activeMissionRepository.findByContractId(contract.getId()).get();
        }

        ActiveMission mission = new ActiveMission();
        mission.setContractId(contract.getId());
        mission.setFreelancerId(contract.getFreelancerId());
        mission.setCompanyId(contract.getCompanyId());
        mission.setTitle(contract.getMissionTitle());
        mission.setDescription("Mission created from contract #" + contract.getId());
        mission.setStatus(ActiveMissionStatus.ACTIVE);
        mission.setProgress(0);
        mission.setStartDate(contract.getStartDate());
        mission.setEndDate(contract.getEndDate());
        mission.setCreatedAt(LocalDateTime.now());

        ActiveMission saved = activeMissionRepository.save(mission);
        log.info("ActiveMission created: {} for contract {}", saved.getId(), contract.getId());
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

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void assertFreelancer(ActiveMission mission, String email) {
        boolean isFreelancer = freelancerRepository.findByEmail(email)
                .map(f -> f.getId().equals(mission.getFreelancerId())).orElse(false);
        if (!isFreelancer) {
            throw new RuntimeException("Only the freelancer assigned to this mission can perform this action");
        }
    }
}
