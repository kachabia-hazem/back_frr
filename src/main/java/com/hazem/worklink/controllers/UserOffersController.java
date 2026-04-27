package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.response.BalanceResponse;
import com.hazem.worklink.dto.response.CompanySubscriptionResponse;
import com.hazem.worklink.dto.response.PointPackResponse;
import com.hazem.worklink.dto.response.SubscriptionPlanResponse;
import com.hazem.worklink.services.UserOffersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class UserOffersController {

    private final UserOffersService userOffersService;

    // ── Public catalog ─────────────────────────────────────────────────────────

    @GetMapping("/packs")
    public ResponseEntity<List<PointPackResponse>> getActivePacks() {
        return ResponseEntity.ok(userOffersService.getActivePacks());
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionPlanResponse>> getActiveSubscriptions() {
        return ResponseEntity.ok(userOffersService.getActiveSubscriptions());
    }

    // ── Freelancer: purchase + balance ─────────────────────────────────────────

    @PostMapping("/packs/{id}/purchase")
    @PreAuthorize("hasAuthority('FREELANCER')")
    public ResponseEntity<BalanceResponse> purchasePack(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(userOffersService.purchasePack(id, authentication.getName()));
    }

    @GetMapping("/my-balance")
    @PreAuthorize("hasAuthority('FREELANCER')")
    public ResponseEntity<BalanceResponse> getMyBalance(Authentication authentication) {
        return ResponseEntity.ok(userOffersService.getFreelancerBalance(authentication.getName()));
    }

    // ── Company: subscribe + subscription status ───────────────────────────────

    @PostMapping("/subscriptions/{id}/subscribe")
    @PreAuthorize("hasAuthority('COMPANY')")
    public ResponseEntity<CompanySubscriptionResponse> subscribeToPlan(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(userOffersService.subscribeToPlan(id, authentication.getName()));
    }

    @GetMapping("/my-subscription")
    @PreAuthorize("hasAuthority('COMPANY')")
    public ResponseEntity<CompanySubscriptionResponse> getMySubscription(Authentication authentication) {
        return ResponseEntity.ok(userOffersService.getCompanySubscription(authentication.getName()));
    }

    @GetMapping("/my-company-balance")
    @PreAuthorize("hasAuthority('COMPANY')")
    public ResponseEntity<BalanceResponse> getMyCompanyBalance(Authentication authentication) {
        return ResponseEntity.ok(userOffersService.getCompanyBalance(authentication.getName()));
    }
}
