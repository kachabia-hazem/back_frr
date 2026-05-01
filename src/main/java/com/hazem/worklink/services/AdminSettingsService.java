package com.hazem.worklink.services;

import com.hazem.worklink.models.Admin;
import com.hazem.worklink.models.PlatformSettings;
import com.hazem.worklink.repositories.AdminRepository;
import com.hazem.worklink.repositories.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminSettingsService {

    private final PlatformSettingsRepository settingsRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public PlatformSettings getSettings() {
        return settingsRepository.findById("platform_default")
                .orElseGet(() -> settingsRepository.save(new PlatformSettings()));
    }

    public PlatformSettings updatePlatformFee(int percent) {
        if (percent < 1 || percent > 50)
            throw new IllegalArgumentException("Commission must be between 1% and 50%");

        PlatformSettings settings = getSettings();
        settings.setPlatformFeePercent(percent);
        settings.setUpdatedAt(LocalDateTime.now());
        return settingsRepository.save(settings);
    }

    public PlatformSettings updatePointCosts(int applicationCost, int aiMatchingCost, int aiRankingCost, int welcomeBonus) {
        if (applicationCost < 1 || applicationCost > 100)
            throw new IllegalArgumentException("Application cost must be between 1 and 100");
        if (aiMatchingCost < 1 || aiMatchingCost > 100)
            throw new IllegalArgumentException("AI matching cost must be between 1 and 100");
        if (aiRankingCost < 1 || aiRankingCost > 100)
            throw new IllegalArgumentException("AI ranking cost must be between 1 and 100");
        if (welcomeBonus < 0 || welcomeBonus > 500)
            throw new IllegalArgumentException("Welcome bonus must be between 0 and 500");

        PlatformSettings settings = getSettings();
        settings.setApplicationCost(applicationCost);
        settings.setAiMatchingCost(aiMatchingCost);
        settings.setAiRankingCost(aiRankingCost);
        settings.setWelcomeBonus(welcomeBonus);
        settings.setUpdatedAt(LocalDateTime.now());
        return settingsRepository.save(settings);
    }

    public void changeAdminPassword(String email, String currentPassword, String newPassword) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!passwordEncoder.matches(currentPassword, admin.getPassword()))
            throw new IllegalArgumentException("Current password is incorrect");

        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);
    }
}
