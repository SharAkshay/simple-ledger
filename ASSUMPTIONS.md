# Assumptions

## Currency & amounts

- All amounts are expressed in the **smallest indivisible unit** of the currency (cents etc.)
  as a `long`. The API does not know or care which currency — that context is assumed to be
  established externally.
- This avoids IEEE 754 floating-point rounding errors that arise with `double` or `float`.
  A value of `1000` means €10.00 if you are working in euro cents, $10.00 if in cents, etc.
- Amounts must be **strictly positive** (`> 0`). The transaction type (`DEPOSIT` / `WITHDRAWAL`)
  carries the direction; there is no signed-amount model.

## Accounts

- An account is **auto-created on the first deposit** against an unknown ID. There is no separate "create account" step.
- Withdrawals, balance queries, and transaction history on an **unknown account ID** return
  `404`. The assumption is: if you have never deposited into an account, it does not exist.

## Overdraft

- Withdrawals that would take the balance **below zero** are rejected with `422 Unprocessable`. No credit facility or overdraft protection is implied.

## Transaction IDs

- Generated server-side as **UUID v4**. The caller cannot supply or predict them.

## Account IDs

- Caller-supplied, arbitrary strings. The API does not validate their format. Any URL-safe
  string is acceptable (e.g. `alice`, `account-42`, `EUR-001`).

## Timestamps

- Recorded as **UTC `Instant`** at the moment the server processes the request. The caller cannot supply a backdated effective date.
- Serialised as ISO-8601 strings (e.g. `2026-10-11T10:15:30Z`).

## Concurrency

- `ConcurrentHashMap.computeIfAbsent` guarantees at-most-one `AccountState` is created per
  account ID under concurrent creation. Balance updates and list appends are then `synchronized`
  on the `AccountState` object itself, so operations on different accounts never contend.
- This is safe for typical usage. It does **not** provide full ACID atomicity across multiple
  accounts (e.g. a transfer between two accounts). That is explicitly out of scope.

## Persistence

- All data lives **in memory only** and is lost on restart. This is intentional per the brief.

## Double-entry bookkeeping

- Not implemented. Each deposit or withdrawal writes a single transaction record to a single
  account. A full double-entry system (debiting one account while crediting another) would
  require an additional transfer endpoint and is out of scope.

## HTTP transport

- Uses the **JDK built-in `com.sun.net.httpserver.HttpServer`** (part of the JDK since Java 6,
  publicly supported module `jdk.httpserver`). No external HTTP server library is required.
- Each request runs on a **cached thread pool** (`Executors.newCachedThreadPool()`), which creates new threads on demand and reuses idle ones.

## No Spring / no DI framework

- The project has no dependency injection framework. With 11 types across four packages and
  only three collaborators wired in `main()` (`ObjectMapper`, `LedgerStore`, `LedgerHandler`),
  manual wiring is simpler and has less overhead than introducing a container.

## Out of scope (explicitly)

- Authentication / authorisation
- Logging / monitoring / metrics
- Atomic multi-account transfers
- Persistence / database
- Pagination on transaction history
- Currency conversion
- Interest / fees
