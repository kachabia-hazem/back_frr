package com.hazem.worklink.services;

import com.hazem.worklink.models.Mission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSearchClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    // ── Recherche sémantique ──────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<AiSearchResult> search(String prompt, int topK) {
        try {
            Map<String, Object> body = Map.of("prompt", prompt, "top_k", topK);
            List<Map<String, Object>> response = restTemplate.postForObject(
                    aiServiceUrl + "/search", body, List.class);

            if (response == null) return Collections.emptyList();

            return response.stream()
                    .map(r -> new AiSearchResult(
                            (String) r.get("mission_id"),
                            ((Number) r.get("score")).doubleValue()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[AI-SEARCH] Error calling AI service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Indexation d'une mission ──────────────────────────────────────────────
    public void indexMission(Mission mission) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("id",                    mission.getId());
            body.put("jobTitle",              nullSafe(mission.getJobTitle()));
            body.put("field",                 nullSafe(mission.getField()));
            body.put("description",           nullSafe(mission.getDescription()));
            body.put("requiredSkills",        nullSafe(mission.getRequiredSkills()));
            body.put("technicalEnvironment",  nullSafe(mission.getTechnicalEnvironment()));
            body.put("missionBusinessSector", nullSafe(mission.getMissionBusinessSector()));
            body.put("speciality",            nullSafe(mission.getSpeciality()));
            body.put("location",              nullSafe(mission.getLocation()));
            body.put("missionType",           nullSafe(mission.getMissionType()));
            restTemplate.postForObject(aiServiceUrl + "/index-mission", body, Map.class);
            log.info("[AI-INDEX] Mission indexed: {}", mission.getId());
        } catch (Exception e) {
            log.warn("[AI-INDEX] Could not index mission {}: {}", mission.getId(), e.getMessage());
        }
    }

    // ── Indexation d'un freelancer ────────────────────────────────────────────
    public void indexFreelancer(com.hazem.worklink.models.Freelancer freelancer) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("id",                freelancer.getId());
            body.put("currentPosition",   nullSafe(freelancer.getCurrentPosition()));
            body.put("skills",            freelancer.getSkills() != null ? freelancer.getSkills() : List.of());
            body.put("bio",               nullSafe(freelancer.getBio()));
            body.put("yearsOfExperience", freelancer.getYearsOfExperience());
            body.put("profileTypes",      freelancer.getProfileTypes() != null
                    ? freelancer.getProfileTypes().stream().map(Enum::name).collect(Collectors.toList())
                    : List.of());
            body.put("location",          nullSafe(freelancer.getLocation()));
            body.put("city",              nullSafe(freelancer.getCity()));
            body.put("country",           nullSafe(freelancer.getCountry()));
            restTemplate.postForObject(aiServiceUrl + "/index-freelancer", body, Map.class);
            log.info("[AI-INDEX] Freelancer indexed: {}", freelancer.getId());
        } catch (Exception e) {
            log.warn("[AI-INDEX] Could not index freelancer {}: {}", freelancer.getId(), e.getMessage());
        }
    }

    // ── Recherche sémantique freelancers ──────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<AiFreelancerSearchResult> searchFreelancers(String prompt, int topK) {
        try {
            Map<String, Object> body = Map.of("prompt", prompt, "top_k", topK);
            List<Map<String, Object>> response = restTemplate.postForObject(
                    aiServiceUrl + "/search-freelancers", body, List.class);

            if (response == null) return Collections.emptyList();

            return response.stream()
                    .map(r -> new AiFreelancerSearchResult(
                            (String) r.get("freelancer_id"),
                            ((Number) r.get("score")).doubleValue()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[AI-SEARCH] Error calling AI service for freelancers: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    // ── DTOs internes ─────────────────────────────────────────────────────────
    public record AiSearchResult(String missionId, double score) {}
    public record AiFreelancerSearchResult(String freelancerId, double score) {}
}
