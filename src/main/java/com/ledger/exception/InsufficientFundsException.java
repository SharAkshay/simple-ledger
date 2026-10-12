package com.ledger.exception;

public final class InsufficientFundsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public final long balance;
    public final long requested;

    public InsufficientFundsException(final long balance, final long requested) {
        super("Insufficient funds: balance %d, requested %d".formatted(balance, requested));
        this.balance = balance;
        this.requested = requested;
    }
}
