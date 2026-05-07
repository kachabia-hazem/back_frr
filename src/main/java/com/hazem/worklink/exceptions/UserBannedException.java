package com.hazem.worklink.exceptions;

public class UserBannedException extends RuntimeException {

    private final String banReason;
    private final String userId;
    private final String userType;

    public UserBannedException(String banReason, String userId, String userType) {
        super("Votre compte a été banni");
        this.banReason = banReason;
        this.userId = userId;
        this.userType = userType;
    }

    public String getBanReason() { return banReason; }
    public String getUserId() { return userId; }
    public String getUserType() { return userType; }
}
