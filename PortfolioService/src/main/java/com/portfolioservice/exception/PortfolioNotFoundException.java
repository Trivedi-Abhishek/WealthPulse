package com.portfolioservice.exception;

public class PortfolioNotFoundException extends RuntimeException {
    public PortfolioNotFoundException(Long portfolioId) {
        super("Portfolio not found: " + portfolioId);
    }
}
