package com.hazem.worklink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentOverviewResponse {
    /** Sum of freelancerAmount for all AUTHORIZED contracts */
    private double totalEscrow;
    /** Sum of freelancerAmount for CAPTURED contracts since start of current month */
    private double releasedThisMonth;
    /** Sum of platformFee for all CAPTURED contracts ever */
    private double totalPlatformCommission;
    /** Number of AUTHORIZED contracts */
    private int escrowContractCount;
    /** Number of CAPTURED contracts this month */
    private int capturedThisMonthCount;
    /** Total number of CAPTURED contracts ever */
    private int totalCapturedContracts;
}
