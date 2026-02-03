package com.hazem.worklink.security;

import com.hazem.worklink.models.Admin;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.repositories.AdminRepository;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Chercher dans Freelancer
        Optional<Freelancer> freelancer = freelancerRepository.findByEmail(email);
        if (freelancer.isPresent()) {
            Freelancer f = freelancer.get();
            return User.builder()
                    .username(f.getEmail())
                    .password(f.getPassword())
                    .authorities(Collections.singletonList(
                            new SimpleGrantedAuthority(f.getRole().name())))
                    .build();
        }

        // Chercher dans Company
        Optional<Company> company = companyRepository.findByEmail(email);
        if (company.isPresent()) {
            Company c = company.get();
            return User.builder()
                    .username(c.getEmail())
                    .password(c.getPassword())
                    .authorities(Collections.singletonList(
                            new SimpleGrantedAuthority(c.getRole().name())))
                    .build();
        }

        // Chercher dans Admin
        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) {
            Admin a = admin.get();
            return User.builder()
                    .username(a.getEmail())
                    .password(a.getPassword())
                    .authorities(Collections.singletonList(
                            new SimpleGrantedAuthority(a.getRole().name())))
                    .build();
        }

        throw new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email);
    }
}
