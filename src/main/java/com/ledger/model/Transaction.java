package com.ledger.model;

import java.time.Instant;
import java.util.UUID;

public record Transaction(
        String id,
        String accountId,
        TransactionType type,
        long amountMinorUnits,
        String description,
        Instant createdAt
) {
    public static Transaction deposit(final String accountId, final long amount, final String description) {
        return new Transaction(UUID.randomUUID().toString(), accountId, TransactionType.DEPOSIT, amount, description, Instant.now());
    }

    public static Transaction withdrawal(final String accountId, final long amount, final String description) {
        return new Transaction(UUID.randomUUID().toString(), accountId, TransactionType.WITHDRAWAL, amount, description, Instant.now());
    }
}
