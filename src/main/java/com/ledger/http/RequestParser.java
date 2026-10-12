package com.ledger.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

public final class RequestParser {

    private final ObjectMapper mapper;

    public RequestParser(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <T> T readBody(final HttpExchange exchange, final Class<T> type) {
        try (final var body = exchange.getRequestBody()) {
            return mapper.readValue(body, type);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or missing request body");
        }
    }

    public long validatedAmount(final Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amountMinorUnits must be a positive integer");
        }
        return amount;
    }
}
