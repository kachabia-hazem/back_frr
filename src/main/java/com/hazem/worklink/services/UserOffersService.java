package com.hazem.worklink.services;

import com.hazem.worklink.dto.response.*;
import com.hazem.worklink.exceptions.InsufficientPointsException;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.PointPack;
import com.hazem.worklink.models.PointTransaction;
import com.hazem.worklink.models.SubscriptionPlan;
import com.hazem.worklink.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserOffersService {

    private final PointPackRepository packRepository;
    private final SubscriptionPlanRepository subscriptionRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final PointTransactionRepository transactionRepository;
    private final AdminPointsService adminPointsService;

    // ── Public catalog ─────────────────────────────────────────────────────────

    public List<PointPackResponse> getActivePacks() {
        List<PointPack> all = packRepository.findAllByOrderByDisplayOrderAsc();
        List<PointPack> active = all.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList();
        double maxPPP = active.stream()
                .filter(p -> p.getPoints() > 0)
                .mapToDouble(p -> p.getPrice() / p.getPoints())
                .max().orElse(1.0);
        return active.stream().map(p -> toPackResponse(p, maxPPP)).toList();
    }

    public List<SubscriptionPlanResponse> getActiveSubscriptions() {
        return subscriptionRepository.findAllByOrderByDisplayOrderAsc().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .map(this::toSubResponse)
                .toList();
    }

    // ── Freelancer: purchase pack ──────────────────────────────────────────────

    public BalanceResponse purchasePack(String packId, String email) {
        PointPack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found: " + packId));
        if (!Boolean.TRUE.equals(pack.getIsActive()))
            throw new IllegalStateException("Pack not available");

        Freelancer freelancer = freelancerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        freelancer.setPointsBalance(freelancer.getPointsBalance() + pack.getPoints());
        freelancerRepository.save(freelancer);

        double price = Boolean.TRUE.equals(pack.getPromoEnabled()) && pack.getPromoDiscountPercent() > 0
                ? Math.round(pack.getPrice() * (1 - pack.getPromoDiscountPercent() / 100.0) * 1000.0) / 1000.0
                : pack.getPrice();

        PointTransaction tx = new PointTransaction();
        tx.setUserId(freelancer.getId());
        tx.setType("PURCHASE_PACK");
        tx.setReferenceId(packId);
        tx.setPoints(pack.getPoints());
        tx.setAmount(price);
        tx.setDescription("Achat " + pack.getName());
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        return buildBalanceResponse(freelancer.getId());
    }

    // ── Company: subscribe to plan ─────────────────────────────────────────────

    public CompanySubscriptionResponse subscribeToPlan(String planId, String email) {
        SubscriptionPlan plan = subscriptionRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
        if (!Boolean.TRUE.equals(plan.getIsActive()))
            throw new IllegalStateException("Plan not available");

        Company company = companyRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        LocalDateTime now = LocalDateTime.now();
        company.setSubscriptionPlanId(planId);
        company.setSubscriptionStartDate(now);
        company.setSubscriptionExpiresAt(now.plusMonths(1));
        company.setPointsBalance(company.getPointsBalance() + plan.getPointsPerMonth());
        companyRepository.save(company);

        double price = Boolean.TRUE.equals(plan.getPromoEnabled()) && plan.getPromoDiscountPercent() > 0
                ? Math.round(plan.getPricePerMonth() * (1 - plan.getPromoDiscountPercent() / 100.0) * 1000.0) / 1000.0
                : plan.getPricePerMonth();

        PointTransaction tx = new PointTransaction();
        tx.setUserId(company.getId());
        tx.setType("SUBSCRIBE_PLAN");
        tx.setReferenceId(planId);
        tx.setPoints(plan.getPointsPerMonth());
        tx.setAmount(price);
        tx.setDescription("Abonnement " + plan.getName());
        tx.setCreatedAt(now);
        transactionRepository.save(tx);

        return buildCompanySubscription(company);
    }

    // ── Freelancer: balance + history ──────────────────────────────────────────

    public BalanceResponse getFreelancerBalance(String email) {
        Freelancer freelancer = freelancerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        return buildBalanceResponse(freelancer.getId());
    }

    // ── Company: subscription status ──────────────────────────────────────────

    public CompanySubscriptionResponse getCompanySubscription(String email) {
        Company company = companyRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return buildCompanySubscription(company);
    }

    // ── Company: balance + transaction history ────────────────────────────────

    public BalanceResponse getCompanyBalance(String email) {
        Company company = companyRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        List<TransactionResponse> txs = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(company.getId())
                .stream().map(this::toTxResponse).toList();
        return new BalanceResponse(company.getPointsBalance(), txs);
    }

    // ── Point deduction (used by business actions) ─────────────────────────────

    public static final int COST_APPLICATION = 3;
    public static final int COST_AI_MATCHING  = 5;
    public static final int COST_AI_RANKING   = 5;

    public void deductFreelancerPoints(String freelancerId, String type, int cost, String description, String referenceId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        if (freelancer.getPointsBalance() < cost)
            throw new InsufficientPointsException(cost, freelancer.getPointsBalance());

        freelancer.setPointsBalance(freelancer.getPointsBalance() - cost);
        freelancerRepository.save(freelancer);

        PointTransaction tx = new PointTransaction();
        tx.setUserId(freelancerId);
        tx.setType(type);
        tx.setReferenceId(referenceId);
        tx.setPoints(-cost);
        tx.setAmount(0);
        tx.setDescription(description);
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    public void deductCompanyPoints(String companyId, String type, int cost, String description, String referenceId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        if (company.getPointsBalance() < cost)
            throw new InsufficientPointsException(cost, company.getPointsBalance());

        company.setPointsBalance(company.getPointsBalance() - cost);
        companyRepository.save(company);

        PointTransaction tx = new PointTransaction();
        tx.setUserId(companyId);
        tx.setType(type);
        tx.setReferenceId(referenceId);
        tx.setPoints(-cost);
        tx.setAmount(0);
        tx.setDescription(description);
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private BalanceResponse buildBalanceResponse(String freelancerId) {
        Freelancer f = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        List<TransactionResponse> txs = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(freelancerId)
                .stream().map(this::toTxResponse).toList();
        return new BalanceResponse(f.getPointsBalance(), txs);
    }

    private CompanySubscriptionResponse buildCompanySubscription(Company company) {
        if (company.getSubscriptionPlanId() == null) {
            return new CompanySubscriptionResponse(false, company.getPointsBalance(), null, null, null);
        }
        SubscriptionPlan plan = subscriptionRepository.findById(company.getSubscriptionPlanId()).orElse(null);
        if (plan == null) {
            return new CompanySubscriptionResponse(false, company.getPointsBalance(), null, null, null);
        }
        boolean active = company.getSubscriptionExpiresAt() != null
                && company.getSubscriptionExpiresAt().isAfter(LocalDateTime.now());

        double maxPPP = 1.0;
        SubscriptionPlanResponse planResp = toSubResponse(plan);
        return new CompanySubscriptionResponse(
                active, company.getPointsBalance(), planResp,
                company.getSubscriptionStartDate(), company.getSubscriptionExpiresAt()
        );
    }

    private TransactionResponse toTxResponse(PointTransaction tx) {
        return new TransactionResponse(
                tx.getId(), tx.getType(), tx.getDescription(),
                tx.getPoints(), tx.getAmount(), tx.getCreatedAt()
        );
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
                ppp, savings, p.getBadge(), true,
                p.getDisplayOrder(), Boolean.TRUE.equals(p.getPromoEnabled()),
                p.getPromoDiscountPercent(), p.getPromoLabel(), p.getPromoExpiresAt(), promoPrice
        );
    }

    private SubscriptionPlanResponse toSubResponse(SubscriptionPlan s) {
        Double promoPrice = Boolean.TRUE.equals(s.getPromoEnabled()) && s.getPromoDiscountPercent() > 0
                ? Math.round(s.getPricePerMonth() * (1 - s.getPromoDiscountPercent() / 100.0) * 100.0) / 100.0
                : null;
        return new SubscriptionPlanResponse(
                s.getId(), s.getName(), s.getPricePerMonth(), s.getPointsPerMonth(),
                s.getAdvantages(), true, s.getDisplayOrder(),
                Boolean.TRUE.equals(s.getPromoEnabled()), s.getPromoDiscountPercent(),
                s.getPromoLabel(), s.getPromoExpiresAt(), promoPrice
        );
    }
}
