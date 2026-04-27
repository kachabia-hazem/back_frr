package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.CreatePackRequest;
import com.hazem.worklink.dto.request.CreateSubscriptionRequest;
import com.hazem.worklink.dto.request.UpdatePackRequest;
import com.hazem.worklink.dto.request.UpdatePromoRequest;
import com.hazem.worklink.dto.request.UpdateSubscriptionRequest;
import com.hazem.worklink.dto.response.PointPackResponse;
import com.hazem.worklink.dto.response.SubscriptionPlanResponse;
import com.hazem.worklink.services.AdminPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/offers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminPointsController {

    private final AdminPointsService adminPointsService;

    // ─── One-time Packs ───────────────────────────────────────────────────────

    @GetMapping("/packs")
    public ResponseEntity<List<PointPackResponse>> getAllPacks() {
        return ResponseEntity.ok(adminPointsService.getAllPacks());
    }

    @PostMapping("/packs")
    public ResponseEntity<PointPackResponse> createPack(
            @RequestBody CreatePackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminPointsService.createPack(request));
    }

    @DeleteMapping("/packs/{id}")
    public ResponseEntity<Void> deletePack(@PathVariable String id) {
        adminPointsService.deletePack(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/packs/{id}")
    public ResponseEntity<PointPackResponse> updatePack(
            @PathVariable String id,
            @RequestBody UpdatePackRequest request) {
        return ResponseEntity.ok(adminPointsService.updatePack(id, request));
    }

    @PutMapping("/packs/{id}/promo")
    public ResponseEntity<PointPackResponse> updatePackPromo(
            @PathVariable String id,
            @RequestBody UpdatePromoRequest request) {
        return ResponseEntity.ok(adminPointsService.updatePackPromo(id, request));
    }

    // ─── Subscription Plans ───────────────────────────────────────────────────

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionPlanResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(adminPointsService.getAllSubscriptions());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<SubscriptionPlanResponse> createSubscription(
            @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminPointsService.createSubscription(request));
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable String id) {
        adminPointsService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/subscriptions/{id}")
    public ResponseEntity<SubscriptionPlanResponse> updateSubscription(
            @PathVariable String id,
            @RequestBody UpdateSubscriptionRequest request) {
        return ResponseEntity.ok(adminPointsService.updateSubscription(id, request));
    }

    @PutMapping("/subscriptions/{id}/promo")
    public ResponseEntity<SubscriptionPlanResponse> updateSubscriptionPromo(
            @PathVariable String id,
            @RequestBody UpdatePromoRequest request) {
        return ResponseEntity.ok(adminPointsService.updateSubscriptionPromo(id, request));
    }
}
