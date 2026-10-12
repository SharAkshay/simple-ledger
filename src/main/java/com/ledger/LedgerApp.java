package com.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class LedgerApp {

    public static void main(final String[] args) throws IOException {
        final var mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final LedgerPort store = new LedgerStore();
        final var server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new LedgerHandler(store, mapper));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("Ledger API running on http://localhost:8080");
        System.out.println("Press Ctrl+C to stop.");
    }
}
