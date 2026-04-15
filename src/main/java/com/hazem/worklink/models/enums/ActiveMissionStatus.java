package com.hazem.worklink.models.enums;

public enum ActiveMissionStatus {
    PENDING,     // Contract signed but mission start date not yet reached
    ACTIVE,
    SUBMITTED,   // Freelancer marked work as done — awaiting company validation
    COMPLETED,
    PAUSED,
    DISPUTE
}
