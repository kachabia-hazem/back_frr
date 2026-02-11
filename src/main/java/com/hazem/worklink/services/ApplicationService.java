package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.CreateApplicationRequest;
import com.hazem.worklink.dto.response.ApplicationResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Application;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.enums.ApplicationStatus;
import com.hazem.worklink.repositories.ApplicationRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.repositories.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final FreelancerRepository freelancerRepository;
    private final MissionRepository missionRepository;
    private final CompanyRepository companyRepository;

    public Application submitApplication(String freelancerEmail, CreateApplicationRequest request) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + freelancerEmail));

        Mission mission = missionRepository.findById(request.getMissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + request.getMissionId()));

        // Check if freelancer already applied to this mission
        applicationRepository.findByFreelancerIdAndMissionId(freelancer.getId(), mission.getId())
                .ifPresent(existing -> {
                    throw new RuntimeException("You have already applied to this mission");
                });

        Application application = new Application();
        application.setFreelancerId(freelancer.getId());
        application.setMissionId(mission.getId());
        application.setFirstName(request.getFirstName());
        application.setLastName(request.getLastName());
        application.setEmail(freelancer.getEmail());
        application.setPhoneNumber(request.getPhoneNumber());
        application.setCountry(request.getCountry());
        application.setPostalCode(request.getPostalCode());
        application.setCity(request.getCity());
        application.setPostalAddress(request.getPostalAddress());
        application.setCvUrl(request.getCvUrl());
        application.setSalaryExpectations(request.getSalaryExpectations());
        application.setCurrentSalaryAndNotice(request.getCurrentSalaryAndNotice());
        application.setPreviouslyWorked(request.getPreviouslyWorked());
        application.setStatus(ApplicationStatus.PENDING);
        application.setSubmittedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());

        return applicationRepository.save(application);
    }

    public List<ApplicationResponse> getMyApplications(String freelancerEmail) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + freelancerEmail));

        List<Application> applications = applicationRepository.findByFreelancerId(freelancer.getId());

        // Collect all mission IDs
        List<String> missionIds = applications.stream()
                .map(Application::getMissionId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Mission> missionMap = missionIds.isEmpty()
                ? Map.of()
                : missionRepository.findAllById(missionIds).stream()
                    .collect(Collectors.toMap(Mission::getId, m -> m));

        // Collect all company IDs from missions
        List<String> companyIds = missionMap.values().stream()
                .map(Mission::getCompanyId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Company> companyMap = companyIds.isEmpty()
                ? Map.of()
                : companyRepository.findAllById(companyIds).stream()
                    .collect(Collectors.toMap(Company::getId, c -> c));

        return applications.stream()
                .map(app -> {
                    Mission mission = missionMap.get(app.getMissionId());
                    Company company = mission != null && mission.getCompanyId() != null
                            ? companyMap.get(mission.getCompanyId())
                            : null;
                    return ApplicationResponse.from(app, mission, company);
                })
                .collect(Collectors.toList());
    }
}
