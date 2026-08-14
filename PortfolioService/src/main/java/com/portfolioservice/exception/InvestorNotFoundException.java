package com.portfolioservice.exception;

public class InvestorNotFoundException extends RuntimeException {
    public InvestorNotFoundException(Long investorId) {
        super("Investor not found: " + investorId);
    }
}
