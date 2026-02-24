package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.CreateMissionRequest;
import com.hazem.worklink.dto.response.MissionResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.enums.MissionStatus;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final CompanyRepository companyRepository;
    private final FreelancerRepository freelancerRepository;
    private final NotificationService notificationService;

    public Mission createMission(String companyEmail, CreateMissionRequest request) {
        Company company = companyRepository.findByEmail(companyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with email: " + companyEmail));

        Mission mission = new Mission();
        mission.setCompanyId(company.getId());
        mission.setJobTitle(request.getJobTitle());
        mission.setField(request.getField());
        mission.setLocation(request.getLocation());
        mission.setMissionType(request.getMissionType());
        mission.setYearsOfExperience(request.getYearsOfExperience());
        mission.setStartDate(request.getStartDate());
        mission.setEndDate(request.getEndDate());
        mission.setDescription(request.getDescription());
        mission.setRequiredSkills(request.getRequiredSkills());
        mission.setTechnicalEnvironment(request.getTechnicalEnvironment());
        mission.setApplicationDeadline(request.getApplicationDeadline());
        mission.setMissionBusinessSector(request.getMissionBusinessSector());
        mission.setSpeciality(request.getSpeciality());
        mission.setTjm(request.getTjm());
        mission.setStatus(MissionStatus.OPEN);
        mission.setCreatedAt(LocalDateTime.now());
        mission.setUpdatedAt(LocalDateTime.now());

        Mission savedMission = missionRepository.save(mission);

        // Notify matching freelancers
        notifyMatchingFreelancers(savedMission, company);

        return savedMission;
    }

    /**
     * Principle:
     * 1. Get required skills from the published mission (strip HTML → plain text → Set)
     * 2. Compare with each freelancer's skills
     * 3. Send notification to every compatible freelancer (at least 1 skill in common)
     */
    private void notifyMatchingFreelancers(Mission mission, Company company) {

        // ── Step 1: extract skill set from mission ──
        log.info("[NOTIF-MATCH] Mission '{}' - raw requiredSkills: {}", mission.getJobTitle(), mission.getRequiredSkills());

        if (mission.getRequiredSkills() == null || mission.getRequiredSkills().isBlank()) {
            log.info("[NOTIF-MATCH] No requiredSkills found, skipping notifications.");
            return;
        }

        String plain = mission.getRequiredSkills()
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&[a-zA-Z0-9#]+;", " ")
                .replaceAll("\\s+", " ")
                .trim();

        log.info("[NOTIF-MATCH] Plain text after HTML strip: '{}'", plain);

        if (plain.isBlank()) return;

        Set<String> missionSkills = Arrays.stream(plain.split("[,;/•\\n\\r]+"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> s.length() > 1)
                .collect(Collectors.toSet());

        log.info("[NOTIF-MATCH] Extracted mission skills: {}", missionSkills);

        if (missionSkills.isEmpty()) return;

        String companyName = (company.getCompanyName() != null && !company.getCompanyName().isBlank())
                ? company.getCompanyName() : "A company";

        // ── Step 2 & 3: compare with freelancer skills → notify compatible ones ──
        List<com.hazem.worklink.models.Freelancer> allFreelancers = freelancerRepository.findAll();
        log.info("[NOTIF-MATCH] Total freelancers in DB: {}", allFreelancers.size());

        allFreelancers.forEach(freelancer -> {
            log.info("[NOTIF-MATCH] Checking freelancer '{}' with skills: {}",
                    freelancer.getFirstName() + " " + freelancer.getLastName(), freelancer.getSkills());

            if (freelancer.getSkills() == null || freelancer.getSkills().isEmpty()) {
                log.info("[NOTIF-MATCH] -> No skills, skipping.");
                return;
            }

            boolean compatible = freelancer.getSkills().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toLowerCase())
                    .anyMatch(missionSkills::contains);

            log.info("[NOTIF-MATCH] -> Compatible: {}", compatible);

            if (compatible) {
                log.info("[NOTIF-MATCH] -> Sending notification to freelancer '{}'",
                        freelancer.getFirstName() + " " + freelancer.getLastName());
                notificationService.sendNewMissionMatchNotification(
                        freelancer.getId(), mission.getJobTitle(), companyName, mission.getId());
            }
        });
    }

    public List<Mission> getMissionsByCompany(String companyEmail) {
        Company company = companyRepository.findByEmail(companyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with email: " + companyEmail));
        return missionRepository.findByCompanyId(company.getId());
    }

    public List<Mission> getAllMissions() {
        return missionRepository.findAll();
    }

    public List<Mission> getOpenMissions() {
        return missionRepository.findByStatus(MissionStatus.OPEN);
    }

    public List<MissionResponse> getOpenMissionsWithCompany() {
        List<Mission> missions = new java.util.ArrayList<>(missionRepository.findByStatus(MissionStatus.OPEN));

        // Include CLOSED missions whose deadline passed less than 1 hour ago
        LocalDate oneHourAgoDate = LocalDateTime.now().minusHours(1).toLocalDate();
        List<Mission> closedMissions = missionRepository.findByStatus(MissionStatus.CLOSED);
        for (Mission m : closedMissions) {
            if (m.getApplicationDeadline() != null && !m.getApplicationDeadline().isBefore(oneHourAgoDate)) {
                missions.add(m);
            }
        }

        return enrichMissionsWithCompany(missions);
    }

    public MissionResponse getMissionByIdWithCompany(String id) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + id));
        Company company = companyRepository.findById(mission.getCompanyId()).orElse(null);
        return MissionResponse.from(mission, company);
    }

    public Mission getMissionById(String id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + id));
    }

    public Mission updateMission(String companyEmail, String missionId, CreateMissionRequest request) {
        Company company = companyRepository.findByEmail(companyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with email: " + companyEmail));
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));
        if (!mission.getCompanyId().equals(company.getId())) {
            throw new RuntimeException("You are not authorized to update this mission");
        }
        mission.setJobTitle(request.getJobTitle());
        mission.setField(request.getField());
        mission.setLocation(request.getLocation());
        mission.setMissionType(request.getMissionType());
        mission.setYearsOfExperience(request.getYearsOfExperience());
        mission.setStartDate(request.getStartDate());
        mission.setEndDate(request.getEndDate());
        mission.setDescription(request.getDescription());
        mission.setRequiredSkills(request.getRequiredSkills());
        mission.setTechnicalEnvironment(request.getTechnicalEnvironment());
        mission.setApplicationDeadline(request.getApplicationDeadline());
        mission.setMissionBusinessSector(request.getMissionBusinessSector());
        mission.setSpeciality(request.getSpeciality());
        mission.setTjm(request.getTjm());
        mission.setUpdatedAt(LocalDateTime.now());
        return missionRepository.save(mission);
    }

    public void deleteMission(String companyEmail, String missionId) {
        Company company = companyRepository.findByEmail(companyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with email: " + companyEmail));
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));
        if (!mission.getCompanyId().equals(company.getId())) {
            throw new RuntimeException("You are not authorized to delete this mission");
        }
        missionRepository.delete(mission);
    }

    private List<MissionResponse> enrichMissionsWithCompany(List<Mission> missions) {
        List<String> companyIds = missions.stream()
                .map(Mission::getCompanyId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Company> companyMap = companyIds.isEmpty()
                ? Map.of()
                : companyRepository.findAllById(companyIds).stream()
                    .collect(Collectors.toMap(Company::getId, c -> c));

        return missions.stream()
                .map(m -> MissionResponse.from(m, m.getCompanyId() != null ? companyMap.get(m.getCompanyId()) : null))
                .collect(Collectors.toList());
    }
}
