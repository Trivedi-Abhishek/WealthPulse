package com.portfolioservice.exception;

public class InsufficientHoldingException extends RuntimeException {
    public InsufficientHoldingException(Long portfolioId, String symbol, Long available, Long requested) {
        super("Insufficient holding for portfolio " + portfolioId + " and symbol " + symbol
                + ": available=" + available + ", requested=" + requested);
    }
}
