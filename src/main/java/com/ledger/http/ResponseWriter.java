package com.ledger.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public final class ResponseWriter {

    private final ObjectMapper mapper;

    public ResponseWriter(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void send(final HttpExchange exchange, final int status, final Object body) throws IOException {
        final var json = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, json.length);
        try (final var out = exchange.getResponseBody()) {
            out.write(json);
        }
    }
}
