package com.hazem.worklink.config;

import com.hazem.worklink.models.PointPack;
import com.hazem.worklink.models.SubscriptionPlan;
import com.hazem.worklink.repositories.PointPackRepository;
import com.hazem.worklink.repositories.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PointPackSeeder implements ApplicationRunner {

    private final PointPackRepository packRepository;
    private final SubscriptionPlanRepository subscriptionRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (packRepository.count() == 0) {
            seedPacks();
            log.info("=== Point packs seeded ===");
        }
        if (subscriptionRepository.count() == 0) {
            seedSubscriptions();
            log.info("=== Subscription plans seeded ===");
        }
    }

    private void seedPacks() {
        List<PointPack> packs = Arrays.asList(
            pack("20 points",   "DECOUVERTE", 20,   10.0,  null,               1),
            pack("50 points",   "DECOUVERTE", 50,   22.0,  null,               2),
            pack("100 points",  "POPULAIRE",  100,  39.0,  "Le plus vendu",    3),
            pack("200 points",  "POPULAIRE",  200,  70.0,  null,               4),
            pack("500 points",  "PRO",        500,  155.0, null,               5),
            pack("1000 points", "PRO",        1000, 280.0, "Meilleure valeur", 6)
        );
        packRepository.saveAll(packs);
    }

    private PointPack pack(String name, String category, int points, double price, String badge, int order) {
        PointPack p = new PointPack();
        p.setName(name);
        p.setCategory(category);
        p.setPoints(points);
        p.setPrice(price);
        p.setBadge(badge);
        p.setIsActive(true);
        p.setDisplayOrder(order);
        p.setPromoEnabled(false);
        p.setPromoDiscountPercent(0);
        p.setPromoLabel("");
        return p;
    }

    private void seedSubscriptions() {
        SubscriptionPlan standard = new SubscriptionPlan();
        standard.setName("Standard");
        standard.setPricePerMonth(32.0);
        standard.setPointsPerMonth(100);
        standard.setAdvantages(Arrays.asList(
            "100 points offerts/mois",
            "-10% sur tous les packs",
            "Support standard"
        ));
        standard.setIsActive(true);
        standard.setDisplayOrder(1);
        standard.setPromoEnabled(false);
        standard.setPromoDiscountPercent(0);
        standard.setPromoLabel("");

        SubscriptionPlan premium = new SubscriptionPlan();
        premium.setName("Premium");
        premium.setPricePerMonth(65.0);
        premium.setPointsPerMonth(250);
        premium.setAdvantages(Arrays.asList(
            "250 points offerts/mois",
            "-20% sur tous les packs",
            "Support prioritaire",
            "Accès aux statistiques avancées"
        ));
        premium.setIsActive(true);
        premium.setDisplayOrder(2);
        premium.setPromoEnabled(false);
        premium.setPromoDiscountPercent(0);
        premium.setPromoLabel("");

        SubscriptionPlan business = new SubscriptionPlan();
        business.setName("Business");
        business.setPricePerMonth(159.0);
        business.setPointsPerMonth(700);
        business.setAdvantages(Arrays.asList(
            "700 points offerts/mois",
            "Boosts gratuits inclus",
            "Support dédié 24/7",
            "Accès anticipé aux nouvelles fonctionnalités",
            "Tableau de bord analytics premium"
        ));
        business.setIsActive(true);
        business.setDisplayOrder(3);
        business.setPromoEnabled(false);
        business.setPromoDiscountPercent(0);
        business.setPromoLabel("");

        subscriptionRepository.saveAll(Arrays.asList(standard, premium, business));
    }
}
