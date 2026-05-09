package com.hazem.worklink.services;

import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BanExpirationScheduler {

    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;

    @Scheduled(fixedRate = 3_600_000) // every hour
    public void autoUnbanExpiredUsers() {
        LocalDateTime now = LocalDateTime.now();

        List<Freelancer> bannedFreelancers = freelancerRepository.findAll().stream()
                .filter(f -> !Boolean.TRUE.equals(f.getIsActive()))
                .filter(f -> isBanExpired(f.getBanStartDate(), f.getBanDuration(), now))
                .toList();

        for (Freelancer f : bannedFreelancers) {
            f.setIsActive(true);
            f.setBanReason(null);
            f.setBanDuration(null);
            f.setBanStartDate(null);
            f.setUpdatedAt(now);
            freelancerRepository.save(f);
            log.info("Auto-unbanned freelancer: {} ({})", f.getEmail(), f.getId());
        }

        List<Company> bannedCompanies = companyRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsActive()))
                .filter(c -> isBanExpired(c.getBanStartDate(), c.getBanDuration(), now))
                .toList();

        for (Company c : bannedCompanies) {
            c.setIsActive(true);
            c.setBanReason(null);
            c.setBanDuration(null);
            c.setBanStartDate(null);
            c.setUpdatedAt(now);
            companyRepository.save(c);
            log.info("Auto-unbanned company: {} ({})", c.getEmail(), c.getId());
        }

        if (!bannedFreelancers.isEmpty() || !bannedCompanies.isEmpty()) {
            log.info("Auto-unban completed: {} freelancer(s), {} company(ies) reactivated.",
                    bannedFreelancers.size(), bannedCompanies.size());
        }
    }

    private boolean isBanExpired(LocalDateTime banStartDate, String banDuration, LocalDateTime now) {
        if (banStartDate == null || banDuration == null || banDuration.isBlank()) return false;
        if ("Permanent".equalsIgnoreCase(banDuration)) return false;
        LocalDateTime expiryDate = switch (banDuration) {
            case "1 Day"   -> banStartDate.plusDays(1);
            case "3 Days"  -> banStartDate.plusDays(3);
            case "7 Days"  -> banStartDate.plusDays(7);
            case "14 Days" -> banStartDate.plusDays(14);
            case "30 Days" -> banStartDate.plusDays(30);
            default        -> null;
        };
        return expiryDate != null && now.isAfter(expiryDate);
    }
}
