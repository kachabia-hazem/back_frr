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
    MISSION_CLOSED
}
