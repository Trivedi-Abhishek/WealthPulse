package com.pnlconsumerservice.dal.dto;

import com.pnlconsumerservice.dal.enums.RiskProfileEnum;

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
