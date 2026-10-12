# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the server (starts on http://localhost:8080)
mvn exec:java

# Compile only
mvn compile

# Package
mvn package
```

There are no tests in this project. There is no linter configured.

## Architecture

No framework, no DI container — everything is wired manually in `main()`. Java 17 with
Jackson for JSON serialization (`jackson-databind` + `jackson-datatype-jsr310`).

**Request path:** `LedgerApp` → `LedgerHandler` → `LedgerStore`

- **`LedgerApp`** — entry point. Wires `ObjectMapper`, `LedgerStore`, and `HttpServer`, then starts on port 8080 with a cached thread pool executor (`Executors.newCachedThreadPool()`).
- **`LedgerHandler`** — single `HttpHandler` on `/`. Matches
  `^/accounts/([^/]+)/(deposits|withdrawals|balance|transactions)$`, enforces HTTP methods,
  and maps domain exceptions to HTTP status codes (400/404/405/422/500).
- **`LedgerPort`** — interface separating `LedgerHandler` from the concrete store. The four
  methods are `deposit`, `withdraw`, `getBalance`, `getTransactions`.
- **`LedgerStore`** — implements `LedgerPort`. One `AccountState` (balance `long` +
  transaction `ArrayList`) per account in a `ConcurrentHashMap`. `computeIfAbsent` for
  creation; `synchronized` on `AccountState` for reads/writes — different accounts never
  contend.
- **`http/RequestParser`** — deserializes JSON body into a typed record; validates that `amountMinorUnits` is a positive `long`.
- **`http/ResponseWriter`** — serializes any object to JSON and writes it to the `HttpExchange` response.
- **`model/Transaction`** — immutable record; factory methods `deposit()` and `withdrawal()`.
- **`model/TransactionType`** — enum `DEPOSIT` / `WITHDRAWAL`.
- **`exception/`** — three typed exceptions: `AccountNotFoundException` (→ 404),
  `InsufficientFundsException` (→ 422, carries `balance` and `requested` fields),
  `MethodNotAllowedException` (→ 405).

## Key design decisions

- Amounts are `long` in minor units (cents) — no floating point.
- Accounts are auto-created on first deposit; balance/history/withdrawal on an unknown ID returns 404.
- Withdrawals below zero return 422 `INSUFFICIENT_FUNDS` with current balance and requested amount in the error body.
- All state is in-memory only — data is lost on restart by design.
- Uses `com.sun.net.httpserver` (JDK built-in) — no servlet container or web framework.

See `ASSUMPTIONS.md` for explicit trade-offs and out-of-scope items (auth, persistence, pagination, transfers, etc.).
