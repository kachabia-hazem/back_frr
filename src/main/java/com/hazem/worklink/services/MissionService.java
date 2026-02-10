package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.CreateMissionRequest;
import com.hazem.worklink.dto.response.MissionResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.enums.MissionStatus;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final CompanyRepository companyRepository;

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
        mission.setTjm(request.getTjm());
        mission.setStatus(MissionStatus.OPEN);
        mission.setCreatedAt(LocalDateTime.now());
        mission.setUpdatedAt(LocalDateTime.now());

        return missionRepository.save(mission);
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
        List<Mission> missions = missionRepository.findByStatus(MissionStatus.OPEN);
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
