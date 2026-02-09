package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.*;
import com.hazem.worklink.dto.response.AuthResponse;
import com.hazem.worklink.exceptions.EmailAlreadyExistsException;
import com.hazem.worklink.models.*;
import com.hazem.worklink.models.enums.Role;
import com.hazem.worklink.repositories.*;
import com.hazem.worklink.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // Register Freelancer
    public AuthResponse registerFreelancer(RegisterFreelancerRequest request) {

        // Vérifier si l'email existe déjà
        if (checkEmailExists(request.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé");
        }

        // Créer le freelancer
        Freelancer freelancer = new Freelancer();
        freelancer.setFirstName(request.getFirstName());
        freelancer.setLastName(request.getLastName());
        freelancer.setEmail(request.getEmail());
        freelancer.setPassword(passwordEncoder.encode(request.getPassword()));
        freelancer.setGender(request.getGender());
        freelancer.setDateOfBirth(request.getDateOfBirth());
        freelancer.setPhoneNumber(request.getPhoneNumber());
        freelancer.setCurrentPosition(request.getCurrentPosition());
        freelancer.setBio(request.getBio());
        freelancer.setSkills(request.getSkills());
        freelancer.setPortfolioUrl(request.getPortfolioUrl());
        freelancer.setRole(Role.FREELANCER);
        freelancer.setIsActive(true);
        freelancer.setCreatedAt(LocalDateTime.now());
        freelancer.setUpdatedAt(LocalDateTime.now());
        freelancer.setCompletedProjects(0);
        freelancer.setRating(0.0);

        Freelancer savedFreelancer = freelancerRepository.save(freelancer);

        // Générer le token
        String token = jwtUtil.generateToken(
                savedFreelancer.getEmail(),
                savedFreelancer.getRole(),
                savedFreelancer.getId()
        );

        return new AuthResponse(
                token,
                savedFreelancer.getEmail(),
                savedFreelancer.getRole(),
                savedFreelancer.getId(),
                "Freelancer enregistré avec succès"
        );
    }

    // Register Company
    public AuthResponse registerCompany(RegisterCompanyRequest request) {

        // Vérifier si l'email existe déjà
        if (checkEmailExists(request.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé");
        }

        // Créer la company
        Company company = new Company();
        company.setCompanyName(request.getCompanyName());
        company.setEmail(request.getEmail());
        company.setPassword(passwordEncoder.encode(request.getPassword()));
        company.setAddress(request.getAddress());
        company.setWebsiteUrl(request.getWebsiteUrl());
        company.setLegalForm(request.getLegalForm());
        company.setTradeRegister(request.getTradeRegister());
        company.setFoundationDate(request.getFoundationDate());
        company.setBusinessSector(request.getBusinessSector());
        company.setManagerName(request.getManagerName());
        company.setManagerEmail(request.getManagerEmail());
        company.setManagerPosition(request.getManagerPosition());
        company.setManagerPhoneNumber(request.getManagerPhoneNumber());
        company.setDescription(request.getDescription());
        company.setNumberOfEmployees(request.getNumberOfEmployees());
        company.setRole(Role.COMPANY);
        company.setIsActive(true);
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        company.setPostedProjects(0);


        Company savedCompany = companyRepository.save(company);

        // Générer le token
        String token = jwtUtil.generateToken(
                savedCompany.getEmail(),
                savedCompany.getRole(),
                savedCompany.getId()
        );

        return new AuthResponse(
                token,
                savedCompany.getEmail(),
                savedCompany.getRole(),
                savedCompany.getId(),
                "Company enregistrée avec succès"
        );
    }

    // Register Admin
    public AuthResponse registerAdmin(RegisterAdminRequest request) {

        // Vérifier si l'email existe déjà
        if (checkEmailExists(request.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé");
        }

        // Créer l'admin
        Admin admin = new Admin();
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setGender(request.getGender());
        admin.setDateOfBirth(request.getDateOfBirth());
        admin.setPhoneNumber(request.getPhoneNumber());
        admin.setCurrentPosition(request.getCurrentPosition());
        admin.setYearsOfExperience(request.getYearsOfExperience());
        admin.setDepartment(request.getDepartment());
        admin.setRole(Role.ADMIN);
        admin.setIsActive(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setIsSuperAdmin(false);

        Admin savedAdmin = adminRepository.save(admin);

        // Générer le token
        String token = jwtUtil.generateToken(
                savedAdmin.getEmail(),
                savedAdmin.getRole(),
                savedAdmin.getId()
        );

        return new AuthResponse(
                token,
                savedAdmin.getEmail(),
                savedAdmin.getRole(),
                savedAdmin.getId(),
                "Admin enregistré avec succès"
        );
    }

    // Login
    public AuthResponse login(LoginRequest request) {

        // Authentifier
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Chercher l'utilisateur
        String email = request.getEmail();
        String userId = null;
        Role role = null;

        // Chercher dans Freelancer
        var freelancer = freelancerRepository.findByEmail(email);
        if (freelancer.isPresent()) {
            userId = freelancer.get().getId();
            role = freelancer.get().getRole();
        }

        // Chercher dans Company
        if (userId == null) {
            var company = companyRepository.findByEmail(email);
            if (company.isPresent()) {
                userId = company.get().getId();
                role = company.get().getRole();
            }
        }

        // Chercher dans Admin
        if (userId == null) {
            var admin = adminRepository.findByEmail(email);
            if (admin.isPresent()) {
                userId = admin.get().getId();
                role = admin.get().getRole();
            }
        }

        if (userId == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        // Générer le token
        String token = jwtUtil.generateToken(email, role, userId);

        return new AuthResponse(
                token,
                email,
                role,
                userId,
                "Connexion réussie"
        );
    }

    // Reset Password
    public void resetPassword(ResetPasswordRequest request) {
        // Verify the code
        boolean verified = emailService.verifyCode(request.getEmail(), request.getCode());
        if (!verified) {
            throw new RuntimeException("Code de vérification invalide ou expiré");
        }

        String email = request.getEmail();
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        // Search in Freelancer
        var freelancer = freelancerRepository.findByEmail(email);
        if (freelancer.isPresent()) {
            freelancer.get().setPassword(encodedPassword);
            freelancerRepository.save(freelancer.get());
            emailService.deleteCode(email);
            return;
        }

        // Search in Company
        var company = companyRepository.findByEmail(email);
        if (company.isPresent()) {
            company.get().setPassword(encodedPassword);
            companyRepository.save(company.get());
            emailService.deleteCode(email);
            return;
        }

        // Search in Admin
        var admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) {
            admin.get().setPassword(encodedPassword);
            adminRepository.save(admin.get());
            emailService.deleteCode(email);
            return;
        }

        throw new RuntimeException("Aucun compte trouvé avec cet email");
    }

    // Vérifier si l'email existe
    private boolean checkEmailExists(String email) {
        return freelancerRepository.existsByEmail(email) ||
                companyRepository.existsByEmail(email) ||
                adminRepository.existsByEmail(email);
    }
}