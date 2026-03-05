package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.CreateTaskRequest;
import com.hazem.worklink.dto.request.UpdateTaskRequest;
import com.hazem.worklink.dto.response.GitActivityResponse;
import com.hazem.worklink.models.ActiveMission;
import com.hazem.worklink.models.Deliverable;
import com.hazem.worklink.models.Task;
import com.hazem.worklink.models.enums.ActiveMissionStatus;
import com.hazem.worklink.services.ActiveMissionService;
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
}
