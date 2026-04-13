package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.UpdateCvDataRequest;
import com.hazem.worklink.dto.request.UpdateFreelancerRequest;
import com.hazem.worklink.dto.response.MissionResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.Review;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.MissionRepository;
import com.hazem.worklink.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FreelancerService {

    private final FreelancerRepository freelancerRepository;
    private final AiSearchClient aiSearchClient;
    private final ReviewRepository reviewRepository;
    private final MissionRepository missionRepository;
    private final CompanyRepository companyRepository;

    public Freelancer getFreelancerById(String id) {
        return freelancerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + id));
    }

    public List<Freelancer> getAllFreelancers() {
        return freelancerRepository.findAll();
    }

    public Freelancer getFreelancerByEmail(String email) {
        return freelancerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + email));
    }

    public Freelancer updateFreelancer(String email, UpdateFreelancerRequest request) {
        Freelancer freelancer = getFreelancerByEmail(email);

        if (request.getFirstName() != null) {
            freelancer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            freelancer.setLastName(request.getLastName());
        }
        if (request.getGender() != null) {
            freelancer.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            freelancer.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPhoneNumber() != null) {
            freelancer.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getYearsOfExperience() != null) {
            freelancer.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getProfileTypes() != null) {
            freelancer.setProfileTypes(request.getProfileTypes());
        }
        if (request.getTjm() != null) {
            freelancer.setTjm(request.getTjm());
        }
        if (request.getLanguages() != null) {
            freelancer.setLanguages(request.getLanguages());
        }
        if (request.getCurrentPosition() != null) {
            freelancer.setCurrentPosition(request.getCurrentPosition());
        }
        if (request.getLocation() != null) {
            freelancer.setLocation(request.getLocation());
        }
        if (request.getBio() != null) {
            freelancer.setBio(request.getBio());
        }
        if (request.getSkills() != null) {
            freelancer.setSkills(request.getSkills());
        }
        if (request.getPortfolioUrl() != null) {
            freelancer.setPortfolioUrl(request.getPortfolioUrl());
        }

        Freelancer saved = freelancerRepository.save(freelancer);
        aiSearchClient.indexFreelancer(saved);
        return saved;
    }

    public Freelancer updateCvData(String email, UpdateCvDataRequest request) {
        Freelancer freelancer = getFreelancerByEmail(email);

        if (request.getBio() != null) {
            freelancer.setBio(request.getBio());
        }
        if (request.getEducation() != null) {
            freelancer.setEducation(request.getEducation());
        }
        if (request.getProjects() != null) {
            freelancer.setProjects(request.getProjects());
        }
        if (request.getSkills() != null) {
            freelancer.setSkills(request.getSkills());
        }
        if (request.getCertifications() != null) {
            freelancer.setCertifications(request.getCertifications());
        }
        if (request.getWorkExperience() != null) {
            freelancer.setWorkExperience(request.getWorkExperience());
        }
        if (request.getLanguages() != null) {
            freelancer.setLanguages(request.getLanguages());
        }

        Freelancer saved = freelancerRepository.save(freelancer);
        aiSearchClient.indexFreelancer(saved);
        return saved;
    }

    public Freelancer updateProfilePicture(String email, String pictureUrl) {
        Freelancer freelancer = getFreelancerByEmail(email);
        freelancer.setProfilePicture(pictureUrl);
        return freelancerRepository.save(freelancer);
    }

    public Freelancer updateCvUrl(String email, String cvUrl) {
        Freelancer freelancer = getFreelancerByEmail(email);
        freelancer.setCvUrl(cvUrl);
        return freelancerRepository.save(freelancer);
    }

    public void incrementProfileViews(String freelancerId) {
        Freelancer freelancer = getFreelancerById(freelancerId);
        freelancer.setProfileViews(freelancer.getProfileViews() + 1);
        freelancerRepository.save(freelancer);
    }

    public void incrementSearchAppearances(List<String> ids) {
        for (String id : ids) {
            freelancerRepository.findById(id).ifPresent(freelancer -> {
                freelancer.setSearchAppearances(freelancer.getSearchAppearances() + 1);
                freelancerRepository.save(freelancer);
            });
        }
    }

    public Freelancer updateCardCustomization(String email, String cardBackground, List<String> portfolioImages) {
        Freelancer freelancer = getFreelancerByEmail(email);
        if (cardBackground != null) {
            freelancer.setCardBackground(cardBackground);
        }
        if (portfolioImages != null) {
            freelancer.setPortfolioImages(portfolioImages);
        }
        return freelancerRepository.save(freelancer);
    }

    // ── AI Semantic Search ────────────────────────────────────────────────────

    public List<AiFreelancerResult> aiSearchFreelancers(String prompt, int topK) {
        List<AiSearchClient.AiFreelancerSearchResult> aiResults = aiSearchClient.searchFreelancers(prompt, topK);

        if (aiResults.isEmpty()) return List.of();

        List<String> ids = aiResults.stream()
                .map(AiSearchClient.AiFreelancerSearchResult::freelancerId)
                .collect(Collectors.toList());

        Map<String, Freelancer> freelancerMap = freelancerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Freelancer::getId, f -> f));

        return aiResults.stream()
                .filter(r -> freelancerMap.containsKey(r.freelancerId()))
                .map(r -> new AiFreelancerResult(freelancerMap.get(r.freelancerId()), r.score()))
                .collect(Collectors.toList());
    }

    public record AiFreelancerResult(Freelancer freelancer, double score) {}

    public List<Review> getFreelancerReviews(String freelancerId) {
        if (!freelancerRepository.existsById(freelancerId)) {
            throw new ResourceNotFoundException("Freelancer not found with id: " + freelancerId);
        }
        return reviewRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId);
    }

    // ─── Saved Missions ───────────────────────────────────────────────────────

    /** Returns the list of saved mission IDs (most-recent first) for the authenticated freelancer. */
    public List<String> getSavedMissionIds(String email) {
        Freelancer freelancer = getFreelancerByEmail(email);
        return freelancer.getSavedMissionIds() != null
                ? freelancer.getSavedMissionIds()
                : new ArrayList<>();
    }

    /**
     * Toggle save/unsave for a given mission.
     * New saves are inserted at position 0 so the list stays most-recent first.
     * Returns the updated list of saved IDs.
     */
    public List<String> toggleSavedMission(String email, String missionId) {
        Freelancer freelancer = getFreelancerByEmail(email);
        if (freelancer.getSavedMissionIds() == null) {
            freelancer.setSavedMissionIds(new ArrayList<>());
        }
        List<String> ids = freelancer.getSavedMissionIds();
        if (ids.contains(missionId)) {
            ids.remove(missionId);
        } else {
            ids.add(0, missionId); // most recent first
        }
        freelancerRepository.save(freelancer);
        return ids;
    }

    /** Returns full MissionResponse objects for all saved missions (most-recent first). */
    public List<MissionResponse> getSavedMissions(String email) {
        List<String> ids = getSavedMissionIds(email);
        if (ids.isEmpty()) return List.of();
        return ids.stream()
                .map(id -> missionRepository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(m -> {
                    Company company = companyRepository.findById(m.getCompanyId()).orElse(null);
                    return MissionResponse.from(m, company);
                })
                .collect(Collectors.toList());
    }
}
