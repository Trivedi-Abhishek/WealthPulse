package com.portfolioservice.exception;

public class DuplicateInvestorException extends RuntimeException {
    public DuplicateInvestorException(String email) {
        super("Investor already registered with email: " + email);
    }
}
