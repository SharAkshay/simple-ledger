package com.ledger;

import com.ledger.model.Transaction;

import java.util.List;

public interface LedgerPort {
    Transaction deposit(String accountId, long amount, String description);
    Transaction withdraw(String accountId, long amount, String description);
    long getBalance(String accountId);
    List<Transaction> getTransactions(String accountId);
}
