package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.CreateMissionRequest;
import com.hazem.worklink.dto.response.MissionResponse;
import com.hazem.worklink.models.Mission;
import com.hazem.worklink.services.MissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @PostMapping
    public ResponseEntity<Mission> createMission(
            Authentication authentication,
            @Valid @RequestBody CreateMissionRequest request) {
        String email = authentication.getName();
        Mission mission = missionService.createMission(email, request);
        return ResponseEntity.ok(mission);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Mission>> getMyMissions(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(missionService.getMissionsByCompany(email));
    }

    @GetMapping("/public/all")
    public ResponseEntity<List<MissionResponse>> getAllMissions() {
        return ResponseEntity.ok(missionService.getOpenMissionsWithCompany());
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<MissionResponse> getMissionById(@PathVariable String id) {
        return ResponseEntity.ok(missionService.getMissionByIdWithCompany(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mission> updateMission(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody CreateMissionRequest request) {
        String email = authentication.getName();
        Mission mission = missionService.updateMission(email, id, request);
        return ResponseEntity.ok(mission);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMission(
            Authentication authentication,
            @PathVariable String id) {
        String email = authentication.getName();
        missionService.deleteMission(email, id);
        return ResponseEntity.noContent().build();
    }
}
