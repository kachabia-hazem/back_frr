package com.hazem.worklink.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hazem.worklink.dto.request.OAuthCompleteRequest;
import com.hazem.worklink.dto.response.AuthResponse;
import com.hazem.worklink.dto.response.LinkedInProfileResponse;
import com.hazem.worklink.dto.response.LinkedInTokenResponse;
import com.hazem.worklink.dto.response.OAuthProfileResponse;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.enums.AuthProvider;
import com.hazem.worklink.models.enums.Role;
import com.hazem.worklink.repositories.AdminRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import com.hazem.worklink.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.linkedin.client-id}")
    private String linkedInClientId;

    @Value("${oauth.linkedin.client-secret}")
    private String linkedInClientSecret;

    @Value("${oauth.linkedin.redirect-uri}")
    private String linkedInRedirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    // ══════════════════════════════════════════════
    //  Google OAuth - Login + Registration
    // ══════════════════════════════════════════════

    public AuthResponse googleLogin(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Token Google invalide");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");
            String picture = (String) payload.get("picture");

            // Check if the user is an admin → block OAuth
            if (adminRepository.existsByEmail(email)) {
                throw new RuntimeException("Les administrateurs ne peuvent pas se connecter via OAuth");
            }

            // Look up existing user by email
            var freelancer = freelancerRepository.findByEmail(email);
            if (freelancer.isPresent()) {
                Freelancer f = freelancer.get();
                // Update provider info if first OAuth login
                if (f.getAuthProvider() == AuthProvider.LOCAL) {
                    f.setAuthProvider(AuthProvider.GOOGLE);
                    f.setProviderId(googleId);
                    f.setUpdatedAt(LocalDateTime.now());
                    freelancerRepository.save(f);
                }
                String token = jwtUtil.generateToken(f.getEmail(), f.getRole(), f.getId());
                return new AuthResponse(token, f.getEmail(), f.getRole(), f.getId(), "Connexion Google réussie");
            }

            var company = companyRepository.findByEmail(email);
            if (company.isPresent()) {
                Company c = company.get();
                if (c.getAuthProvider() == AuthProvider.LOCAL) {
                    c.setAuthProvider(AuthProvider.GOOGLE);
                    c.setProviderId(googleId);
                    c.setUpdatedAt(LocalDateTime.now());
                    companyRepository.save(c);
                }
                String token = jwtUtil.generateToken(c.getEmail(), c.getRole(), c.getId());
                return new AuthResponse(token, c.getEmail(), c.getRole(), c.getId(), "Connexion Google réussie");
            }

            // New user → needs registration (select role)
            OAuthProfileResponse profile = OAuthProfileResponse.builder()
                    .providerId(googleId)
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .picture(picture)
                    .provider(AuthProvider.GOOGLE)
                    .build();

            AuthResponse response = new AuthResponse();
            response.setNeedsRegistration(true);
            response.setOauthProfile(profile);
            response.setMessage("Veuillez sélectionner votre rôle pour continuer.");
            return response;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du token Google", e);
            throw new RuntimeException("Erreur lors de la connexion avec Google");
        }
    }

    // ══════════════════════════════════════════════
    //  LinkedIn OAuth - Login + Registration
    // ══════════════════════════════════════════════

    public AuthResponse linkedInLogin(String code) {
        try {
            // 1. Exchange code for access token
            LinkedInTokenResponse tokenResponse = exchangeLinkedInCode(code);

            // 2. Fetch profile with access token
            LinkedInProfileResponse profile = fetchLinkedInProfile(tokenResponse.getAccessToken());

            String email = profile.getEmail();
            String linkedInId = profile.getSub();

            // Check if the user is an admin → block OAuth
            if (adminRepository.existsByEmail(email)) {
                throw new RuntimeException("Les administrateurs ne peuvent pas se connecter via OAuth");
            }

            // 3. Look up existing user
            var freelancer = freelancerRepository.findByEmail(email);
            if (freelancer.isPresent()) {
                Freelancer f = freelancer.get();
                if (f.getAuthProvider() == AuthProvider.LOCAL) {
                    f.setAuthProvider(AuthProvider.LINKEDIN);
                    f.setProviderId(linkedInId);
                    f.setUpdatedAt(LocalDateTime.now());
                    freelancerRepository.save(f);
                }
                String token = jwtUtil.generateToken(f.getEmail(), f.getRole(), f.getId());
                return new AuthResponse(token, f.getEmail(), f.getRole(), f.getId(), "Connexion LinkedIn réussie");
            }

            var company = companyRepository.findByEmail(email);
            if (company.isPresent()) {
                Company c = company.get();
                if (c.getAuthProvider() == AuthProvider.LOCAL) {
                    c.setAuthProvider(AuthProvider.LINKEDIN);
                    c.setProviderId(linkedInId);
                    c.setUpdatedAt(LocalDateTime.now());
                    companyRepository.save(c);
                }
                String token = jwtUtil.generateToken(c.getEmail(), c.getRole(), c.getId());
                return new AuthResponse(token, c.getEmail(), c.getRole(), c.getId(), "Connexion LinkedIn réussie");
            }

            // 4. New user → needs registration (select role)
            OAuthProfileResponse oauthProfile = OAuthProfileResponse.builder()
                    .providerId(linkedInId)
                    .email(email)
                    .firstName(profile.getGivenName())
                    .lastName(profile.getFamilyName())
                    .picture(profile.getPicture())
                    .provider(AuthProvider.LINKEDIN)
                    .build();

            AuthResponse response = new AuthResponse();
            response.setNeedsRegistration(true);
            response.setOauthProfile(oauthProfile);
            response.setMessage("Veuillez sélectionner votre rôle pour continuer.");
            return response;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la connexion LinkedIn", e);
            throw new RuntimeException("Erreur lors de la connexion avec LinkedIn");
        }
    }

    // ══════════════════════════════════════════════
    //  Generic OAuth Complete Registration (simple)
    // ══════════════════════════════════════════════

    public AuthResponse oauthCompleteRegistration(OAuthCompleteRequest request) {
        // Verify email not already taken
        if (freelancerRepository.existsByEmail(request.getEmail()) ||
                companyRepository.existsByEmail(request.getEmail()) ||
                adminRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Block admin registration via OAuth
        if (request.getRole() == Role.ADMIN) {
            throw new RuntimeException("L'inscription Admin via OAuth n'est pas autorisée");
        }

        // Generate random password (user won't use it, they login via OAuth)
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        if (request.getRole() == Role.FREELANCER) {
            return createFreelancerFromOAuth(request, randomPassword);
        } else if (request.getRole() == Role.COMPANY) {
            return createCompanyFromOAuth(request, randomPassword);
        }

        throw new RuntimeException("Rôle invalide");
    }

    private AuthResponse createFreelancerFromOAuth(OAuthCompleteRequest request, String encodedPassword) {
        Freelancer freelancer = new Freelancer();
        freelancer.setEmail(request.getEmail());
        freelancer.setPassword(encodedPassword);
        freelancer.setRole(Role.FREELANCER);
        freelancer.setAuthProvider(request.getProvider());
        freelancer.setProviderId(request.getProviderId());
        freelancer.setIsActive(true);
        freelancer.setCreatedAt(LocalDateTime.now());
        freelancer.setUpdatedAt(LocalDateTime.now());

        // Set basic info from OAuth provider
        freelancer.setFirstName(request.getFirstName());
        freelancer.setLastName(request.getLastName());
        freelancer.setProfilePicture(request.getProfilePicture());

        // Initialize with default values
        freelancer.setCompletedProjects(0);
        freelancer.setRating(0.0);

        Freelancer saved = freelancerRepository.save(freelancer);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole(), saved.getId());
        return new AuthResponse(token, saved.getEmail(), saved.getRole(), saved.getId(),
                "Inscription Freelancer via " + request.getProvider().name() + " réussie");
    }

    private AuthResponse createCompanyFromOAuth(OAuthCompleteRequest request, String encodedPassword) {
        Company company = new Company();
        company.setEmail(request.getEmail());
        company.setPassword(encodedPassword);
        company.setRole(Role.COMPANY);
        company.setAuthProvider(request.getProvider());
        company.setProviderId(request.getProviderId());
        company.setIsActive(true);
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());

        // Set manager info from OAuth provider
        String fullName = "";
        if (request.getFirstName() != null) {
            fullName += request.getFirstName();
        }
        if (request.getLastName() != null) {
            fullName += (fullName.isEmpty() ? "" : " ") + request.getLastName();
        }
        company.setManagerName(fullName.isEmpty() ? null : fullName);
        company.setManagerEmail(request.getEmail());

        // Initialize with default values
        company.setPostedProjects(0);

        Company saved = companyRepository.save(company);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole(), saved.getId());
        return new AuthResponse(token, saved.getEmail(), saved.getRole(), saved.getId(),
                "Inscription Company via " + request.getProvider().name() + " réussie");
    }

    // ── Private helpers ──

    private LinkedInTokenResponse exchangeLinkedInCode(String code) {
        String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", linkedInRedirectUri);
        params.add("client_id", linkedInClientId);
        params.add("client_secret", linkedInClientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<LinkedInTokenResponse> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, request, LinkedInTokenResponse.class);

        if (response.getBody() == null || response.getBody().getAccessToken() == null) {
            throw new RuntimeException("Impossible d'obtenir le token LinkedIn");
        }

        return response.getBody();
    }

    private LinkedInProfileResponse fetchLinkedInProfile(String accessToken) {
        String profileUrl = "https://api.linkedin.com/v2/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<LinkedInProfileResponse> response = restTemplate.exchange(
                profileUrl, HttpMethod.GET, request, LinkedInProfileResponse.class);

        if (response.getBody() == null || response.getBody().getEmail() == null) {
            throw new RuntimeException("Impossible de récupérer le profil LinkedIn");
        }

        return response.getBody();
    }
}
