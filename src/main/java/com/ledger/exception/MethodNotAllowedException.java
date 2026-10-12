package com.ledger.exception;

public final class MethodNotAllowedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MethodNotAllowedException(final String expected, final String actual) {
        super("%s not allowed on this endpoint; use %s".formatted(actual, expected));
    }
}
