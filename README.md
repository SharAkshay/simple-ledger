# Simple Ledger

A minimal HTTP API for managing a ledger of money movements.

## Features

- Record **deposits** and **withdrawals** against any account
- Query the **current balance** for an account
- Browse the full **transaction history** for an account

## Stack

- Java 17 (uses records)
- JDK built-in HTTP server — no web framework required
- Jackson for JSON serialisation
- In-memory storage — no database or external services needed

## Running locally

Requires JDK 17+ on your `PATH`.

```bash
mvn exec:java
```

Server starts on `http://localhost:8080`.

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/accounts/{id}/deposits` | Record a deposit |
| `POST` | `/accounts/{id}/withdrawals` | Record a withdrawal |
| `GET` | `/accounts/{id}/balance` | Get current balance |
| `GET` | `/accounts/{id}/transactions` | Get transaction history |

Amounts are in **minor units** (pence, cents, etc.) as integers.

### Example

```bash
# Deposit £100.00
curl -X POST http://localhost:8080/accounts/alice/deposits \
  -H 'Content-Type: application/json' \
  -d '{"amountMinorUnits": 10000, "description": "Salary"}'

# Withdraw £25.00
curl -X POST http://localhost:8080/accounts/alice/withdrawals \
  -H 'Content-Type: application/json' \
  -d '{"amountMinorUnits": 2500, "description": "Coffee"}'

# Check balance
curl http://localhost:8080/accounts/alice/balance

# View history
curl http://localhost:8080/accounts/alice/transactions
```

### Error responses

| Status | Code | Meaning |
|--------|------|---------|
| `400` | `VALIDATION_ERROR` | Missing or invalid request body |
| `404` | `ACCOUNT_NOT_FOUND` | Account ID does not exist |
| `404` | `NOT_FOUND` | Unknown endpoint |
| `405` | `METHOD_NOT_ALLOWED` | Wrong HTTP method |
| `422` | `INSUFFICIENT_FUNDS` | Withdrawal exceeds current balance |

See [ASSUMPTIONS.md](ASSUMPTIONS.md) for design decisions and trade-offs.
