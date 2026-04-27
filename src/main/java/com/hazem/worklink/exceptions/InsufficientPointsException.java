package com.hazem.worklink.exceptions;

public class InsufficientPointsException extends RuntimeException {

    private final int required;
    private final int available;

    public InsufficientPointsException(int required, int available) {
        super("Solde insuffisant : " + available + " pts disponibles, " + required + " pts requis.");
        this.required = required;
        this.available = available;
    }

    public int getRequired()  { return required; }
    public int getAvailable() { return available; }
}
