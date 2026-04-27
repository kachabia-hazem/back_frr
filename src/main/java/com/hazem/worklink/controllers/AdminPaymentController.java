package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.response.AdminContractPaymentItem;
import com.hazem.worklink.dto.response.AdminPaymentOverviewResponse;
import com.hazem.worklink.dto.response.AdminPointTransactionItem;
import com.hazem.worklink.services.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final StripeService stripeService;

    /** GET /api/admin/payments/overview — 4 KPI metrics for the admin dashboard */
    @GetMapping("/overview")
    public ResponseEntity<AdminPaymentOverviewResponse> getOverview() {
        return ResponseEntity.ok(stripeService.getAdminPaymentOverview());
    }

    /** GET /api/admin/payments/contracts?status=&search=
     *  All contracts with payment data (paymentStatus != UNPAID), with optional filters */
    @GetMapping("/contracts")
    public ResponseEntity<List<AdminContractPaymentItem>> getContracts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(stripeService.getAdminContractPayments(status, search));
    }

    /** GET /api/admin/payments/transactions/points — All point pack / subscription transactions */
    @GetMapping("/transactions/points")
    public ResponseEntity<List<AdminPointTransactionItem>> getPointTransactions() {
        return ResponseEntity.ok(stripeService.getAdminPointTransactions());
    }
}
