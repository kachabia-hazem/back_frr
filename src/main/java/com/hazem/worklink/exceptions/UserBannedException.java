package com.hazem.worklink.exceptions;

import java.time.LocalDateTime;

public class UserBannedException extends RuntimeException {

    private final String banReason;
    private final String banDuration;
    private final LocalDateTime banStartDate;
    private final String userId;
    private final String userType;

    public UserBannedException(String banReason, String banDuration, LocalDateTime banStartDate, String userId, String userType) {
        super("Votre compte a été banni");
        this.banReason = banReason;
        this.banDuration = banDuration;
        this.banStartDate = banStartDate;
        this.userId = userId;
        this.userType = userType;
    }

    public String getBanReason() { return banReason; }
    public String getBanDuration() { return banDuration; }
    public LocalDateTime getBanStartDate() { return banStartDate; }
    public String getUserId() { return userId; }
    public String getUserType() { return userType; }
}
