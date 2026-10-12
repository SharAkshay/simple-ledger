package com.ledger.exception;

public final class AccountNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public final String accountId;

    public AccountNotFoundException(final String accountId) {
        super("Account not found: " + accountId);
        this.accountId = accountId;
    }
}
