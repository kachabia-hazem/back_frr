package com.hazem.worklink.dto.response;

import com.hazem.worklink.models.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerPaymentSummaryResponse {

    /** Total argent bloqué en escrow (contrats AUTHORIZED) en EUR */
    private double escrowBalance;

    /** Total argent libéré et gagné (contrats CAPTURED) en EUR */
    private double earnedBalance;

    /** Nombre de contrats avec paiement en escrow */
    private int escrowContractCount;

    /** Nombre de contrats dont le paiement a été libéré */
    private int earnedContractCount;

    /** Liste des contrats avec leur état de paiement */
    private List<ContractPaymentItem> contracts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractPaymentItem {
        private String id;
        private String missionTitle;
        private String companyName;
        private PaymentStatus paymentStatus;
        private Double freelancerAmount;
        private LocalDateTime paidAt;
        private LocalDateTime capturedAt;
    }
}
