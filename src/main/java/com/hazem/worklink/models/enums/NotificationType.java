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
    MISSION_VALIDATED,             // freelancer: company approved or requested revision

    // ── Mission lifecycle notifications ───────────────────────────────
    MISSION_ACTIVATED,             // freelancer + company: pending mission start date reached
    ACTIVE_MISSION_CANCELLED,      // freelancer + company: active mission cancelled due to company non-payment

    // ── Admin / Company verification notifications ────────────────────────────
    COMPANY_PENDING_VERIFICATION,  // company: inscription en attente de validation
    COMPANY_APPROVED,              // company: compte approuvé par l'admin
    COMPANY_REJECTED,              // company: compte rejeté par l'admin

    // ── Admin mission management ──────────────────────────────────────
    MISSION_DELETED_BY_ADMIN,      // company: une mission supprimée par l'admin

    // ── Admin contract management ─────────────────────────────────────
    CONTRACT_CANCELLED_BY_ADMIN,   // freelancer + company: contrat annulé par l'admin
    CONTRACT_AUTO_CANCELLED,              // freelancer + company: payment deadline expired, auto-cancelled
    CONTRACT_PAYMENT_DEADLINE_WARNING,    // company: payment deadline approaching (3 days / 1 day before)
    CONTRACT_EXPIRED_UNSIGNED,            // freelancer + company: contract auto-cancelled — freelancer never signed

    // ── Feedback moderation ───────────────────────────────────────────
    FEEDBACK_VALIDATED,            // user: feedback approuvé et publié sur la page d'accueil
    FEEDBACK_REJECTED,             // user: feedback rejeté avec motif

    // ── Admin-targeted notifications ──────────────────────────────────
    ADMIN_COMPANY_VERIFICATION_REQUEST, // admin: une entreprise attend vérification
    ADMIN_NEW_FREELANCER_REGISTERED,    // admin: nouveau freelancer inscrit
    ADMIN_NEW_CONTRACT_SIGNED,          // admin: un contrat a été signé
    ADMIN_NEW_MISSION_PUBLISHED,        // admin: une nouvelle mission publiée

    // ── Payment notifications ─────────────────────────────────────────
    CONTRACT_PAYMENT_AUTHORIZED,        // both: payment secured in escrow
    CONTRACT_PAYMENT_RELEASED,          // freelancer: funds released after validation
    PACK_PURCHASED,                     // user: points credited after Stripe checkout

    // ── Report notifications ──────────────────────────────────────────
    REPORT_WARNED,      // reported-against party: admin issued a warning following a report
    REPORT_REJECTED,    // reporter: admin rejected their report

    // ── Legit (dispute) notifications ────────────────────────────────
    LEGIT_OPENED,                         // both parties: a dispute was opened on a mission
    LEGIT_RESPONSE_FROM_ADMIN,            // user: admin sent a response to their legit
    LEGIT_MISSION_CANCELLED_BY_ADMIN,     // both: admin cancelled mission, funds go to company escrow
    LEGIT_REFUND_BY_ADMIN,               // both: admin decided refund with percentage split
    LEGIT_MISSION_CONTINUED_BY_ADMIN,    // both: admin decided to continue the mission
    LEGIT_STATUS_UPDATED                 // both: admin changed legit status (progress update)
}
