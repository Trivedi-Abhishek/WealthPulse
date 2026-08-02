package com.portfolioservice.exception;

public class HoldingNotFoundException extends RuntimeException {
    public HoldingNotFoundException(Long portfolioId, String symbol) {
        super("No active holding for portfolio " + portfolioId + " and symbol " + symbol);
    }
}
