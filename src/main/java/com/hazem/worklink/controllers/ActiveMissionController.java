package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.CreateTaskRequest;
import com.hazem.worklink.dto.request.ExtendDeadlineRequest;
import com.hazem.worklink.dto.request.SubmitMissionRequest;
import com.hazem.worklink.dto.request.UpdateTaskRequest;
import com.hazem.worklink.dto.request.ValidateMissionRequest;
import com.hazem.worklink.dto.response.GitActivityResponse;
import com.hazem.worklink.models.ActiveMission;
import com.hazem.worklink.models.Deliverable;
import com.hazem.worklink.models.Task;
import com.hazem.worklink.models.enums.ActiveMissionStatus;
import com.hazem.worklink.services.ActiveMissionService;
import com.hazem.worklink.services.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/active-missions")
@RequiredArgsConstructor
public class ActiveMissionController {

    private final ActiveMissionService activeMissionService;
    private final ContractService contractService;

    // ─── Mission listings ─────────────────────────────────────────────────────

    @GetMapping("/freelancer")
    public ResponseEntity<List<ActiveMission>> getFreelancerMissions(Authentication auth) {
        return ResponseEntity.ok(activeMissionService.getFreelancerMissions(auth.getName()));
    }

    @GetMapping("/company")
    public ResponseEntity<List<ActiveMission>> getCompanyMissions(Authentication auth) {
        return ResponseEntity.ok(activeMissionService.getCompanyMissions(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActiveMission> getMission(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(activeMissionService.getByIdForUser(id, auth.getName()));
    }

    // ─── Kanban Tasks ─────────────────────────────────────────────────────────

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<Task>> getTasks(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(activeMissionService.getTasks(id, auth.getName()));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<Task> createTask(@PathVariable String id,
                                           @RequestBody CreateTaskRequest req,
                                           Authentication auth) {
        return ResponseEntity.ok(activeMissionService.createTask(id, req, auth.getName()));
    }

    @PutMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<Task> updateTask(@PathVariable String id,
                                           @PathVariable String taskId,
                                           @RequestBody UpdateTaskRequest req,
                                           Authentication auth) {
        return ResponseEntity.ok(activeMissionService.updateTask(id, taskId, req, auth.getName()));
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id,
                                           @PathVariable String taskId,
                                           Authentication auth) {
        activeMissionService.deleteTask(id, taskId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // ─── Deliverables ─────────────────────────────────────────────────────────

    @GetMapping("/{id}/deliverables")
    public ResponseEntity<List<Deliverable>> getDeliverables(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(activeMissionService.getDeliverables(id, auth.getName()));
    }

    @PostMapping("/{id}/deliverables")
    public ResponseEntity<Deliverable> uploadDeliverable(@PathVariable String id,
                                                         @RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "description", required = false) String description,
                                                         Authentication auth) {
        return ResponseEntity.ok(activeMissionService.uploadDeliverable(id, file, description, auth.getName()));
    }

    // ─── Git Activity ─────────────────────────────────────────────────────────

    @GetMapping("/{id}/git-validate")
    public ResponseEntity<Map<String, Object>> validateGitUrl(@PathVariable String id,
                                                               @RequestParam String url,
                                                               Authentication auth) {
        return ResponseEntity.ok(activeMissionService.validateGitUrl(id, url, auth.getName()));
    }

    @PutMapping("/{id}/git-repo")
    public ResponseEntity<ActiveMission> setGitRepoUrl(@PathVariable String id,
                                                        @RequestBody Map<String, String> body,
                                                        Authentication auth) {
        String repoUrl = body.get("gitRepositoryUrl");
        return ResponseEntity.ok(activeMissionService.setGitRepoUrl(id, repoUrl, auth.getName()));
    }

    @GetMapping("/{id}/git-refresh")
    public ResponseEntity<GitActivityResponse> refreshGitActivity(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(activeMissionService.refreshGitActivity(id, auth.getName()));
    }

    // ─── Status ───────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    public ResponseEntity<ActiveMission> updateStatus(@PathVariable String id,
                                                       @RequestBody Map<String, String> body,
                                                       Authentication auth) {
        ActiveMissionStatus status = ActiveMissionStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(activeMissionService.updateStatus(id, status, auth.getName()));
    }

    // ─── Mission Validation ───────────────────────────────────────────────────

    /** Freelancer: mark mission as done and request company validation */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ActiveMission> submitMission(@PathVariable String id,
                                                        @RequestBody SubmitMissionRequest req,
                                                        Authentication auth) {
        return ResponseEntity.ok(activeMissionService.submitMission(id, req, auth.getName()));
    }

    /** Company: approve or request revision for a submitted mission */
    @PostMapping("/{id}/validate")
    public ResponseEntity<ActiveMission> validateMission(@PathVariable String id,
                                                          @RequestBody ValidateMissionRequest req,
                                                          Authentication auth) {
        return ResponseEntity.ok(activeMissionService.validateMission(id, req, auth.getName()));
    }

    /** Company: list missions pending validation */
    @GetMapping("/pending-validation")
    public ResponseEntity<List<ActiveMission>> getPendingValidations(Authentication auth) {
        return ResponseEntity.ok(activeMissionService.getPendingValidations(auth.getName()));
    }

    /** Freelancer: remove a completed mission from history */
    @DeleteMapping("/{id}/history")
    public ResponseEntity<Void> deleteFromHistory(@PathVariable String id, Authentication auth) {
        activeMissionService.deleteFromHistory(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    /** Company: extend deadline of an overdue ACTIVE mission and reset contract for re-signing */
    @PostMapping("/{id}/extend")
    public ResponseEntity<ActiveMission> extendDeadline(@PathVariable String id,
                                                         @RequestBody ExtendDeadlineRequest req,
                                                         Authentication auth) {
        // 1. Update mission end date
        ActiveMission updated = activeMissionService.extendMissionDeadline(id, req.getNewEndDate(), auth.getName());

        // 2. Reset the linked contract so freelancer must sign again
        if (updated.getContractId() != null) {
            contractService.extendContract(updated.getContractId(), req.getNewEndDate(), req.getAdjustedPayment());
        }

        return ResponseEntity.ok(updated);
    }
}
