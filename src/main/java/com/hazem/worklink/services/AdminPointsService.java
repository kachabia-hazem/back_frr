package com.hazem.worklink.services;

import com.hazem.worklink.dto.request.CreatePackRequest;
import com.hazem.worklink.dto.request.CreateSubscriptionRequest;
import com.hazem.worklink.dto.request.UpdatePackRequest;
import com.hazem.worklink.dto.request.UpdatePromoRequest;
import com.hazem.worklink.dto.request.UpdateSubscriptionRequest;
import com.hazem.worklink.dto.response.PointPackResponse;
import com.hazem.worklink.dto.response.SubscriptionPlanResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.PointPack;
import com.hazem.worklink.models.SubscriptionPlan;
import com.hazem.worklink.repositories.PointPackRepository;
import com.hazem.worklink.repositories.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPointsService {

    private final PointPackRepository packRepository;
    private final SubscriptionPlanRepository subscriptionRepository;

    // ─── One-time Packs ───────────────────────────────────────────────────────

    public List<PointPackResponse> getAllPacks() {
        List<PointPack> packs = packRepository.findAllByOrderByDisplayOrderAsc();
        double maxPPP = packs.stream()
                .filter(p -> p.getPoints() > 0)
                .mapToDouble(p -> p.getPrice() / p.getPoints())
                .max()
                .orElse(1.0);
        return packs.stream().map(p -> toPackResponse(p, maxPPP)).collect(Collectors.toList());
    }

    public PointPackResponse createPack(CreatePackRequest req) {
        PointPack p = new PointPack();
        p.setName(req.getName());
        p.setCategory(req.getCategory());
        p.setPoints(req.getPoints());
        p.setPrice(req.getPrice());
        p.setBadge(req.getBadge());
        p.setIsActive(req.isActive());
        p.setDisplayOrder(req.getDisplayOrder());
        p.setPromoEnabled(false);
        p.setPromoDiscountPercent(0);
        p.setPromoLabel("");
        packRepository.save(p);
        List<PointPack> all = packRepository.findAllByOrderByDisplayOrderAsc();
        double maxPPP = all.stream().filter(x -> x.getPoints() > 0)
                .mapToDouble(x -> x.getPrice() / x.getPoints()).max().orElse(1.0);
        return toPackResponse(p, maxPPP);
    }

    public void deletePack(String id) {
        if (!packRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pack not found: " + id);
        }
        packRepository.deleteById(id);
    }

    public PointPackResponse updatePack(String id, UpdatePackRequest req) {
        PointPack p = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found: " + id));
        p.setName(req.getName());
        p.setPoints(req.getPoints());
        p.setPrice(req.getPrice());
        p.setBadge(req.getBadge());
        p.setIsActive(req.isActive());
        p.setDisplayOrder(req.getDisplayOrder());
        packRepository.save(p);
        return getAllPacks().stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow();
    }

    public PointPackResponse updatePackPromo(String id, UpdatePromoRequest req) {
        PointPack p = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found: " + id));
        p.setPromoEnabled(req.isPromoEnabled());
        p.setPromoDiscountPercent(req.getPromoDiscountPercent());
        p.setPromoLabel(req.getPromoLabel());
        p.setPromoExpiresAt(req.getPromoExpiresAt());
        packRepository.save(p);
        return getAllPacks().stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow();
    }

    private PointPackResponse toPackResponse(PointPack p, double maxPPP) {
        double ppp = p.getPoints() > 0 ? p.getPrice() / p.getPoints() : 0;
        ppp = Math.round(ppp * 100.0) / 100.0;
        int savings = maxPPP > 0 ? (int) Math.round((maxPPP - ppp) / maxPPP * 100) : 0;
        Double promoPrice = Boolean.TRUE.equals(p.getPromoEnabled()) && p.getPromoDiscountPercent() > 0
                ? Math.round(p.getPrice() * (1 - p.getPromoDiscountPercent() / 100.0) * 100.0) / 100.0
                : null;
        return new PointPackResponse(
                p.getId(), p.getName(), p.getCategory(), p.getPoints(), p.getPrice(),
                ppp, savings, p.getBadge(), Boolean.TRUE.equals(p.getIsActive()),
                p.getDisplayOrder(), Boolean.TRUE.equals(p.getPromoEnabled()),
                p.getPromoDiscountPercent(), p.getPromoLabel(), p.getPromoExpiresAt(), promoPrice
        );
    }

    // ─── Subscription Plans ───────────────────────────────────────────────────

    public List<SubscriptionPlanResponse> getAllSubscriptions() {
        return subscriptionRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(this::toSubResponse).collect(Collectors.toList());
    }

    public SubscriptionPlanResponse createSubscription(CreateSubscriptionRequest req) {
        SubscriptionPlan s = new SubscriptionPlan();
        s.setName(req.getName());
        s.setPricePerMonth(req.getPricePerMonth());
        s.setPointsPerMonth(req.getPointsPerMonth());
        s.setAdvantages(req.getAdvantages());
        s.setIsActive(req.isActive());
        s.setDisplayOrder(req.getDisplayOrder());
        s.setPromoEnabled(false);
        s.setPromoDiscountPercent(0);
        s.setPromoLabel("");
        subscriptionRepository.save(s);
        return toSubResponse(s);
    }

    public void deleteSubscription(String id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subscription not found: " + id);
        }
        subscriptionRepository.deleteById(id);
    }

    public SubscriptionPlanResponse updateSubscription(String id, UpdateSubscriptionRequest req) {
        SubscriptionPlan s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
        s.setName(req.getName());
        s.setPricePerMonth(req.getPricePerMonth());
        s.setPointsPerMonth(req.getPointsPerMonth());
        s.setAdvantages(req.getAdvantages());
        s.setIsActive(req.isActive());
        s.setDisplayOrder(req.getDisplayOrder());
        subscriptionRepository.save(s);
        return toSubResponse(s);
    }

    public SubscriptionPlanResponse updateSubscriptionPromo(String id, UpdatePromoRequest req) {
        SubscriptionPlan s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
        s.setPromoEnabled(req.isPromoEnabled());
        s.setPromoDiscountPercent(req.getPromoDiscountPercent());
        s.setPromoLabel(req.getPromoLabel());
        s.setPromoExpiresAt(req.getPromoExpiresAt());
        subscriptionRepository.save(s);
        return toSubResponse(s);
    }

    private SubscriptionPlanResponse toSubResponse(SubscriptionPlan s) {
        Double promoPrice = Boolean.TRUE.equals(s.getPromoEnabled()) && s.getPromoDiscountPercent() > 0
                ? Math.round(s.getPricePerMonth() * (1 - s.getPromoDiscountPercent() / 100.0) * 100.0) / 100.0
                : null;
        return new SubscriptionPlanResponse(
                s.getId(), s.getName(), s.getPricePerMonth(), s.getPointsPerMonth(),
                s.getAdvantages(), Boolean.TRUE.equals(s.getIsActive()), s.getDisplayOrder(),
                Boolean.TRUE.equals(s.getPromoEnabled()), s.getPromoDiscountPercent(),
                s.getPromoLabel(), s.getPromoExpiresAt(), promoPrice
        );
    }
}
