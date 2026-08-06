package com.alertservice.dal.dto;

import java.time.Instant;

public record PortfolioRebalanceTriggeredEvent(
        Long portfolioId,
        String reason,
        Instant triggeredAt
) {}
