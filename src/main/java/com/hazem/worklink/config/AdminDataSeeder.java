package com.hazem.worklink.config;

import com.hazem.worklink.models.Admin;
import com.hazem.worklink.models.enums.Gender;
import com.hazem.worklink.models.enums.Role;
import com.hazem.worklink.repositories.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataSeeder implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setFirstName("Hazem");
            admin.setLastName("Admin");
            admin.setEmail("admin@worklink.com");
            admin.setPassword(passwordEncoder.encode("Admin@2026"));
            admin.setGender(Gender.MALE);
            admin.setDateOfBirth(LocalDate.of(1995, 1, 1));
            admin.setPhoneNumber("+21612345678");
            admin.setCurrentPosition("Platform Administrator");
            admin.setYearsOfExperience(5);
            admin.setDepartment("IT");
            admin.setRole(Role.ADMIN);
            admin.setIsActive(true);
            admin.setIsSuperAdmin(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());

            adminRepository.save(admin);
            log.info("=== Default admin created: admin@worklink.com / Admin@2026 ===");
        } else {
            log.info("=== Admin already exists, skipping seed ===");
        }
    }
}
