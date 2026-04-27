package com.hazem.worklink.exceptions;

public class UserBannedException extends RuntimeException {

    private final String banReason;

    public UserBannedException(String banReason) {
        super("Votre compte a été banni");
        this.banReason = banReason;
    }

    public String getBanReason() {
        return banReason;
    }
}
