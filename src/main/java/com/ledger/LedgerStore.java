package com.ledger;

import com.ledger.exception.AccountNotFoundException;
import com.ledger.exception.InsufficientFundsException;
import com.ledger.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory ledger store. One AccountState per account ID.
 *
 * Thread safety: ConcurrentHashMap.computeIfAbsent ensures at-most-one AccountState per key.
 * Balance updates and transaction appends are then synchronized on the AccountState itself,
 * so operations on different accounts never block each other.
 */
public final class LedgerStore implements LedgerPort {

    private static final class AccountState {
        long balance = 0;
        final List<Transaction> transactions = new ArrayList<>();
    }

    private final Map<String, AccountState> accounts = new ConcurrentHashMap<>();

    @Override
    public Transaction deposit(final String accountId, final long amount, final String description) {
        final var state = accounts.computeIfAbsent(accountId, id -> new AccountState());
        synchronized (state) {
            final var tx = Transaction.deposit(accountId, amount, description);
            state.balance += amount;
            state.transactions.add(tx);
            return tx;
        }
    }

    @Override
    public Transaction withdraw(final String accountId, final long amount, final String description) {
        final var state = requireAccount(accountId);
        synchronized (state) {
            if (state.balance < amount) {
                throw new InsufficientFundsException(state.balance, amount);
            }
            final var tx = Transaction.withdrawal(accountId, amount, description);
            state.balance -= amount;
            state.transactions.add(tx);
            return tx;
        }
    }

    @Override
    public long getBalance(final String accountId) {
        final var state = requireAccount(accountId);
        synchronized (state) {
            return state.balance;
        }
    }

    @Override
    public List<Transaction> getTransactions(final String accountId) {
        final var state = requireAccount(accountId);
        synchronized (state) {
            return Collections.unmodifiableList(new ArrayList<>(state.transactions));
        }
    }

    private AccountState requireAccount(final String accountId) {
        final var state = accounts.get(accountId);
        if (state == null) throw new AccountNotFoundException(accountId);
        return state;
    }

}
