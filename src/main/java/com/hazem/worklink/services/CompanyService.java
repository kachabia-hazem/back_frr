package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.UpdateCompanyRequest;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public Company getCompanyById(String id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Company getCompanyByEmail(String email) {
        return companyRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with email: " + email));
    }

    public Company updateCompany(String email, UpdateCompanyRequest request) {
        Company company = getCompanyByEmail(email);

        if (request.getCompanyName() != null) {
            company.setCompanyName(request.getCompanyName());
        }
        if (request.getAddress() != null) {
            company.setAddress(request.getAddress());
        }
        if (request.getWebsiteUrl() != null) {
            company.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getLegalForm() != null) {
            company.setLegalForm(request.getLegalForm());
        }
        if (request.getTradeRegister() != null) {
            company.setTradeRegister(request.getTradeRegister());
        }
        if (request.getFoundationDate() != null) {
            company.setFoundationDate(request.getFoundationDate());
        }
        if (request.getBusinessSector() != null) {
            company.setBusinessSector(request.getBusinessSector());
        }
        if (request.getManagerName() != null) {
            company.setManagerName(request.getManagerName());
        }
        if (request.getManagerEmail() != null) {
            company.setManagerEmail(request.getManagerEmail());
        }
        if (request.getManagerPosition() != null) {
            company.setManagerPosition(request.getManagerPosition());
        }
        if (request.getManagerPhoneNumber() != null) {
            company.setManagerPhoneNumber(request.getManagerPhoneNumber());
        }
        if (request.getDescription() != null) {
            company.setDescription(request.getDescription());
        }
        if (request.getNumberOfEmployees() != null) {
            company.setNumberOfEmployees(request.getNumberOfEmployees());
        }

        return companyRepository.save(company);
    }

    public Company updateCompanyLogo(String email, String logoUrl) {
        Company company = getCompanyByEmail(email);
        company.setCompanyLogo(logoUrl);
        return companyRepository.save(company);
    }
}
