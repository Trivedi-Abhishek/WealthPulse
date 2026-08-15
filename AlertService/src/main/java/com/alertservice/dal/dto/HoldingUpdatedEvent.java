package com.alertservice.dal.dto;

import com.alertservice.dal.enums.RiskProfileEnum;

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

