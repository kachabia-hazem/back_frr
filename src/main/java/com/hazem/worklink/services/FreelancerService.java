package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.UpdateCvDataRequest;
import com.hazem.worklink.dto.request.UpdateFreelancerRequest;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.repositories.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FreelancerService {

    private final FreelancerRepository freelancerRepository;

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
        if (request.getBio() != null) {
            freelancer.setBio(request.getBio());
        }
        if (request.getSkills() != null) {
            freelancer.setSkills(request.getSkills());
        }
        if (request.getPortfolioUrl() != null) {
            freelancer.setPortfolioUrl(request.getPortfolioUrl());
        }

        return freelancerRepository.save(freelancer);
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

        return freelancerRepository.save(freelancer);
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
}
