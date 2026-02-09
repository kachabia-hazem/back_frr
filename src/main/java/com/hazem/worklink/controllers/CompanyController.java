package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.UpdateCompanyRequest;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.services.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor






public class
CompanyController {

    private final CompanyService companyService;

    @GetMapping("/public/{id}")
    public ResponseEntity<Company> getCompanyById(@PathVariable String id) {
        Company company = companyService.getCompanyById(id);
        return ResponseEntity.ok(company);
    }

    @GetMapping("/public/all")
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/me")
    public ResponseEntity<Company> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        Company company = companyService.getCompanyByEmail(email);
        return ResponseEntity.ok(company);
    }

    @PutMapping("/me")
    public ResponseEntity<Company> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateCompanyRequest request) {
        String email = authentication.getName();
        Company company = companyService.updateCompany(email, request);
        return ResponseEntity.ok(company);
    }

    @PutMapping("/me/logo")
    public ResponseEntity<Company> updateCompanyLogo(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        String email = authentication.getName();
        String logoUrl = request.get("companyLogo");
        Company company = companyService.updateCompanyLogo(email, logoUrl);
        return ResponseEntity.ok(company);
    }
}
