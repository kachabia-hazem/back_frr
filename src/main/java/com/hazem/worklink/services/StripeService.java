package com.hazem.worklink.services;

import com.hazem.worklink.dto.response.AdminContractPaymentItem;
import com.hazem.worklink.dto.response.AdminPaymentOverviewResponse;
import com.hazem.worklink.dto.response.AdminPointTransactionItem;
import com.hazem.worklink.dto.response.FreelancerPaymentSummaryResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.PointPack;
import com.hazem.worklink.models.PointTransaction;
import com.hazem.worklink.models.enums.PaymentStatus;
import com.hazem.worklink.repositories.*;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final ContractRepository      contractRepository;
    private final FreelancerRepository    freelancerRepository;
    private final CompanyRepository       companyRepository;
    private final PointPackRepository     packRepository;
    private final PointTransactionRepository transactionRepository;
    private final NotificationService     notificationService;
    private final EmailService            emailService;

    @Value("${stripe.currency}")
    private String currency;

    @Value("${stripe.platform-fee-percent}")
    private int platformFeePercent;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final double CENTS = 100.0;

    // ─── CONTRACT ESCROW ─────────────────────────────────────────────────────

    /**
     * Called by company after contract is fully signed.
     * Creates a Stripe PaymentIntent with manual capture (escrow).
     * Returns clientSecret for the frontend Stripe.js to confirm payment.
     */
    public Map<String, Object> createContractPaymentIntent(String contractId, String companyEmail)
            throws StripeException {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (contract.getPaymentStatus() != null
                && contract.getPaymentStatus() != PaymentStatus.UNPAID
                && contract.getPaymentStatus() != PaymentStatus.FAILED) {
            throw new IllegalStateException("Payment already initiated for this contract");
        }

        // Calculate total contract amount
        double total    = calculateContractTotal(contract);
        double fee      = Math.round(total * platformFeePercent) / 100.0;
        double toFreelancer = total - fee;

        long amountCents = Math.round(total * CENTS);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                .setDescription("WorkLink — Contrat: " + contract.getMissionTitle())
                .putMetadata("contractId",   contractId)
                .putMetadata("companyId",    contract.getCompanyId())
                .putMetadata("freelancerId", contract.getFreelancerId())
                .putMetadata("type", "CONTRACT_ESCROW")
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        // Persist intent info on contract
        contract.setPaymentIntentId(intent.getId());
        contract.setPaymentStatus(PaymentStatus.UNPAID);
        contract.setTotalAmount(total);
        contract.setPlatformFee(fee);
        contract.setFreelancerAmount(toFreelancer);
        contractRepository.save(contract);

        log.info("PaymentIntent {} created for contract {} — amount: {} {}",
                intent.getId(), contractId, total, currency.toUpperCase());

        return Map.of(
                "clientSecret", intent.getClientSecret(),
                "paymentIntentId", intent.getId(),
                "totalAmount", total,
                "platformFee", fee,
                "freelancerAmount", toFreelancer,
                "currency", currency.toUpperCase()
        );
    }

    /**
     * Called when Stripe confirms payment_intent.succeeded (webhook).
     * Marks contract payment as AUTHORIZED (funds held in escrow).
     */
    public void handlePaymentAuthorized(String paymentIntentId) {
        contractRepository.findByPaymentIntentId(paymentIntentId).ifPresent(contract -> {
            contract.setPaymentStatus(PaymentStatus.AUTHORIZED);
            contract.setPaidAt(LocalDateTime.now());
            contractRepository.save(contract);

            // Notify both parties
            notificationService.sendContractPaymentNotification(
                    contract.getCompanyId(), contract.getFreelancerId(),
                    contract.getMissionTitle(), contract.getTotalAmount()
            );

            log.info("Contract {} payment AUTHORIZED — intent: {}", contract.getId(), paymentIntentId);
        });
    }

    /**
     * Called when company validates the mission → capture the held funds.
     * 93% is credited to freelancer balance. Platform keeps 7%.
     */
    public void captureContractPayment(String contractId) throws StripeException {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (contract.getPaymentStatus() != PaymentStatus.AUTHORIZED) {
            log.warn("Contract {} is not in AUTHORIZED state, skipping capture", contractId);
            return;
        }

        // Capture the PaymentIntent
        PaymentIntent intent = PaymentIntent.retrieve(contract.getPaymentIntentId());
        intent.capture(PaymentIntentCaptureParams.builder().build());

        // Update contract
        contract.setPaymentStatus(PaymentStatus.CAPTURED);
        contract.setCapturedAt(LocalDateTime.now());
        contractRepository.save(contract);

        // Log the credit (balance tracking is internal)
        freelancerRepository.findById(contract.getFreelancerId()).ifPresent(freelancer ->
            log.info("Crediting freelancer {} with {} DT (after 7% fee)",
                    freelancer.getId(), contract.getFreelancerAmount())
        );

        // Notify freelancer
        notificationService.sendPaymentReleasedNotification(
                contract.getFreelancerId(), contract.getMissionTitle(), contract.getFreelancerAmount()
        );

        log.info("Contract {} payment CAPTURED — freelancer gets {} DT, platform keeps {} DT",
                contractId, contract.getFreelancerAmount(), contract.getPlatformFee());
    }

    // ─── PACK CHECKOUT ───────────────────────────────────────────────────────

    /**
     * Creates a Stripe Checkout Session for point pack purchase.
     * Returns the Stripe-hosted checkout URL.
     */
    public String createPackCheckoutSession(String packId, String userEmail)
            throws StripeException {

        PointPack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack not found: " + packId));

        if (!Boolean.TRUE.equals(pack.getIsActive()))
            throw new IllegalStateException("Pack is not available");

        // Apply promo discount if active
        double finalPrice = pack.getPrice();
        if (Boolean.TRUE.equals(pack.getPromoEnabled()) && pack.getPromoDiscountPercent() > 0) {
            finalPrice = pack.getPrice() * (1 - pack.getPromoDiscountPercent() / 100.0);
        }

        long amountCents = Math.round(finalPrice * CENTS);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}&pack=" + packId)
                .setCancelUrl(frontendUrl + "/payment/cancel")
                .setCustomerEmail(userEmail)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("WorkLink — " + pack.getName())
                                        .setDescription(pack.getPoints() + " points · " + pack.getCategory())
                                        .build())
                                .build())
                        .build())
                .putMetadata("packId",    packId)
                .putMetadata("userEmail", userEmail)
                .putMetadata("type", "PACK_PURCHASE")
                .build();

        Session session = Session.create(params);
        log.info("Checkout session {} created for pack {} — user {}", session.getId(), packId, userEmail);
        return session.getUrl();
    }

    /**
     * Called when Stripe confirms checkout.session.completed (webhook).
     * Credits the purchased points to the user.
     */
    public void handlePackPurchaseCompleted(String sessionId, String packId, String userEmail) {
        PointPack pack = packRepository.findById(packId).orElse(null);
        if (pack == null) { log.error("Pack {} not found for session {}", packId, sessionId); return; }

        // Try freelancer first, then company
        var freelancerOpt = freelancerRepository.findByEmail(userEmail);
        if (freelancerOpt.isPresent()) {
            Freelancer fl = freelancerOpt.get();
            fl.setPointsBalance(fl.getPointsBalance() + pack.getPoints());
            freelancerRepository.save(fl);
            recordTransaction(fl.getId(), pack, sessionId);
            notificationService.sendPackPurchaseNotification(fl.getId(), pack.getName(), pack.getPoints());
            log.info("Credited {} points to freelancer {}", pack.getPoints(), userEmail);
        } else {
            companyRepository.findByEmail(userEmail).ifPresent(company -> {
                company.setPointsBalance(company.getPointsBalance() + pack.getPoints());
                companyRepository.save(company);
                recordTransaction(company.getId(), pack, sessionId);
                notificationService.sendPackPurchaseNotification(company.getId(), pack.getName(), pack.getPoints());
                log.info("Credited {} points to company {}", pack.getPoints(), userEmail);
            });
        }
    }

    /**
     * Called directly from frontend after confirmPayment() succeeds.
     * Retrieves the PaymentIntent from Stripe to verify its status, then marks the contract AUTHORIZED.
     * Works without Stripe CLI / webhooks.
     */
    public void syncContractPaymentStatus(String contractId) throws StripeException {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (contract.getPaymentIntentId() == null) {
            throw new IllegalStateException("No payment intent on this contract");
        }
        if (contract.getPaymentStatus() == PaymentStatus.AUTHORIZED
                || contract.getPaymentStatus() == PaymentStatus.CAPTURED) {
            log.info("Contract {} already {}  — skipping sync", contractId, contract.getPaymentStatus());
            return;
        }

        PaymentIntent intent = PaymentIntent.retrieve(contract.getPaymentIntentId());
        log.info("Syncing contract {} — PaymentIntent status: {}", contractId, intent.getStatus());

        // "requires_capture" = payment confirmed, funds authorized (manual capture)
        if ("requires_capture".equals(intent.getStatus())) {
            contract.setPaymentStatus(PaymentStatus.AUTHORIZED);
            contract.setPaidAt(LocalDateTime.now());
            contractRepository.save(contract);

            notificationService.sendContractPaymentNotification(
                    contract.getCompanyId(), contract.getFreelancerId(),
                    contract.getMissionTitle(), contract.getTotalAmount()
            );
            log.info("Contract {} → AUTHORIZED via direct sync", contractId);
        } else if ("succeeded".equals(intent.getStatus())) {
            contract.setPaymentStatus(PaymentStatus.CAPTURED);
            contractRepository.save(contract);
            log.info("Contract {} → CAPTURED via direct sync", contractId);
        } else if ("canceled".equals(intent.getStatus())
                || intent.getLastPaymentError() != null) {
            contract.setPaymentStatus(PaymentStatus.FAILED);
            contractRepository.save(contract);
            log.warn("Contract {} → FAILED via direct sync", contractId);
        }
    }

    /**
     * Called from the success page after Stripe Checkout redirects the user back.
     * Retrieves the Checkout Session from Stripe, verifies it is paid, then credits the pack points.
     * Idempotent — safe to call multiple times.
     */
    public void verifyAndCompletePackPurchase(String sessionId) throws StripeException {
        // Idempotency guard — already processed?
        if (transactionRepository.existsByReferenceId(sessionId)) {
            log.info("Session {} already processed — skipping", sessionId);
            return;
        }

        Session session = Session.retrieve(sessionId);
        log.info("Verifying pack purchase session {} — status: {} paymentStatus: {}",
                sessionId, session.getStatus(), session.getPaymentStatus());

        if (!"complete".equals(session.getStatus())
                || !"paid".equals(session.getPaymentStatus())) {
            throw new IllegalStateException("Session " + sessionId + " not paid");
        }

        String packId    = session.getMetadata().get("packId");
        String userEmail = session.getMetadata().get("userEmail");

        if (packId == null || userEmail == null) {
            throw new IllegalStateException("Missing metadata on session " + sessionId);
        }

        handlePackPurchaseCompleted(sessionId, packId, userEmail);
    }

    /**
     * Called when payment_intent.payment_failed webhook fires.
     * Marks contract payment as FAILED.
     */
    public void handlePaymentFailed(String paymentIntentId) {
        contractRepository.findByPaymentIntentId(paymentIntentId).ifPresent(contract -> {
            contract.setPaymentStatus(PaymentStatus.FAILED);
            contractRepository.save(contract);
            log.warn("Contract {} payment FAILED — intent: {}", contract.getId(), paymentIntentId);
        });
    }

    // ─── Freelancer payment summary ──────────────────────────────────────────

    public FreelancerPaymentSummaryResponse getFreelancerPaymentSummary(String email) {
        Freelancer freelancer = freelancerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        List<Contract> contracts = contractRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancer.getId());

        // Only contracts that have payment data
        List<Contract> paymentContracts = contracts.stream()
                .filter(c -> c.getPaymentStatus() != null && c.getPaymentStatus() != PaymentStatus.UNPAID)
                .collect(Collectors.toList());

        double escrowBalance = paymentContracts.stream()
                .filter(c -> c.getPaymentStatus() == PaymentStatus.AUTHORIZED)
                .mapToDouble(c -> c.getFreelancerAmount() != null ? c.getFreelancerAmount() : 0.0)
                .sum();

        double earnedBalance = paymentContracts.stream()
                .filter(c -> c.getPaymentStatus() == PaymentStatus.CAPTURED)
                .mapToDouble(c -> c.getFreelancerAmount() != null ? c.getFreelancerAmount() : 0.0)
                .sum();

        long escrowCount  = paymentContracts.stream().filter(c -> c.getPaymentStatus() == PaymentStatus.AUTHORIZED).count();
        long capturedCount = paymentContracts.stream().filter(c -> c.getPaymentStatus() == PaymentStatus.CAPTURED).count();

        List<FreelancerPaymentSummaryResponse.ContractPaymentItem> items = paymentContracts.stream()
                .map(c -> new FreelancerPaymentSummaryResponse.ContractPaymentItem(
                        c.getId(),
                        c.getMissionTitle(),
                        c.getCompanyName(),
                        c.getPaymentStatus(),
                        c.getFreelancerAmount(),
                        c.getPaidAt(),
                        c.getCapturedAt()
                ))
                .collect(Collectors.toList());

        return new FreelancerPaymentSummaryResponse(
                escrowBalance, earnedBalance,
                (int) escrowCount, (int) capturedCount,
                items
        );
    }

    // ─── Status query ────────────────────────────────────────────────────────

    public Map<String, Object> getContractPaymentStatus(String contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        return Map.of(
                "contractId",      contractId,
                "paymentStatus",   contract.getPaymentStatus() != null ? contract.getPaymentStatus() : "UNPAID",
                "totalAmount",     contract.getTotalAmount() != null ? contract.getTotalAmount() : 0.0,
                "platformFee",     contract.getPlatformFee() != null ? contract.getPlatformFee() : 0.0,
                "freelancerAmount",contract.getFreelancerAmount() != null ? contract.getFreelancerAmount() : 0.0
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private double calculateContractTotal(Contract contract) {
        if (contract.getSalary() == null) return 0.0;
        if (contract.getStartDate() == null || contract.getEndDate() == null) {
            return contract.getSalary();
        }
        long days = ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate()) + 1;
        return contract.getSalary() * Math.max(days, 1);
    }

    private void recordTransaction(String userId, PointPack pack, String stripeRef) {
        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setType("PURCHASE_PACK");
        tx.setPoints(pack.getPoints());
        tx.setAmount(pack.getPrice());
        tx.setReferenceId(stripeRef);
        tx.setDescription(pack.getName() + " — " + pack.getPoints() + " pts");
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    // ─── Admin Payment Analytics ─────────────────────────────────────────────

    public AdminPaymentOverviewResponse getAdminPaymentOverview() {
        List<Contract> allContracts = contractRepository.findAll();

        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        double totalEscrow = allContracts.stream()
                .filter(c -> c.getPaymentStatus() == PaymentStatus.AUTHORIZED)
                .mapToDouble(c -> c.getFreelancerAmount() != null ? c.getFreelancerAmount() : 0.0)
                .sum();

        double releasedThisMonth = allContracts.stream()
                .filter(c -> c.getPaymentStatus() == PaymentStatus.CAPTURED
                        && c.getCapturedAt() != null
                        && c.getCapturedAt().isAfter(startOfMonth))
                .mapToDouble(c -> c.getFreelancerAmount() != null ? c.getFreelancerAmount() : 0.0)
                .sum();

        double totalPlatformCommission = allContracts.stream()
                .filter(c -> c.getPaymentStatus() == PaymentStatus.CAPTURED)
                .mapToDouble(c -> c.getPlatformFee() != null ? c.getPlatformFee() : 0.0)
                .sum();

        int escrowCount = (int) allContracts.stream()
                .filter(c -> c.getPaymentStatus() == PaymentStatus.AUTHORIZED)
                .count();

        int capturedThisMonthCount = (int) allContracts.stream()
                .filter(c -> c.getPaymentStatus() == PaymentStatus.CAPTURED
                        && c.getCapturedAt() != null
                        && c.getCapturedAt().isAfter(startOfMonth))
                .count();

        int totalCaptured = (int) allContracts.stream()
                .filter(c -> c.getPaymentStatus() == PaymentStatus.CAPTURED)
                .count();

        return new AdminPaymentOverviewResponse(
                totalEscrow, releasedThisMonth, totalPlatformCommission,
                escrowCount, capturedThisMonthCount, totalCaptured);
    }

    public List<AdminContractPaymentItem> getAdminContractPayments(String statusFilter, String search) {
        List<Contract> contracts = contractRepository.findAll().stream()
                .filter(c -> c.getPaymentStatus() != null && c.getPaymentStatus() != PaymentStatus.UNPAID)
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            try {
                PaymentStatus ps = PaymentStatus.valueOf(statusFilter.toUpperCase());
                contracts = contracts.stream()
                        .filter(c -> c.getPaymentStatus() == ps)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            contracts = contracts.stream()
                    .filter(c -> (c.getFreelancerName() != null && c.getFreelancerName().toLowerCase().contains(q))
                            || (c.getFreelancerEmail() != null && c.getFreelancerEmail().toLowerCase().contains(q))
                            || (c.getCompanyName() != null && c.getCompanyName().toLowerCase().contains(q))
                            || (c.getCompanyEmail() != null && c.getCompanyEmail().toLowerCase().contains(q))
                            || (c.getMissionTitle() != null && c.getMissionTitle().toLowerCase().contains(q))
                            || c.getId().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        return contracts.stream()
                .map(c -> new AdminContractPaymentItem(
                        c.getId(),
                        c.getFreelancerName(),
                        c.getFreelancerEmail(),
                        c.getCompanyName(),
                        c.getCompanyEmail(),
                        c.getMissionTitle(),
                        c.getTotalAmount(),
                        c.getPlatformFee(),
                        c.getFreelancerAmount(),
                        c.getPaymentStatus(),
                        c.getCreatedAt(),
                        c.getPaidAt(),
                        c.getCapturedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<AdminPointTransactionItem> getAdminPointTransactions() {
        List<PointTransaction> txs = transactionRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        return txs.stream().map(tx -> {
            String userName  = "Utilisateur inconnu";
            String userEmail = "";

            var freelancerOpt = freelancerRepository.findById(tx.getUserId());
            if (freelancerOpt.isPresent()) {
                var fl = freelancerOpt.get();
                userName  = (fl.getFirstName() != null ? fl.getFirstName() : "") + " "
                          + (fl.getLastName()  != null ? fl.getLastName()  : "");
                userEmail = fl.getEmail() != null ? fl.getEmail() : "";
            } else {
                var companyOpt = companyRepository.findById(tx.getUserId());
                if (companyOpt.isPresent()) {
                    var co = companyOpt.get();
                    userName  = co.getCompanyName() != null ? co.getCompanyName() : "Entreprise";
                    userEmail = co.getEmail() != null ? co.getEmail() : "";
                }
            }

            return new AdminPointTransactionItem(
                    tx.getId(),
                    tx.getUserId(),
                    userName.trim(),
                    userEmail,
                    tx.getType() != null ? tx.getType() : "PURCHASE_PACK",
                    tx.getPoints(),
                    tx.getAmount(),
                    tx.getDescription() != null ? tx.getDescription() : "",
                    tx.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }
}
