package com.portfolioservice.dal.dto;

import com.portfolioservice.enums.RiskProfileEnum;

import java.math.BigDecimal;
import java.time.Instant;

public record HoldingUpdatedEvent(
        Long portfolioId,
        String symbol,
        Long quantity,
        BigDecimal averagePrice,
        RiskProfileEnum riskProfile,
        Instant updatedAt
) {}
