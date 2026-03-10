package com.hazem.worklink.services;

import com.hazem.worklink.models.Mission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

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
            Map<String, Object> body = Map.of(
                    "id",                    mission.getId(),
                    "jobTitle",              nullSafe(mission.getJobTitle()),
                    "field",                 nullSafe(mission.getField()),
                    "description",           nullSafe(mission.getDescription()),
                    "requiredSkills",        nullSafe(mission.getRequiredSkills()),
                    "technicalEnvironment",  nullSafe(mission.getTechnicalEnvironment()),
                    "missionBusinessSector", nullSafe(mission.getMissionBusinessSector()),
                    "speciality",            nullSafe(mission.getSpeciality())
            );
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

    // ── Extraction CV via Ollama ──────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> extractCvData(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "cv.pdf";

            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() { return filename; }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    aiServiceUrl + "/extract-cv", requestEntity, Map.class);

            return response != null ? response : Collections.emptyMap();
        } catch (Exception e) {
            log.error("[AI-EXTRACT] Error extracting CV: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    // ── DTOs internes ─────────────────────────────────────────────────────────
    public record AiSearchResult(String missionId, double score) {}
    public record AiFreelancerSearchResult(String freelancerId, double score) {}
}
