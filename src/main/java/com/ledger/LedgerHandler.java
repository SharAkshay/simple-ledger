package com.ledger;

import com.ledger.exception.AccountNotFoundException;
import com.ledger.exception.InsufficientFundsException;
import com.ledger.exception.MethodNotAllowedException;
import com.ledger.http.RequestParser;
import com.ledger.http.ResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public final class LedgerHandler implements HttpHandler {

    private static final int HTTP_OK                    = 200;
    private static final int HTTP_CREATED               = 201;
    private static final int HTTP_BAD_REQUEST           = 400;
    private static final int HTTP_NOT_FOUND             = 404;
    private static final int HTTP_METHOD_NOT_ALLOWED    = 405;
    private static final int HTTP_UNPROCESSABLE_ENTITY  = 422;
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;

    // Matches: /accounts/{id}/deposits|withdrawals|balance|transactions
    private static final Pattern ROUTE =
            Pattern.compile("^/accounts/([^/]+)/(deposits|withdrawals|balance|transactions)$");

    private record MoneyRequest(Long amountMinorUnits, String description) {}

    @FunctionalInterface
    private interface RouteHandler {
        void handle(HttpExchange exchange, String accountId) throws IOException;
    }

    private final LedgerPort store;
    private final RequestParser parser;
    private final ResponseWriter writer;
    private final Map<String, RouteHandler> routes;

    public LedgerHandler(final LedgerPort store, final ObjectMapper mapper) {
        this.store  = store;
        this.parser = new RequestParser(mapper);
        this.writer = new ResponseWriter(mapper);
        this.routes = Map.of(
                "deposits",     (ex, id) -> { requireMethod(ex, "POST"); handleDeposit(ex, id); },
                "withdrawals",  (ex, id) -> { requireMethod(ex, "POST"); handleWithdrawal(ex, id); },
                "balance",      (ex, id) -> { requireMethod(ex, "GET");  handleBalance(ex, id); },
                "transactions", (ex, id) -> { requireMethod(ex, "GET");  handleTransactions(ex, id); }
        );
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        final var path    = exchange.getRequestURI().getPath();
        final var matcher = ROUTE.matcher(path);

        if (!matcher.matches()) {
            writer.send(exchange, HTTP_NOT_FOUND, Map.of("error", "No such endpoint", "code", "NOT_FOUND"));
            return;
        }

        final var accountId = matcher.group(1);
        final var action    = matcher.group(2);

        try {
            routes.get(action).handle(exchange, accountId);
        } catch (MethodNotAllowedException e) {
            writer.send(exchange, HTTP_METHOD_NOT_ALLOWED, Map.of("error", e.getMessage(), "code", "METHOD_NOT_ALLOWED"));
        } catch (AccountNotFoundException e) {
            writer.send(exchange, HTTP_NOT_FOUND, Map.of("error", e.getMessage(), "code", "ACCOUNT_NOT_FOUND"));
        } catch (InsufficientFundsException e) {
            writer.send(exchange, HTTP_UNPROCESSABLE_ENTITY, Map.of("error", e.getMessage(), "code", "INSUFFICIENT_FUNDS",
                    "balanceMinorUnits", e.balance, "requestedMinorUnits", e.requested));
        } catch (IllegalArgumentException e) {
            writer.send(exchange, HTTP_BAD_REQUEST, Map.of("error", e.getMessage(), "code", "VALIDATION_ERROR"));
        } catch (Exception e) {
            writer.send(exchange, HTTP_INTERNAL_SERVER_ERROR, Map.of("error", "Internal server error", "code", "INTERNAL_ERROR"));
        }
    }

    private void handleDeposit(final HttpExchange exchange, final String accountId) throws IOException {
        final var req    = parser.readBody(exchange, MoneyRequest.class);
        final var amount = parser.validatedAmount(req.amountMinorUnits());
        final var tx     = store.deposit(accountId, amount, req.description());
        writer.send(exchange, HTTP_CREATED, tx);
    }

    private void handleWithdrawal(final HttpExchange exchange, final String accountId) throws IOException {
        final var req    = parser.readBody(exchange, MoneyRequest.class);
        final var amount = parser.validatedAmount(req.amountMinorUnits());
        final var tx     = store.withdraw(accountId, amount, req.description());
        writer.send(exchange, HTTP_CREATED, tx);
    }

    private void handleBalance(final HttpExchange exchange, final String accountId) throws IOException {
        final var balance = store.getBalance(accountId);
        writer.send(exchange, HTTP_OK, Map.of("accountId", accountId, "balanceMinorUnits", balance));
    }

    private void handleTransactions(final HttpExchange exchange, final String accountId) throws IOException {
        final var txs = store.getTransactions(accountId);
        writer.send(exchange, HTTP_OK, Map.of("accountId", accountId, "total", txs.size(), "items", txs));
    }

    private void requireMethod(final HttpExchange exchange, final String expected) {
        if (!exchange.getRequestMethod().equalsIgnoreCase(expected)) {
            throw new MethodNotAllowedException(expected, exchange.getRequestMethod());
        }
    }
}
