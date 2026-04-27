package com.hazem.worklink.controllers;

import com.hazem.worklink.services.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeService stripeService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    /**
     * POST /api/webhook/stripe
     * Receives and processes Stripe webhook events.
     * Must be permitted without authentication (see SecurityConfig).
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        boolean devMode = "whsec_placeholder".equals(webhookSecret);
        try {
            if (devMode) {
                // Dev mode: skip signature verification (Stripe CLI secret not configured)
                event = ApiResource.GSON.fromJson(payload, Event.class);
                log.info("[DEV] Webhook signature skipped — set STRIPE_WEBHOOK_SECRET to enable verification");
            } else {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            }
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature. Run: stripe listen --forward-to localhost:8080/api/webhook/stripe");
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Error parsing Stripe webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        log.info("Stripe webhook received: {}", event.getType());

        switch (event.getType()) {

            // Manual-capture PI: "requires_capture" = customer paid, funds held
            case "payment_intent.amount_capturable_updated" -> {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    PaymentIntent intent = (PaymentIntent) obj;
                    String type = intent.getMetadata().get("type");
                    if ("CONTRACT_ESCROW".equals(type)) {
                        stripeService.handlePaymentAuthorized(intent.getId());
                    }
                });
            }

            // payment_intent.succeeded fires after CAPTURE (mission validated)
            case "payment_intent.succeeded" -> {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    PaymentIntent intent = (PaymentIntent) obj;
                    String type = intent.getMetadata().get("type");
                    if ("CONTRACT_ESCROW".equals(type)) {
                        // Already handled by captureContractPayment() — just log
                        log.info("PaymentIntent {} succeeded (captured)", intent.getId());
                    }
                });
            }

            case "checkout.session.completed" -> {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    Session session = (Session) obj;
                    String type = session.getMetadata().get("type");
                    if ("PACK_PURCHASE".equals(type)) {
                        String packId    = session.getMetadata().get("packId");
                        String userEmail = session.getMetadata().get("userEmail");
                        stripeService.handlePackPurchaseCompleted(session.getId(), packId, userEmail);
                    }
                });
            }

            case "payment_intent.payment_failed" -> {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    PaymentIntent intent = (PaymentIntent) obj;
                    log.warn("Payment failed for intent {}", intent.getId());
                    stripeService.handlePaymentFailed(intent.getId());
                });
            }

            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("OK");
    }
}
