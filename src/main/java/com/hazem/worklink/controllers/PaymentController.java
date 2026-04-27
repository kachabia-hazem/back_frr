package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.response.FreelancerPaymentSummaryResponse;
import com.hazem.worklink.services.StripeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final StripeService stripeService;

    /** POST /api/payments/contract/{id}/intent
     *  Company initiates escrow payment after contract is fully signed.
     *  Returns clientSecret + amount breakdown for Stripe.js. */
    @PostMapping("/contract/{contractId}/intent")
    public ResponseEntity<Map<String, Object>> createContractPaymentIntent(
            @PathVariable String contractId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> result = stripeService.createContractPaymentIntent(
                    contractId, userDetails.getUsername());
            return ResponseEntity.ok(result);
        } catch (StripeException e) {
            log.error("Stripe error creating payment intent for contract {}: {}", contractId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/payments/freelancer/summary
     *  Returns escrow balance + earned balance + contract payment list for the authenticated freelancer. */
    @GetMapping("/freelancer/summary")
    public ResponseEntity<FreelancerPaymentSummaryResponse> getFreelancerPaymentSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        FreelancerPaymentSummaryResponse summary =
                stripeService.getFreelancerPaymentSummary(userDetails.getUsername());
        return ResponseEntity.ok(summary);
    }

    /** GET /api/payments/contract/{id}/status
     *  Returns payment status of a contract (accessible by both parties). */
    @GetMapping("/contract/{contractId}/status")
    public ResponseEntity<Map<String, Object>> getContractPaymentStatus(
            @PathVariable String contractId) {
        // ContractRepository is accessed via StripeService; expose a simple status check
        return ResponseEntity.ok(stripeService.getContractPaymentStatus(contractId));
    }

    /** POST /api/payments/packs/checkout
     *  Creates a Stripe Checkout Session for point pack purchase.
     *  Returns the Stripe-hosted checkout URL. */
    @PostMapping("/packs/checkout")
    public ResponseEntity<Map<String, String>> createPackCheckout(
            @RequestParam String packId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String url = stripeService.createPackCheckoutSession(packId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("checkoutUrl", url));
        } catch (StripeException e) {
            log.error("Stripe error creating checkout session for pack {}: {}", packId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/payments/contract/{id}/sync
     *  Called by frontend after Stripe.js confirms the payment.
     *  Retrieves PaymentIntent from Stripe and updates contract status directly — no webhook needed. */
    @PostMapping("/contract/{contractId}/sync")
    public ResponseEntity<Map<String, Object>> syncContractPayment(
            @PathVariable String contractId) {
        try {
            stripeService.syncContractPaymentStatus(contractId);
            return ResponseEntity.ok(stripeService.getContractPaymentStatus(contractId));
        } catch (StripeException e) {
            log.error("Stripe error syncing contract {}: {}", contractId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/payments/packs/verify
     *  Called from /payment/success page after Stripe Checkout redirect.
     *  Verifies session is paid and credits points — no webhook needed. */
    @PostMapping("/packs/verify")
    public ResponseEntity<Map<String, String>> verifyPackPurchase(
            @RequestParam String sessionId) {
        try {
            stripeService.verifyAndCompletePackPurchase(sessionId);
            return ResponseEntity.ok(Map.of("status", "credited"));
        } catch (StripeException e) {
            log.error("Stripe error verifying session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
