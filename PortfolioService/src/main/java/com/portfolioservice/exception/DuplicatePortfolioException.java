package com.portfolioservice.exception;

public class DuplicatePortfolioException extends RuntimeException {
    public DuplicatePortfolioException(Long investorId, String portfolioName) {
        super("Investor " + investorId + " already has a portfolio named '" + portfolioName + "'");
    }
}
