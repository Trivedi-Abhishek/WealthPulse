package com.roboadvisorservice.dal.dto;

import java.math.BigDecimal;

public record PortfolioMetricsEvent(Long portfolioId, BigDecimal currentValue, BigDecimal investedAmount, BigDecimal pnl) {
}