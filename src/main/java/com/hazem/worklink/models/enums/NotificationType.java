package com.hazem.worklink.models.enums;

public enum NotificationType {
    // ── Freelancer notifications ──────────────────────────────────────
    WELCOME,
    APPLICATION_SUBMITTED,
    APPLICATION_ACCEPTED,
    APPLICATION_REJECTED,
    APPLICATION_WITHDRAWN,
    NEW_MISSION_MATCH,
    MISSION_DEADLINE_SOON,
    PROFILE_INCOMPLETE,

    // ── Company notifications ─────────────────────────────────────────
    COMPANY_WELCOME,
    MISSION_PUBLISHED,
    APPLICATION_RECEIVED,
    PENDING_APPLICATIONS_REMINDER,
    MISSION_CLOSED,

    // ── Contract notifications ────────────────────────────────────────
    CONTRACT_GENERATED,            // freelancer: a contract is ready to sign
    CONTRACT_SIGNED,               // company: freelancer signed the contract
    CONTRACT_SIGNATURE_REMINDER,   // freelancer: reminder to sign after 3 days
    CONTRACT_REJECTED,             // company: freelancer rejected the contract

    // ── Mission validation notifications ─────────────────────────────
    MISSION_SUBMITTED,             // company: freelancer submitted work for validation
    MISSION_VALIDATED              // freelancer: company approved or requested revision
}
