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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final FreelancerRepository freelancerRepository;
    private final MissionRepository missionRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;
    private final ContractService contractService;

    public Application submitApplication(String freelancerEmail, CreateApplicationRequest request) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + freelancerEmail));

        Mission mission = missionRepository.findById(request.getMissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + request.getMissionId()));

        // Check if freelancer already applied to this mission
        applicationRepository.findByFreelancerIdAndMissionId(freelancer.getId(), mission.getId())
                .ifPresent(existing -> {
                    // If the previous application was withdrawn (old data), delete it to allow re-applying
                    if (existing.getStatus() == ApplicationStatus.WITHDRAWN) {
                        applicationRepository.delete(existing);
                    } else {
                        throw new RuntimeException("You have already applied to this mission");
                    }
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
        application.setPreviousWorkDate(request.getPreviousWorkDate());
        application.setPreviousWorkExperience(request.getPreviousWorkExperience());
        application.setStatus(ApplicationStatus.PENDING);
        application.setSubmittedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());

        Application savedApplication = applicationRepository.save(application);

        // Notify freelancer: application submitted
        Company company = companyRepository.findById(mission.getCompanyId()).orElse(null);
        String companyName = company != null ? company.getCompanyName() : "the company";
        notificationService.sendApplicationSubmittedNotification(
                freelancer.getId(), mission.getJobTitle(), companyName);

        // Notify company: new application received
        if (company != null) {
            String freelancerName = (freelancer.getFirstName() != null ? freelancer.getFirstName() : "")
                    + " " + (freelancer.getLastName() != null ? freelancer.getLastName() : "");
            notificationService.sendApplicationReceivedNotification(
                    company.getId(), mission.getJobTitle(), freelancerName.trim());
        }

        return savedApplication;
    }

    public boolean hasApplied(String freelancerEmail, String missionId) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + freelancerEmail));
        return applicationRepository.findByFreelancerIdAndMissionId(freelancer.getId(), missionId)
                .filter(app -> app.getStatus() != ApplicationStatus.WITHDRAWN)
                .isPresent();
    }

    public void withdrawApplication(String freelancerEmail, String missionId) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + freelancerEmail));

        Application application = applicationRepository.findByFreelancerIdAndMissionId(freelancer.getId(), missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found for this mission"));

        if (application.getStatus() == ApplicationStatus.ACCEPTED) {
            throw new RuntimeException("Cannot withdraw an accepted application");
        }

        // Fetch mission title for notification before deleting
        Mission mission = missionRepository.findById(application.getMissionId()).orElse(null);
        String missionTitle = mission != null ? mission.getJobTitle() : "the mission";

        // Delete the application completely so it disappears from company history
        // and the freelancer can re-apply later
        applicationRepository.delete(application);

        // Notify freelancer: application withdrawn
        notificationService.sendApplicationWithdrawnNotification(freelancer.getId(), missionTitle);
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

    public List<ApplicationResponse> getCompanyApplications(String companyEmail) {
        Company company = companyRepository.findByEmail(companyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with email: " + companyEmail));

        List<Mission> missions = missionRepository.findByCompanyId(company.getId());

        List<Application> applications = missions.stream()
                .flatMap(mission -> applicationRepository.findByMissionId(mission.getId()).stream())
                .filter(app -> app.getStatus() != ApplicationStatus.WITHDRAWN)
                .collect(Collectors.toList());

        Map<String, Mission> missionMap = missions.stream()
                .collect(Collectors.toMap(Mission::getId, m -> m));

        List<String> freelancerIds = applications.stream()
                .map(Application::getFreelancerId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Freelancer> freelancerMap = freelancerIds.isEmpty()
                ? Map.of()
                : freelancerRepository.findAllById(freelancerIds).stream()
                    .collect(Collectors.toMap(Freelancer::getId, f -> f));

        return applications.stream()
                .map(app -> ApplicationResponse.fromWithFreelancer(
                        app, missionMap.get(app.getMissionId()), company, freelancerMap.get(app.getFreelancerId())))
                .collect(Collectors.toList());
    }

    public List<ApplicationResponse> getMissionApplications(String companyEmail, String missionId) {
        Company company = companyRepository.findByEmail(companyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with email: " + companyEmail));

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));

        if (!mission.getCompanyId().equals(company.getId())) {
            throw new RuntimeException("Unauthorized: mission does not belong to this company");
        }

        List<Application> applications = applicationRepository.findByMissionId(missionId)
                .stream()
                .filter(app -> app.getStatus() != ApplicationStatus.WITHDRAWN)
                .collect(Collectors.toList());

        List<String> freelancerIds = applications.stream()
                .map(Application::getFreelancerId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Freelancer> freelancerMap = freelancerIds.isEmpty()
                ? Map.of()
                : freelancerRepository.findAllById(freelancerIds).stream()
                    .collect(Collectors.toMap(Freelancer::getId, f -> f));

        return applications.stream()
                .map(app -> ApplicationResponse.fromWithFreelancer(
                        app, mission, company, freelancerMap.get(app.getFreelancerId())))
                .collect(Collectors.toList());
    }

    public ApplicationResponse updateApplicationStatus(String companyEmail, String applicationId, String status) {
        Company company = companyRepository.findByEmail(companyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with email: " + companyEmail));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        Mission mission = missionRepository.findById(application.getMissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));

        if (!mission.getCompanyId().equals(company.getId())) {
            throw new RuntimeException("Unauthorized: application does not belong to this company");
        }

        com.hazem.worklink.models.enums.ApplicationStatus newStatus =
                com.hazem.worklink.models.enums.ApplicationStatus.valueOf(status.toUpperCase());
        application.setStatus(newStatus);
        application.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(application);

        // Notify freelancer about status change
        if (newStatus == ApplicationStatus.ACCEPTED) {
            notificationService.sendApplicationAcceptedNotification(
                    application.getFreelancerId(), mission.getJobTitle(), company.getCompanyName(), company.getId());

            // Auto-generate contract
            Freelancer freelancer = freelancerRepository.findById(application.getFreelancerId()).orElse(null);
            if (freelancer != null) {
                try {
                    contractService.generateContract(mission, freelancer, company);
                } catch (Exception e) {
                    log.error("Failed to generate contract for application {}: {}", applicationId, e.getMessage());
                }
            }
        } else if (newStatus == ApplicationStatus.REJECTED) {
            notificationService.sendApplicationRejectedNotification(
                    application.getFreelancerId(), mission.getJobTitle(), company.getCompanyName(), company.getId());
        }

        return ApplicationResponse.from(application, mission, company);
    }
}
