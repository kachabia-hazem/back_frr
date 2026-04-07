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
            body.put("location",           nullSafe(freelancer.getLocation()));
            body.put("city",               nullSafe(freelancer.getCity()));
            body.put("country",            nullSafe(freelancer.getCountry()));
            body.put("completedProjects",  freelancer.getCompletedProjects());
            body.put("rating",             freelancer.getRating());
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

    // ── Recommandation de missions pour un freelancer ─────────────────────────
    @SuppressWarnings("unchecked")
    public List<AiSearchResult> recommendMissions(com.hazem.worklink.models.Freelancer freelancer) {
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

            List<Map<String, Object>> response = restTemplate.postForObject(
                    aiServiceUrl + "/recommend-missions", body, List.class);

            if (response == null) return Collections.emptyList();

            return response.stream()
                    .map(r -> new AiSearchResult(
                            (String) r.get("mission_id"),
                            ((Number) r.get("score")).doubleValue()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[AI-RECOMMEND] Error calling recommend-missions: {}", e.getMessage());
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

    // ── Mission Matching (rapide, sans LLM ~2s) ──────────────────────────────
    @SuppressWarnings("unchecked")
    public MatchMissionResponse matchMissionQuick(com.hazem.worklink.models.Freelancer freelancer,
                                                  com.hazem.worklink.models.Mission mission) {
        return callMatchEndpoint(freelancer, mission, "/match-mission-quick");
    }

    // ── Mission Matching complet (avec LLM ~30s) ──────────────────────────────
    @SuppressWarnings("unchecked")
    public MatchMissionResponse matchMission(com.hazem.worklink.models.Freelancer freelancer,
                                             com.hazem.worklink.models.Mission mission) {
        return callMatchEndpoint(freelancer, mission, "/match-mission");
    }

    @SuppressWarnings("unchecked")
    private MatchMissionResponse callMatchEndpoint(com.hazem.worklink.models.Freelancer freelancer,
                                                   com.hazem.worklink.models.Mission mission,
                                                   String endpoint) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("freelancerSkills",    freelancer.getSkills() != null ? freelancer.getSkills() : List.of());
            body.put("freelancerBio",       nullSafe(freelancer.getBio()));
            body.put("freelancerPosition",  nullSafe(freelancer.getCurrentPosition()));
            body.put("freelancerExperience", freelancer.getYearsOfExperience());

            // Work experience
            List<Map<String, Object>> weList = new java.util.ArrayList<>();
            if (freelancer.getWorkExperience() != null) {
                for (var we : freelancer.getWorkExperience()) {
                    Map<String, Object> weMap = new HashMap<>();
                    weMap.put("jobTitle",    we.getJobTitle() != null ? we.getJobTitle() : "");
                    weMap.put("company",     we.getCompany() != null ? we.getCompany() : "");
                    weMap.put("description", we.getDescription() != null ? we.getDescription() : "");
                    weMap.put("isCurrent",   Boolean.TRUE.equals(we.getIsCurrent()));
                    weList.add(weMap);
                }
            }
            body.put("workExperience", weList);

            // Projects
            List<Map<String, Object>> projList = new java.util.ArrayList<>();
            if (freelancer.getProjects() != null) {
                for (var p : freelancer.getProjects()) {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("name",         p.getName() != null ? p.getName() : "");
                    pMap.put("description",  p.getDescription() != null ? p.getDescription() : "");
                    pMap.put("technologies", p.getTechnologies() != null ? p.getTechnologies() : List.of());
                    projList.add(pMap);
                }
            }
            body.put("projects", projList);

            // Mission data
            body.put("missionTitle",               nullSafe(mission.getJobTitle()));
            body.put("missionDescription",         nullSafe(mission.getDescription()));
            body.put("missionRequiredSkills",       nullSafe(mission.getRequiredSkills()));
            body.put("missionTechnicalEnvironment", nullSafe(mission.getTechnicalEnvironment()));

            Map<String, Object> response = restTemplate.postForObject(
                    aiServiceUrl + endpoint, body, Map.class);

            if (response == null) throw new RuntimeException("Null response from AI service");

            return new MatchMissionResponse(
                    ((Number) response.getOrDefault("score", 0)).intValue(),
                    ((Number) response.getOrDefault("skillScore", 0)).intValue(),
                    ((Number) response.getOrDefault("semanticScore", 0)).intValue(),
                    (List<String>) response.getOrDefault("matchedSkills", List.of()),
                    (List<String>) response.getOrDefault("missingSkills", List.of()),
                    (String) response.getOrDefault("recommendation", ""),
                    (String) response.getOrDefault("explanation", "")
            );
        } catch (Exception e) {
            log.error("[AI-MATCH] Error calling {}: {}", endpoint, e.getMessage());
            throw new RuntimeException("AI matching service unavailable: " + e.getMessage());
        }
    }

    // ── Rank Candidates ───────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<RankCandidateResult> rankCandidates(com.hazem.worklink.models.Mission mission,
                                                     List<Map<String, Object>> candidates) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("missionId",                    nullSafe(mission.getId()));
            body.put("missionTitle",                 nullSafe(mission.getJobTitle()));
            body.put("missionDescription",           nullSafe(mission.getDescription()));
            body.put("missionRequiredSkills",        nullSafe(mission.getRequiredSkills()));
            body.put("missionTechnicalEnvironment",  nullSafe(mission.getTechnicalEnvironment()));
            body.put("missionYearsOfExperience",     mission.getYearsOfExperience());
            body.put("candidates",                   candidates);

            List<Map<String, Object>> response = restTemplate.postForObject(
                    aiServiceUrl + "/rank-candidates", body, List.class);

            if (response == null) return Collections.emptyList();

            return response.stream().map(r -> new RankCandidateResult(
                    (String)  r.get("applicationId"),
                    (String)  r.get("freelancerId"),
                    ((Number) r.get("rank")).intValue(),
                    ((Number) r.get("totalScore")).doubleValue(),
                    ((Number) r.get("skillScore")).doubleValue(),
                    ((Number) r.get("experienceScore")).doubleValue(),
                    ((Number) r.get("semanticScore")).doubleValue(),
                    ((Number) r.get("completenessScore")).doubleValue(),
                    (List<String>) r.getOrDefault("matchedSkills", List.of()),
                    (List<String>) r.getOrDefault("missingSkills", List.of())
            )).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[AI-RANK] Error calling rank-candidates: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    // ── DTOs internes ─────────────────────────────────────────────────────────
    public record AiSearchResult(String missionId, double score) {}
    public record AiFreelancerSearchResult(String freelancerId, double score) {}
    public record RankCandidateResult(
            String applicationId,
            String freelancerId,
            int rank,
            double totalScore,
            double skillScore,
            double experienceScore,
            double semanticScore,
            double completenessScore,
            List<String> matchedSkills,
            List<String> missingSkills
    ) {}
    public record MatchMissionResponse(
            int score,
            int skillScore,
            int semanticScore,
            List<String> matchedSkills,
            List<String> missingSkills,
            String recommendation,
            String explanation
    ) {}
}
