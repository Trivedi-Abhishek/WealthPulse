package com.pnlconsumerservice.dal.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HoldingUpdatedEvent(
        Long portfolioId,
        String symbol,
        Long quantity,
        BigDecimal averagePrice,
        Instant updatedAt
) {}
