package com.hazem.worklink.services;

import com.hazem.worklink.dto.response.GitActivityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GitHubService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${github.token:}")
    private String githubToken;

    private static final String GITHUB_API = "https://api.github.com";

    /**
     * Parses a GitHub repo URL (https://github.com/owner/repo) into owner/repo format.
     */
    private String[] parseRepoUrl(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("GitHub repository URL is required");
        }
        String cleaned = repoUrl.trim().replaceAll("/$", "").replace(".git", "");
        String[] parts = cleaned.split("github\\.com/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub URL: " + repoUrl);
        }
        String[] ownerRepo = parts[1].split("/");
        if (ownerRepo.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub URL format — expected owner/repo");
        }
        return new String[]{ownerRepo[0], ownerRepo[1]};
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        if (githubToken != null && !githubToken.isBlank()) {
            headers.set("Authorization", "Bearer " + githubToken);
        }
        return headers;
    }

    /**
     * Validates that a GitHub URL is well-formed AND that the repository is accessible via the API.
     * Returns a map with "valid" (boolean) and "message" (string).
     */
    public Map<String, Object> validateRepo(String repoUrl) {
        String[] ownerRepo;
        try {
            ownerRepo = parseRepoUrl(repoUrl);
        } catch (IllegalArgumentException e) {
            return Map.of("valid", false, "message", e.getMessage());
        }
        String owner = ownerRepo[0];
        String repo  = ownerRepo[1];
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    GITHUB_API + "/repos/" + owner + "/" + repo,
                    HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return Map.of("valid", true, "message", "Repository found: " + owner + "/" + repo);
            }
            return Map.of("valid", false, "message", "Repository not accessible");
        } catch (Exception e) {
            return Map.of("valid", false, "message", "Repository not found or not accessible: " + owner + "/" + repo);
        }
    }

    public GitActivityResponse fetchGitActivity(String repoUrl) {
        String[] ownerRepo = parseRepoUrl(repoUrl);
        String owner = ownerRepo[0];
        String repo = ownerRepo[1];

        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

        // Fetch default branch from repo info
        String branch = "main";
        Integer commitCount = null;
        String lastCommitMessage = null;
        LocalDateTime lastPushDate = null;

        try {
            ResponseEntity<Map> repoInfo = restTemplate.exchange(
                    GITHUB_API + "/repos/" + owner + "/" + repo,
                    HttpMethod.GET, entity, Map.class);
            if (repoInfo.getBody() != null) {
                Object defaultBranch = repoInfo.getBody().get("default_branch");
                if (defaultBranch != null) {
                    branch = defaultBranch.toString();
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch repo info for {}/{}: {}", owner, repo, e.getMessage());
        }

        // Fetch last commit
        try {
            ResponseEntity<List> commitsResponse = restTemplate.exchange(
                    GITHUB_API + "/repos/" + owner + "/" + repo + "/commits?per_page=1",
                    HttpMethod.GET, entity, List.class);
            if (commitsResponse.getBody() != null && !commitsResponse.getBody().isEmpty()) {
                Map commit = (Map) commitsResponse.getBody().get(0);
                Map commitData = (Map) commit.get("commit");
                if (commitData != null) {
                    Map authorData = (Map) commitData.get("author");
                    lastCommitMessage = (String) commitData.get("message");
                    if (lastCommitMessage != null && lastCommitMessage.length() > 200) {
                        lastCommitMessage = lastCommitMessage.substring(0, 200);
                    }
                    if (authorData != null && authorData.get("date") != null) {
                        String dateStr = authorData.get("date").toString();
                        try {
                            lastPushDate = LocalDateTime.parse(dateStr,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
                        } catch (Exception ex) {
                            log.debug("Could not parse commit date: {}", dateStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch commits for {}/{}: {}", owner, repo, e.getMessage());
        }

        // Fetch commit count (last 100)
        try {
            ResponseEntity<List> allCommits = restTemplate.exchange(
                    GITHUB_API + "/repos/" + owner + "/" + repo + "/commits?per_page=100",
                    HttpMethod.GET, entity, List.class);
            if (allCommits.getBody() != null) {
                commitCount = allCommits.getBody().size();
            }
        } catch (Exception e) {
            log.warn("Could not fetch commit count for {}/{}: {}", owner, repo, e.getMessage());
        }

        return new GitActivityResponse(lastCommitMessage, lastPushDate, branch, commitCount);
    }
}
